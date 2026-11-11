package de.sternenwerft.core.grid;

import de.sternenwerft.block.CableBlock;
import de.sternenwerft.block.ConduitBlock;
import de.sternenwerft.block.FluidPipeBlock;
import de.sternenwerft.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Kabel- und Rohrnetze je Welt (3A.3). Die Topologie wird nur nach Blockänderungen neu berechnet. */
public final class GridManager {

    private static final Map<LevelAccessor, GridManager> BY_LEVEL = new WeakHashMap<>();

    private final Map<BlockPos, EnergyGrid> energy = new HashMap<>();
    private final Map<BlockPos, FluidGrid> fluid = new HashMap<>();

    private GridManager() {
    }

    public static synchronized GridManager get(Level level) {
        return BY_LEVEL.computeIfAbsent(level, l -> new GridManager());
    }

    /** Verwirft alle Netze der Welt; sie entstehen beim nächsten Zugriff neu. */
    public static synchronized void invalidate(LevelAccessor level) {
        GridManager m = BY_LEVEL.get(level);
        if (m != null) {
            m.energy.clear();
            m.fluid.clear();
        }
    }

    /** Energienetz des Kabels an {@code pos} oder null. */
    public @Nullable EnergyGrid gridAt(Level level, BlockPos pos) {
        EnergyGrid grid = energy.get(pos);
        if (grid != null) {
            return grid;
        }
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof CableBlock)) {
            return null;
        }
        Topology t = build(level, pos, block);
        grid = new EnergyGrid(t.conduits, t.endpoints, throughput(block));
        for (BlockPos p : t.conduits) {
            energy.put(p, grid);
        }
        return grid;
    }

    /** Rohrnetz des Rohrs an {@code pos} oder null. */
    public @Nullable FluidGrid fluidGridAt(Level level, BlockPos pos) {
        FluidGrid grid = fluid.get(pos);
        if (grid != null) {
            return grid;
        }
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof FluidPipeBlock)) {
            return null;
        }
        Topology t = build(level, pos, block);
        grid = new FluidGrid(t.conduits, t.endpoints, throughput(block));
        for (BlockPos p : t.conduits) {
            fluid.put(p, grid);
        }
        return grid;
    }

    private static long throughput(Block block) {
        return BalanceManager.get().cableThroughput(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    private record Topology(Set<BlockPos> conduits, List<EnergyGrid.Endpoint> endpoints) {
    }

    private static Topology build(Level level, BlockPos origin, Block conduit) {
        Set<BlockPos> conduits = new HashSet<>();
        List<EnergyGrid.Endpoint> endpoints = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        conduits.add(origin.immutable());
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (conduits.contains(n) || !level.isLoaded(n)) {
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (ns.getBlock() == conduit) {
                    conduits.add(n);
                    queue.add(n);
                } else if (!(ns.getBlock() instanceof ConduitBlock) && !ns.isAir()) {
                    endpoints.add(new EnergyGrid.Endpoint(n, d.getOpposite()));
                }
            }
        }
        return new Topology(conduits, endpoints);
    }
}
