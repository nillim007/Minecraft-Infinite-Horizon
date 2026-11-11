package de.sternenwerft.core.grid;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ein zusammenhängender Strang gleicher Kabel (4.4). Durchsatz pro Tick unabhängig von der Länge;
 * Verteilung gleichmäßig auf alle Empfänger, die noch aufnehmen können.
 */
public final class EnergyGrid {

    /** Angrenzender Block und dessen Seite, die zum Kabel zeigt. */
    public record Endpoint(BlockPos pos, Direction side) {
    }

    private final Set<BlockPos> cables;
    private final List<Endpoint> endpoints;
    private final long throughput;
    private long tick = Long.MIN_VALUE;
    private long usedThisTick;

    // Wiederverwendete Puffer (3A.7)
    private final List<EnergyTarget> targets = new ArrayList<>();
    private long[] wants = new long[0];

    EnergyGrid(Set<BlockPos> cables, List<Endpoint> endpoints, long throughput) {
        this.cables = Set.copyOf(cables);
        this.endpoints = List.copyOf(endpoints);
        this.throughput = throughput;
    }

    public Set<BlockPos> cables() {
        return cables;
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public long throughput() {
        return throughput;
    }

    /** Schiebt bis zu {@code amount} J aus {@code source} ins Netz und gibt die verteilte Menge zurück. */
    public long push(Level level, BlockPos source, long amount) {
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            usedThisTick = 0;
        }
        long budget = Math.min(amount, throughput - usedThisTick);
        if (budget <= 0) {
            return 0;
        }
        targets.clear();
        for (Endpoint ep : endpoints) {
            if (ep.pos().equals(source)) {
                continue;
            }
            EnergyTarget t = resolve(level, ep);
            if (t != null) {
                targets.add(t);
            }
        }
        int n = targets.size();
        if (n == 0) {
            return 0;
        }
        if (wants.length < n) {
            wants = new long[n];
        }
        for (int i = 0; i < n; i++) {
            wants[i] = targets.get(i).insert(budget, true);
        }
        long[] give = distribute(budget, wants, n);
        long moved = 0;
        for (int i = 0; i < n; i++) {
            if (give[i] > 0) {
                moved += targets.get(i).insert(give[i], false);
            }
        }
        usedThisTick += moved;
        targets.clear();
        return moved;
    }

    /** Gleichmäßige Verteilung: jeder bekommt höchstens, was er will; Reste gehen an die anderen. */
    public static long[] distribute(long budget, long[] wants, int n) {
        long[] give = new long[n];
        boolean[] done = new boolean[n];
        long remaining = budget;
        int open = 0;
        for (int i = 0; i < n; i++) {
            if (wants[i] > 0) {
                open++;
            } else {
                done[i] = true;
            }
        }
        while (remaining > 0 && open > 0) {
            long share = Math.max(1, remaining / open);
            boolean progressed = false;
            for (int i = 0; i < n && remaining > 0; i++) {
                if (done[i]) {
                    continue;
                }
                long g = Math.min(Math.min(share, wants[i] - give[i]), remaining);
                if (g > 0) {
                    give[i] += g;
                    remaining -= g;
                    progressed = true;
                }
                if (give[i] >= wants[i]) {
                    done[i] = true;
                    open--;
                }
            }
            if (!progressed) {
                break;
            }
        }
        return give;
    }

    private static EnergyTarget resolve(Level level, Endpoint ep) {
        BlockEntity be = level.getBlockEntity(ep.pos());
        if (be instanceof EnergyNode node) {
            return node.acceptsEnergyFrom(ep.side()) ? node.asTarget() : null;
        }
        return Services.ENERGY.findTarget(level, ep.pos(), ep.side());
    }
}
