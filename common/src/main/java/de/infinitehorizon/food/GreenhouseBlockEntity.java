package de.infinitehorizon.food;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.MachineStats;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.machine.MachineLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Gewächshaus (14.2, 17): verhält sich wie 9 Hydrokultur-Becken, nur im Orbit, braucht {@code consumes} (Trockeneis) je
 * {@code per_ticks}. Werte in agriculture.json; Tank und Füll-/Leerslots wie beim Becken (SPEC-GAP #97).
 */
public class GreenhouseBlockEntity extends HydroponicsBlockEntity {

    private static final String BASIN = InfiniteHorizon.id("hydroponics_basin").toString();
    private int supplyTicks;

    public GreenhouseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject cfg() {
        return BalanceManager.get().entry("agriculture", machineId()).orElse(new JsonObject());
    }

    private JsonObject consumes() {
        return cfg().has("consumes") ? cfg().getAsJsonObject("consumes") : new JsonObject();
    }

    @Override
    public MachineStats stats() {
        JsonObject c = cfg();
        long energy = c.has("energy_per_tick") ? c.get("energy_per_tick").getAsLong() : 0;
        JsonObject slots = c.has("item_slots") ? c.getAsJsonObject("item_slots") : new JsonObject();
        int in = slots.has("input") ? slots.get("input").getAsInt() : 0;
        int out = slots.has("output") ? slots.get("output").getAsInt() : 0;
        // ein Slot mehr für das Verbrauchsgut (Trockeneis) hinter den Pflanzslots
        return new MachineStats(machineId(), energy, 0, BalanceManager.get().bufferFormula().apply(energy, null),
                in + (consumes().has("item") ? 1 : 0), out, 0, 0);
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        MachineStats s = stats();
        int[] containers = BalanceManager.get().containerSlots("machines", BASIN);
        return new MachineLayout(s.inputSlots(), 0, s.outputSlots(), 0, 0, containers[0], containers[1]);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        MachineStats s = stats();
        storage.reconfigure(s.buffer(), s.buffer(), 0);
        configureTanks(BalanceManager.get().tanks("machines", BASIN));
    }

    @Override
    protected long baseEnergyPerTick() {
        return stats().energyPerTick();
    }

    @Override
    protected @Nullable JsonObject water() {
        JsonElement fc = cfg().get("fluid_consumption");
        return fc != null && fc.isJsonArray() && !fc.getAsJsonArray().isEmpty() ? fc.getAsJsonArray().get(0).getAsJsonObject() : null;
    }

    @Override
    protected boolean located(ServerLevel level) {
        return MachineLocation.allowed(cfg(), level, getBlockPos());
    }

    @Override
    protected boolean acceptsExtra(ItemStack stack) {
        JsonObject c = consumes();
        return c.has("item") && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Identifier.parse(c.get("item").getAsString()));
    }

    /** Je {@code per_ticks} Wachstum wird {@code count} Trockeneis verbraucht; ohne Vorrat kein Wachstum. */
    @Override
    protected boolean supplied(ServerLevel level, boolean consume) {
        JsonObject c = consumes();
        if (!c.has("item")) {
            return true;
        }
        if (supplyTicks > 0) {
            if (consume) {
                supplyTicks--;
            }
            return true;
        }
        ItemStack slot = items.get(plantSlots());
        int count = c.get("count").getAsInt();
        if (!acceptsExtra(slot) || slot.getCount() < count) {
            return false;
        }
        if (consume) {
            slot.shrink(count);
            supplyTicks = c.get("per_ticks").getAsInt() - 1;
            setChanged();
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("supply_ticks", supplyTicks);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        supplyTicks = input.getIntOr("supply_ticks", 0);
    }
}
