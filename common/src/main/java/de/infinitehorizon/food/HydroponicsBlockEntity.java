package de.infinitehorizon.food;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.AtmosphereRules;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.machine.ProcessingMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrokultur-Becken (6.1, 17): ein Pflanzslot (Vanilla-Samen oder Setzling), Wachstum je Altersstufe nach
 * agriculture.json, Ernte nach der Vanilla-Loot-Table in den Ausgabeslot, der Samen bleibt im Pflanzslot. Braucht Strom,
 * Wasser, Licht und (außer auf der Erde) eine aktive Kuppel.
 */
public class HydroponicsBlockEntity extends ProcessingMachineBlockEntity {

    /** Kein Wachstum: zu dunkel oder ohne aktive Kuppel (Tooltip {@code status.growth}). */
    public static final int HINT_GROWTH = 4;

    /** Pflanze im Pflanzslot: Block, Zahl der Stufen, Setzling oder Feldfrucht. */
    public record Plant(Block block, int stages, boolean sapling) {
    }

    private int stage;
    private int stageTicks;
    private int waterTicks;
    private long fertilizedUntil;
    private int ticksPerStage;
    private boolean noGrowth;

    public HydroponicsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private static JsonObject cfg(String key) {
        return BalanceManager.get().entry("agriculture", key).orElseThrow();
    }

    private static JsonObject basin() {
        return cfg(InfiniteHorizon.id("hydroponics_basin").toString());
    }

