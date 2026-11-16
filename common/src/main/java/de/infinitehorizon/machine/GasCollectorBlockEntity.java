package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code gas_collector} (6.1, 13.3): sammelt im Orbit eines Gasriesen das Gas des Planeten ohne Zutat, Menge und Dauer
 * aus {@code gas_collector_output} des Planeten. Ortsbindung über machines.json.
 */
public class GasCollectorBlockEntity extends ProcessingMachineBlockEntity {

    public GasCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    /** Gas des Planeten an dieser Stelle ({@code gas_collector_output}), sonst null. */
    public static @Nullable JsonObject output(ServerLevel level, BlockPos pos) {
        Planet p = PlanetResolver.at(level, pos).planet();
        if (p == null || !p.raw().has("gas_collector_output") || !p.raw().get("gas_collector_output").isJsonObject()) {
            return null;
        }
        return p.raw().getAsJsonObject("gas_collector_output");
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        JsonObject out = output(level, getBlockPos());
        if (out == null || !out.has("fluid")) {
            return Optional.empty();
        }
        IhFluidStack gas = IhFluidStack.of(Identifier.parse(out.get("fluid").getAsString()), out.get("amount").getAsLong());
        return Optional.of(new Plan(new int[0], new int[0], List.of(), Optional.of(out.get("ticks").getAsInt()),
                Optional.empty(), IhFluidStack.EMPTY, List.of(gas)));
    }
}
