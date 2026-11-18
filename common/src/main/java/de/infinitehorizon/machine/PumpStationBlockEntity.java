package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.event.EventService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code pump_station} (6.1): fördert aus dem Block direkt darunter ({@code source_map}: Geysire und Fluid-Quellblöcke)
 * je Vorgang {@code output_per_cycle} mB, ohne die Quelle zu verbrauchen. Steht sie auf einem Ponton, zählt der Block
 * darunter. Wetter mit {@code pump_station_multiplier} (Methanregen) erhöht die Menge.
 */
public class PumpStationBlockEntity extends ProcessingMachineBlockEntity {

    public PumpStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private JsonObject process() {
        JsonObject m = BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
        return m.has("process") ? m.getAsJsonObject("process") : new JsonObject();
    }

    /** Fluid aus der Quelle unter der Station oder null. */
    public @Nullable Identifier source(ServerLevel level) {
        BlockPos below = getBlockPos().below();
        if (level.getBlockState(below).is(de.infinitehorizon.registry.ModBlocks.PONTOON.get())) {
            below = below.below();
        }
        JsonObject map = process().has("source_map") ? process().getAsJsonObject("source_map") : new JsonObject();
        BlockState s = level.getBlockState(below);
        String block = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
        if (map.has(block) && s.getFluidState().isEmpty()) {
            return Identifier.parse(map.get(block).getAsString());
        }
        FluidState f = s.getFluidState();
        if (f.isSource()) {
            String fluid = BuiltInRegistries.FLUID.getKey(f.getType()).toString();
            if (map.has(fluid)) {
                return Identifier.parse(map.get(fluid).getAsString());
            }
        }
        return null;
    }

    /** Menge je Vorgang mit Wetter-Bonus (19: Methanregen). */
    public long amount(ServerLevel level) {
        return Math.round(process().get("output_per_cycle").getAsLong() * EventService.pumpMultiplier(level));
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        Identifier fluid = source(level);
        if (fluid == null) {
            return Optional.empty();
        }
        return Optional.of(new Plan(new int[0], new int[0], List.of(), Optional.empty(), Optional.empty(), IhFluidStack.EMPTY,
                List.of(IhFluidStack.of(fluid, amount(level)))));
    }
}