    /** Pflanze zu einem Item, oder null (nur Vanilla-Feldfrüchte und Setzlinge, 17). */
    public static @Nullable Plant plant(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) {
            return null;
        }
        // SPEC-GAP: „Vanilla-Samen/Setzlinge“ = Blöcke vom Typ CropBlock oder SaplingBlock (Stängel, Kakao, Beeren nicht)
        if (bi.getBlock() instanceof CropBlock crop) {
            return new Plant(crop, crop.getMaxAge(), false);
        }
        if (bi.getBlock() instanceof SaplingBlock) {
            return new Plant(bi.getBlock(), cfg("saplings").get("growth_stages").getAsInt(), true);
        }
        return null;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return plant(stack) != null;
    }

    public int stage() {
        return stage;
    }

    /** Setzt die Altersstufe (für Tests und Befehle). */
    public void setStage(int stage) {
        this.stage = stage;
        this.stageTicks = 0;
        setChanged();
    }

    /** Ammoniakdünger (17): schnelleres Wachstum für {@code duration_ticks}. */
    public void fertilize(ServerLevel level) {
        JsonObject f = cfg(InfiniteHorizon.id("ammonia_fertilizer").toString());
        fertilizedUntil = level.getGameTime() + f.get("duration_ticks").getAsLong();
        setChanged();
    }

    public boolean fertilized(ServerLevel level) {
        return level.getGameTime() < fertilizedUntil;
    }

    @Override
    public int statusHint() {
        return noGrowth ? HINT_GROWTH : super.statusHint();
    }

    @Override
    protected long syncValue(int index) {
        Plant p = plant(items.isEmpty() ? ItemStack.EMPTY : items.get(0));
        int stages = p == null ? 0 : p.stages();
        return switch (index) {
            case 2 -> (long) stage * ticksPerStage + stageTicks;
            case 3 -> (long) stages * ticksPerStage;
            default -> super.syncValue(index);
        };
    }

    @Override
    protected void process(ServerLevel level) {
        noGrowth = false;
        if (de.infinitehorizon.event.EventService.machinePaused(level, getBlockPos())) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        ItemStack seed = items.get(0);
        Plant plant = plant(seed);
        if (plant == null) {
            stage = 0;
            stageTicks = 0;
            setStatus(MachineStatus.IDLE);
            return;
        }
        if (stage >= plant.stages()) {
            List<ItemStack> drops = harvest(level, plant, seed);
            if (!outputsFit(drops)) {
                setStatus(MachineStatus.ERROR);
                return;
            }
            drops.forEach(this::insertOutput);
            stage = 0;
            stageTicks = 0;
            setStatus(MachineStatus.RUNNING);
            setChanged();
            return;
        }
        JsonObject b = basin();
        boolean requiresDome = cfg("plant_growth_rules").get("requires_active_dome_outside_earth").getAsBoolean();
        boolean airless = AtmosphereRules.mayBeAirless(level) && AtmosphereRules.airless(level, getBlockPos());
        int light = level.getRawBrightness(getBlockPos().above(), 0);
        if (!FoodRules.canGrow(light, b.get("light_min").getAsInt(), airless, requiresDome)) {
            noGrowth = true;
            setStatus(MachineStatus.IDLE);
            return;
        }
        int normal = plant.sapling() ? cfg("saplings").get("ticks_per_stage").getAsInt()
                : b.get("ticks_per_growth_stage").getAsInt();
        int base = FoodRules.ticksPerStage(normal, b.get("ticks_per_growth_stage_fertilized").getAsInt(), fertilized(level));
        ProcessingMath.Effective eff = ProcessingMath.effective(stats().withProcess(stats().energyPerTick(), base),
                installedUpgrades());
        ticksPerStage = eff.ticks();
        if (!hasWater() || !energy.useInternal(eff.energyPerTick())) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        consumeWater();
        if (++stageTicks >= ticksPerStage) {
            stage++;
            stageTicks = 0;
        }
        setStatus(MachineStatus.RUNNING);
        setChanged();
    }

    /** Wasserverbrauch aus machines.json ({@code fluid_consumption}). */
    private @Nullable JsonObject water() {
        JsonElement fc = BalanceManager.get().entry("machines", machineId()).map(o -> o.get("fluid_consumption")).orElse(null);
        return fc != null && fc.isJsonArray() && !fc.getAsJsonArray().isEmpty() ? fc.getAsJsonArray().get(0).getAsJsonObject() : null;
    }

    private boolean hasWater() {
        JsonObject w = water();
        return w == null || !inputTanks().isEmpty() && inputTanks().getFirst().stack().amount() >= w.get("amount").getAsLong();
    }

    private void consumeWater() {
        JsonObject w = water();
        if (w == null || ++waterTicks < w.get("per_ticks").getAsInt()) {
            return;
        }
        waterTicks = 0;
        IhFluidTank tank = inputTanks().getFirst();
        try (Transaction tx = Transaction.open()) {
            tank.extract(tank.stack().fluid(), w.get("amount").getAsLong(), tx);
            tx.commit();
        }
    }

    /** Ernte (17): Feldfrucht nach der Vanilla-Loot-Table, ein Samen davon bleibt im Pflanzslot; Setzling gibt Stämme. */
    private List<ItemStack> harvest(ServerLevel level, Plant plant, ItemStack seed) {
        List<ItemStack> out = new ArrayList<>();
        if (plant.sapling()) {
            Identifier sid = BuiltInRegistries.BLOCK.getKey(plant.block());
            Item log = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(sid.getNamespace(),
                    FoodRules.logFor(sid.getPath())));
            if (log == Items.AIR) {
                log = Items.OAK_LOG;
            }
            // yield[0] = Stämme; yield[1] = der Setzling, der im Pflanzslot bleibt
            int count = cfg("saplings").getAsJsonArray("yield").get(0).getAsJsonObject().get("count").getAsInt();
            out.add(new ItemStack(log, count));
            return out;
        }
        CropBlock crop = (CropBlock) plant.block();
        boolean seedTaken = false;
        for (ItemStack d : Block.getDrops(crop.getStateForAge(crop.getMaxAge()), level, getBlockPos(), null)) {
            if (!seedTaken && d.is(seed.getItem())) {
                d.shrink(1);
                seedTaken = true;
            }
            if (!d.isEmpty()) {
                out.add(d);
            }
        }
        return out;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("stage", stage);
        output.putInt("stage_ticks", stageTicks);
        output.putInt("water_ticks", waterTicks);
        output.putLong("fertilized_until", fertilizedUntil);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stage = input.getIntOr("stage", 0);
        stageTicks = input.getIntOr("stage_ticks", 0);
        waterTicks = input.getIntOr("water_ticks", 0);
        fertilizedUntil = input.getLongOr("fertilized_until", 0L);
    }
}
