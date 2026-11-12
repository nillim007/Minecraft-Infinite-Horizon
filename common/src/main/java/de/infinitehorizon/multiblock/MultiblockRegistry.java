package de.infinitehorizon.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Registrierliste der Multiblock-Controller je Level (4.3). Gehäuse- und Port-Blöcke melden jede Änderung an alle
 * Controller im Abstand von höchstens {@link #NOTIFY_RADIUS} Blöcken (Würfel um den geänderten Block). Nur Server.
 */
public final class MultiblockRegistry {

    // SPEC-GAP: „Abstand von höchstens 5 Blöcken“ steht nur in 4.3, nicht in spec/data (spec-gaps #24)
    public static final int NOTIFY_RADIUS = 5;

    private static final Map<Level, Set<BlockPos>> CONTROLLERS = Collections.synchronizedMap(new WeakHashMap<>());

    private MultiblockRegistry() {
    }

    public static void register(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            CONTROLLERS.computeIfAbsent(level, l -> new HashSet<>()).add(pos.immutable());
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        Set<BlockPos> set = CONTROLLERS.get(level);
        if (set != null) {
            set.remove(pos);
        }
    }

    /** Ein Gehäuse- oder Port-Block an {@code changed} wurde gesetzt, entfernt oder umgeschaltet. */
    public static void notifyChange(Level level, BlockPos changed) {
        Set<BlockPos> set = CONTROLLERS.get(level);
        if (level.isClientSide() || set == null) {
            return;
        }
        for (BlockPos p : Set.copyOf(set)) {
            if (Math.abs(p.getX() - changed.getX()) <= NOTIFY_RADIUS && Math.abs(p.getY() - changed.getY()) <= NOTIFY_RADIUS
                    && Math.abs(p.getZ() - changed.getZ()) <= NOTIFY_RADIUS
                    && level.getBlockEntity(p) instanceof MultiblockController c) {
                c.markStructureDirty();
            }
        }
    }
}
