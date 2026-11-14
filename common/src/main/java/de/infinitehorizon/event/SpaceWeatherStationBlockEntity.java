package de.infinitehorizon.event;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineLocation;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Weltraumwetter-Station (6.1, 19): braucht Strom und einen Besitzer, läuft überall außer auf der Erde und bringt ihrer
 * Agentur in dieser Dimension die frühe Warnung vor Ereignissen.
 */
public class SpaceWeatherStationBlockEntity extends ProcessingMachineBlockEntity {

    private boolean wrongPlace;

    public SpaceWeatherStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    @Override
    public int statusHint() {
        return wrongPlace ? HINT_LOCATION : owner() == null ? HINT_NO_OWNER : super.statusHint();
    }

    @Override
    protected void process(ServerLevel level) {
        wrongPlace = !MachineLocation.allowed(BalanceManager.get().entry("machines", machineId()).orElse(null), level,
                getBlockPos());
        if (wrongPlace) {
            WeatherStations.stopped(level, getBlockPos());
            setStatus(MachineStatus.ERROR);
            return;
        }
        if (owner() == null || !energy.useInternal(stats().energyPerTick())) {
            WeatherStations.stopped(level, getBlockPos());
            setStatus(MachineStatus.IDLE);
            return;
        }
        WeatherStations.running(level, getBlockPos(), owner());
        setStatus(MachineStatus.RUNNING);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            WeatherStations.stopped(sl, getBlockPos());
        }
    }
}
