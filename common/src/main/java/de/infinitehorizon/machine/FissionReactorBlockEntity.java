package de.infinitehorizon.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.MachineStats;
import de.infinitehorizon.environment.RadiationZones;
import de.infinitehorizon.multiblock.MultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Kernreaktor (6.3, 4.3, 15): Controller eines 5×5×5-Multiblocks. Verbraucht je {@code per_ticks} einen Brennstab aus
 * seinen Brennstoff-Slots und braucht eines der Kühlmittel ({@code coolant_mode} {@code any_one}). Hitze nach
 * {@link FissionHeat}; ab {@code warning_at} warnt er (Ton, blinkende Lampe), bei {@code scram_at} schaltet er ab und
 * hinterlässt eine Strahlungszone ({@code radiation_zone}). Keine Explosion.
 */
public class FissionReactorBlockEntity extends MultiblockControllerBlockEntity {

    /** Warnton alle 40 t, Lampe wechselt alle 10 t (SPEC-GAP #110). */
    private static final int WARN_SOUND_TICKS = 40;
    private static final int BLINK_TICKS = 10;

    private FissionHeat heat = FissionHeat.START;
    private int burnLeft;

    public FissionReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private JsonObject cfg() {
        return BalanceManager.get().entry("generators", machineId()).orElse(new JsonObject());
    }

    private JsonObject safety() {
        return cfg().has("safety") ? cfg().getAsJsonObject("safety") : new JsonObject();
    }

    /** Hitzeregeln aus {@code safety}. */
    public FissionHeat.Rules rules() {
        JsonObject s = safety();
        return new FissionHeat.Rules(s.get("heat_min").getAsInt(), s.get("heat_max").getAsInt(),
                s.get("heat_per_tick_cooled").getAsInt(), s.get("heat_per_tick_uncooled").getAsInt(),
                s.get("warning_at").getAsInt(), s.get("scram_at").getAsInt(), s.get("restart_below_heat").getAsInt());
    }

    public FissionHeat heat() {
        return heat;
    }

    /** Für Tests: Hitze setzen. */
    public void setHeat(int value) {
        heat = new FissionHeat(value, heat.scrammed());
        setChanged();
    }

    public int burnLeft() {
        return burnLeft;
    }

    @Override
    public MachineStats stats() {
        long buffer = cfg().has("buffer") ? cfg().get("buffer").getAsLong() : 0;
        return new MachineStats(machineId(), 0, 0, buffer, 0, 0, 0, 0);
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        int[] containers = BalanceManager.get().containerSlots("generators", machineId());
        return new MachineLayout(0, 0, 0, 0, BalanceManager.get().generator(machineId()).fuelSlots(), containers[0], containers[1]);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        long buffer = stats().buffer();
        storage.reconfigure(buffer, 0, buffer);
        configureTanks(BalanceManager.get().tanks("generators", machineId()));
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return true;
    }

    private @Nullable JsonObject fuel() {
        JsonElement f = cfg().get("fuel");
        return f != null && f.isJsonArray() && !f.getAsJsonArray().isEmpty() ? f.getAsJsonArray().get(0).getAsJsonObject() : null;
    }

