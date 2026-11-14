package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schwerkraftplatte (6.1, 12.9, 14.3): Mit Strom gilt im Bereich {@code area} über der Platte (stations.json) die
 * Schwerkraft {@code gravity_g}. Laufende Platten melden sich jeden Tick (Laufzeitdaten, nicht gespeichert).
 */
public class GravityPlateBlockEntity extends ProcessingMachineBlockEntity {

    /** Dimension → (Plattenposition → letzter Tick mit Strom). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> RUNNING = new ConcurrentHashMap<>();

    public GravityPlateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    @Override
    protected void process(ServerLevel level) {
        if (energy.useInternal(stats().energyPerTick())) {
            RUNNING.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(worldPosition.immutable(),
                    level.getGameTime());
            setStatus(MachineStatus.RUNNING);
        } else {
            stop(level);
            setStatus(MachineStatus.IDLE);
        }
    }

    private void stop(ServerLevel level) {
        Map<BlockPos, Long> m = RUNNING.get(level.dimension());
        if (m != null) {
            m.remove(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            stop(sl);
        }
    }

    /** Liegt {@code pos} im Wirkbereich einer laufenden Platte (Grundfläche um die Platte, Höhe darüber)? */
    public static boolean covered(ServerLevel level, Vec3 pos) {
        Map<BlockPos, Long> m = RUNNING.get(level.dimension());
        if (m == null || m.isEmpty()) {
            return false;
        }
        JsonObject area = StationModules.config("gravity_plate").getAsJsonObject("area");
        if (area == null) {
            return false;
        }
        long now = level.getGameTime();
        m.values().removeIf(t -> t < now - 1);
        for (BlockPos p : m.keySet()) {
            if (StationRules.inPlateArea(p.getX(), p.getY(), p.getZ(), pos.x, pos.y, pos.z, area.get("width").getAsInt(),
                    area.get("depth").getAsInt(), area.get("height").getAsInt())) {
                return true;
            }
        }
        return false;
    }
}
