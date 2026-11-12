package de.infinitehorizon.core.grid;

import de.infinitehorizon.api.fluid.FluidNode;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rohrnetz (4.4): kein Puffer, Durchsatz pro Tick unabhängig von der Länge, gleichmäßige Verteilung. Ein Netz führt
 * nur ein Fluid; ein anderes Fluid kommt erst hinein, wenn das Netz „leer“ ist (in den letzten
 * {@link #EMPTY_AFTER_TICKS} Ticks nichts übertragen).
 */
public final class FluidGrid {

    // SPEC-GAP: „leer = 20 t ohne Übertragung“ steht nur in 4.4, nicht in transport.json (spec-gaps #22).
    public static final long EMPTY_AFTER_TICKS = 20;

    private final Set<BlockPos> pipes;
    private final List<EnergyGrid.Endpoint> endpoints;
    private final long throughput;
    private long tick = Long.MIN_VALUE;
    private long usedThisTick;
    private @Nullable Identifier currentFluid;
    private long lastTransfer = Long.MIN_VALUE / 2;

    private final List<FluidTarget> targets = new ArrayList<>();
    private long[] wants = new long[0];

    FluidGrid(Set<BlockPos> pipes, List<EnergyGrid.Endpoint> endpoints, long throughput) {
        this.pipes = Set.copyOf(pipes);
        this.endpoints = List.copyOf(endpoints);
        this.throughput = throughput;
    }

    public Set<BlockPos> pipes() {
        return pipes;
    }

    public List<EnergyGrid.Endpoint> endpoints() {
        return endpoints;
    }

    public long throughput() {
        return throughput;
    }

    /** Fluid, das das Netz gerade führt (null = leer). */
    public @Nullable Identifier currentFluid(Level level) {
        return level.getGameTime() - lastTransfer < EMPTY_AFTER_TICKS ? currentFluid : null;
    }

    /** Schiebt bis zu {@code offer.amount()} mB aus {@code source} ins Netz; gibt die verteilte Menge zurück. */
    public long push(Level level, BlockPos source, IhFluidStack offer) {
        if (offer.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            usedThisTick = 0;
        }
        Identifier carried = currentFluid(level);
        if (carried != null && !carried.equals(offer.fluid())) {
            return 0;
        }
        long budget = Math.min(offer.amount(), throughput - usedThisTick);
        if (budget <= 0) {
            return 0;
        }
        targets.clear();
        for (EnergyGrid.Endpoint ep : endpoints) {
            if (ep.pos().equals(source)) {
                continue;
            }
            FluidTarget t = resolve(level, ep);
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
        IhFluidStack probe = offer.withAmount(budget);
        for (int i = 0; i < n; i++) {
            wants[i] = targets.get(i).insert(probe, true);
        }
        long[] give = EnergyGrid.distribute(budget, wants, n);
        long moved = 0;
        for (int i = 0; i < n; i++) {
            if (give[i] > 0) {
                moved += targets.get(i).insert(offer.withAmount(give[i]), false);
            }
        }
        if (moved > 0) {
            usedThisTick += moved;
            currentFluid = offer.fluid();
            lastTransfer = now;
        }
        targets.clear();
        return moved;
    }

    private static @Nullable FluidTarget resolve(Level level, EnergyGrid.Endpoint ep) {
        BlockEntity be = level.getBlockEntity(ep.pos());
        if (be instanceof FluidNode node) {
            return node.fluidInput(ep.side());
        }
        return Services.FLUID.findTarget(level, ep.pos(), ep.side());
    }
}
