package de.sternenwerft.machine;

import com.google.gson.JsonObject;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.data.BalanceManager;
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

    private final SwFluidTank tank = new SwFluidTank(0, null, this::setChanged);

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SwFluidTank tank() {
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
    protected void configureEnergy(SwEnergyStorage storage) {
        storage.reconfigure(0, 0, 0);
        JsonObject s = stats();
        tank.reconfigure(s.has("capacity") ? s.get("capacity").getAsLong() : 0, null);
    }

    @Override
    protected List<SwFluidTank> inputTanks() {
        return List.of(tank);
    }

    @Override
    protected List<SwFluidTank> outputTanks() {
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
