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

    /** Gas-Angabe des Planeten an dieser Stelle ({@code gas_collector_output}), sonst null. */
    public static @Nullable JsonObject output(ServerLevel level, BlockPos pos) {
        Planet p = PlanetResolver.at(level, pos).planet();
        if (p == null || !p.raw().has("gas_collector_output") || !p.raw().get("gas_collector_output").isJsonObject()) {
            return null;
        }
        return p.raw().getAsJsonObject("gas_collector_output");
    }

    /** Zwei Betriebsarten, wenn der Planet ein {@code alternative}-Gas hat (13.3: Uranus, Neptun). */
    @Override
    public int modeCount() {
        JsonObject out = level instanceof ServerLevel sl ? output(sl, getBlockPos()) : null;
        return out != null && out.has("alternative") && out.get("alternative").isJsonPrimitive() ? 2 : 0;
    }

    /** Gesammeltes Gas: Standard {@code fluid}, in Betriebsart 1 {@code alternative}. */
    public @Nullable Identifier gas(ServerLevel level) {
        JsonObject out = output(level, getBlockPos());
        if (out == null || !out.has("fluid")) {
            return null;
        }
        String key = mode() == 1 && out.has("alternative") && out.get("alternative").isJsonPrimitive() ? "alternative" : "fluid";
        return Identifier.parse(out.get(key).getAsString());
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        JsonObject out = output(level, getBlockPos());
        Identifier fluid = gas(level);
        if (out == null || fluid == null) {
            return Optional.empty();
        }
        IhFluidStack gas = IhFluidStack.of(fluid, out.get("amount").getAsLong());
        return Optional.of(new Plan(new int[0], new int[0], List.of(), Optional.of(out.get("ticks").getAsInt()),
                Optional.empty(), IhFluidStack.EMPTY, List.of(gas)));
    }
}
