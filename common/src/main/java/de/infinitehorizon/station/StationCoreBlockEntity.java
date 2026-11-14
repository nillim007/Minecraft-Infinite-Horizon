package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.dome.DomeGeneratorBlockEntity;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stationskern (14.1): eingebauter Kuppelgenerator (Werte der Kuppelstufe aus domes.json), eingebaute Solarzellen
 * ({@code panels} × {@code output_per_panel} × Solarfaktor des Orbits), O₂-Tank, nimmt zusätzlich Strom über Kabel an.
 */
public class StationCoreBlockEntity extends DomeGeneratorBlockEntity {

    public StationCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static JsonObject config() {
        return BalanceManager.get().entry("stations", InfiniteHorizon.id("station_core").toString()).orElse(new JsonObject());
    }

    @Override
    protected JsonObject json() {
        JsonObject dome = config().has("builtin_dome") ? config().getAsJsonObject("builtin_dome") : new JsonObject();
        int tier = dome.has("tier") ? dome.get("tier").getAsInt() : 1;
        JsonObject base = BalanceManager.get().entry("domes", InfiniteHorizon.id("dome_generator_t" + tier).toString())
                .orElse(new JsonObject()).deepCopy();
        if (dome.has("radius")) {
            base.add("radius", dome.get("radius"));
        }
        if (config().has("o2_tank")) {
            base.add("o2_tank", config().get("o2_tank"));
        }
        return base;
    }

    /** Leistung der eingebauten Solarzellen in J/t an dieser Stelle. */
    public double solarOutput(ServerLevel level) {
        JsonObject s = config().has("builtin_solar") ? config().getAsJsonObject("builtin_solar") : new JsonObject();
        Planet planet = PlanetResolver.at(level, worldPosition).planet();
        double factor = planet == null ? 0 : planet.solarFactor();
        return StationRules.builtinSolar(s.has("panels") ? s.get("panels").getAsInt() : 0,
                s.has("output_per_panel") ? s.get("output_per_panel").getAsDouble() : 0, factor)
                * de.infinitehorizon.event.EventService.solarMultiplier(level, worldPosition);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (ensureLayout()) {
            long produced = Math.round(solarOutput(level));
            if (produced > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.produceInternal(produced, tx);
                    tx.commit();
                }
            }
        }
        super.serverTick(level);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            StationData.get(sl).remove(pos);
        }
    }
}
