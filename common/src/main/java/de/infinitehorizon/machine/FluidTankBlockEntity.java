package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Fluid-Speicher (6.3, 4.4): ein Tank, Ausgabe nur über die Ausgangsseite (Standard {@code facing}), alle anderen
 * Seiten nehmen an; Füll- und Leerslot (4.1). Werte aus {@code storage.json} per Block-ID.
 */
public class FluidTankBlockEntity extends MachineBlockEntity {

    private final IhFluidTank tank = new IhFluidTank(0, null, this::setChanged);

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IhFluidTank tank() {
        return tank;
    }

    private JsonObject stats() {
        return BalanceManager.get().entry("storage", machineId()).orElse(new JsonObject());
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        JsonObject slots = stats().has("container_slots") ? stats().getAsJsonObject("container_slots") : new JsonObject();
        int fill = slots.has("fill") ? slots.get("fill").getAsInt() : 0;
        int drain = slots.has("drain") ? slots.get("drain").getAsInt() : 0;
        return new MachineLayout(0, 0, 0, 0, 0, fill, drain);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        storage.reconfigure(0, 0, 0);
        JsonObject s = stats();
        String accepts = s.has("accepts") && s.get("accepts").isJsonPrimitive() ? s.get("accepts").getAsString() : null;
        // Tank nur für Flüssigkeiten (6.3: accepts = liquid)
        tank.reconfigure(s.has("capacity") ? s.get("capacity").getAsLong() : 0,
                id -> de.infinitehorizon.core.fluid.FluidIds.matchesAccepts(accepts, id));
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
        return List.of(tank);
    }

    @Override
    protected List<IhFluidTank> outputTanks() {
        return List.of(tank);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    /** Beim Abbauen: Inhalt als {@code infinite_horizon:fluid} ans Item (Loot kopiert die Komponente, 4.1). */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.stack().isEmpty()) {
            components.set(ModDataComponents.FLUID.get(), tank.stack());
        }
    }

    /** Beim Platzieren eines Tanks mit Inhalt. */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        IhFluidStack stored = components.get(ModDataComponents.FLUID.get());
        if (stored != null) {
            ensureLayout();
            tank.setStackUnchecked(stored);
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        handleContainers();
        long before = tank.stack().amount();
        pushFluids(level);
        setStatus(tank.stack().amount() < before ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }
}
