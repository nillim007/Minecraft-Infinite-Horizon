package de.sternenwerft.core.grid;

import de.sternenwerft.block.CableBlock;
import de.sternenwerft.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Kabelnetze je Welt (3A.3). Die Topologie wird nur nach Blockänderungen neu berechnet. */
public final class GridManager {

    private static final Map<LevelAccessor, GridManager> BY_LEVEL = new WeakHashMap<>();

    private final Map<BlockPos, EnergyGrid> byCable = new HashMap<>();

    private GridManager() {
    }

    public static synchronized GridManager get(Level level) {
        return BY_LEVEL.computeIfAbsent(level, l -> new GridManager());
    }

    /** Verwirft alle Netze der Welt; sie entstehen beim nächsten Zugriff neu. */
    public static synchronized void invalidate(LevelAccessor level) {
        GridManager m = BY_LEVEL.get(level);
        if (m != null) {
            m.byCable.clear();
        }
    }

    /** Netz des Kabels an {@code pos} oder null, wenn dort kein Kabel liegt. */
    public EnergyGrid gridAt(Level level, BlockPos pos) {
        EnergyGrid grid = byCable.get(pos);
        if (grid != null) {
            return grid;
        }
        BlockState start = level.getBlockState(pos);
        if (!(start.getBlock() instanceof CableBlock)) {
            return null;
        }
        grid = build(level, pos, start.getBlock());
        for (BlockPos p : grid.cables()) {
            byCable.put(p, grid);
        }
        return grid;
    }

    private static EnergyGrid build(Level level, BlockPos origin, Block cable) {
        Set<BlockPos> cables = new HashSet<>();
        List<EnergyGrid.Endpoint> endpoints = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cables.add(origin.immutable());
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (cables.contains(n) || !level.isLoaded(n)) {
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (ns.getBlock() == cable) {
                    cables.add(n);
                    queue.add(n);
                } else if (!(ns.getBlock() instanceof CableBlock) && !ns.isAir()) {
                    endpoints.add(new EnergyGrid.Endpoint(n, d.getOpposite()));
                }
            }
        }
        String id = BuiltInRegistries.BLOCK.getKey(cable).toString();
        return new EnergyGrid(cables, endpoints, BalanceManager.get().cableThroughput(id));
    }
}