    private boolean isFuel(ItemStack stack) {
        JsonObject f = fuel();
        return f != null && stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(f.get("item").getAsString())));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (layout().role(slot) == MachineLayout.Role.FUEL) {
            return isFuel(stack);
        }
        return super.canPlaceItem(slot, stack);
    }

    /** Nimmt einen Brennstab aus den Brennstoff-Slots; false, wenn keiner da ist. */
    private boolean takeFuel() {
        JsonObject f = fuel();
        if (f == null) {
            return false;
        }
        MachineLayout l = layout();
        for (int i = l.fuelStart(); i < l.fuelStart() + l.fuel() && i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (isFuel(s) && s.getCount() >= f.get("amount").getAsInt()) {
                s.shrink(f.get("amount").getAsInt());
                burnLeft = f.get("per_ticks").getAsInt();
                return true;
            }
        }
        return false;
    }

    /** Erstes Kühlmittel mit genug Vorrat ({@code any_one}); null ohne Kühlmittel. */
    private @Nullable JsonObject coolant() {
        JsonElement c = cfg().get("coolant");
        if (c == null || !c.isJsonArray()) {
            return null;
        }
        for (JsonElement e : c.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            Identifier fluid = Identifier.parse(o.get("fluid").getAsString());
            long have = 0;
            for (IhFluidTank t : inputTanks()) {
                if (t.stack().is(fluid)) {
                    have += t.stack().amount();
                }
            }
            if (have >= o.get("amount").getAsLong()) {
                return o;
            }
        }
        return null;
    }

    private void consumeCoolant(JsonObject o) {
        Identifier fluid = Identifier.parse(o.get("fluid").getAsString());
        long left = o.get("amount").getAsLong();
        try (Transaction tx = Transaction.open()) {
            for (IhFluidTank t : inputTanks()) {
                if (left > 0 && t.stack().is(fluid)) {
                    left -= t.extract(fluid, left, tx).amount();
                }
            }
            tx.commit();
        }
    }

    @Override
    protected void process(ServerLevel level) {
        JsonObject c = cfg();
        FissionHeat.Rules r = rules();
        boolean wantsPower = energy.getAmount() < energy.getCapacity();
        if (!heat.scrammed() && burnLeft <= 0 && wantsPower) {
            takeFuel();
        }
        boolean burning = !heat.scrammed() && burnLeft > 0 && wantsPower;
        JsonObject coolant = coolant();
        boolean cooled = coolant != null && (burning || heat.heat() > r.min());
        if (cooled) {
            consumeCoolant(coolant);
        }
        boolean wasScrammed = heat.scrammed();
        heat = heat.step(burning, cooled, r);
        if (burning) {
            burnLeft--;
            try (Transaction tx = Transaction.open()) {
                energy.produceInternal(c.get("output_per_tick").getAsLong(), tx);
                tx.commit();
            }
        }
        if (heat.scrammed() && !wasScrammed) {
            scram(level);
        }
        boolean warning = heat.warning(r) && !heat.scrammed();
        if (warning && level.getGameTime() % WARN_SOUND_TICKS == 0) {
            var sound = BuiltInRegistries.SOUND_EVENT.getValue(InfiniteHorizon.id("warning.radiation"));
            if (sound != null) {
                level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 1f, 1f);
            }
        }
        pushEnergy(level);
        MachineStatus status = heat.scrammed() ? MachineStatus.ERROR : burning ? MachineStatus.RUNNING : MachineStatus.IDLE;
        if (warning && (level.getGameTime() / BLINK_TICKS) % 2 == 0) {
            status = MachineStatus.ERROR; // Lampe blinkt (6.3)
        }
        setStatus(status);
        setChanged();
    }

    /** Notabschaltung (6.3): Strahlungszone um den Reaktor, ohne Explosion. */
    private void scram(ServerLevel level) {
        JsonObject s = safety();
        if (s.has("radiation_zone")) {
            JsonObject z = s.getAsJsonObject("radiation_zone");
            RadiationZones.add(level, getBlockPos(), z.get("radius").getAsInt(), z.get("radiation").getAsInt(),
                    z.get("duration_ticks").getAsLong());
        }
    }

    /** GUI: Fortschrittsbalken zeigt die Hitze (Wert 2) von der Höchsthitze (Wert 3). */
    @Override
    protected long syncValue(int index) {
        if (index == 2) {
            return heat.heat();
        }
        if (index == 3) {
            return cfg().has("safety") ? rules().max() : 0;
        }
        return super.syncValue(index);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat.heat());
        output.putBoolean("scrammed", heat.scrammed());
        output.putInt("burn_left", burnLeft);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = new FissionHeat(input.getIntOr("heat", 0), input.getBooleanOr("scrammed", false));
        burnLeft = input.getIntOr("burn_left", 0);
    }
}
