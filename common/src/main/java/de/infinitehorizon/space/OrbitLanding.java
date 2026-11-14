package de.infinitehorizon.space;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.station.StationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Ankunft im Orbit ohne {@code docking_bay} (12.5, 7.): Die Rakete schwebt auf {@code y} an der gewählten X/Z-Position.
 * Ist dort keine freie Säule oder liegt ein fremder Stationskern zu nah, sucht der Server spiralförmig weiter.
 */
public final class OrbitLanding {

    private OrbitLanding() {
    }

    private static JsonObject rules() {
        return BalanceManager.get().entry("spaceflight", "orbit_arrival").orElse(new JsonObject());
    }

    public static BlockPos find(ServerLevel level, int x, int z, @Nullable UUID agency) {
        JsonObject o = rules();
        int y = o.get("y").getAsInt();
        JsonArray col = o.getAsJsonArray("clear_column");
        int w = col.get(0).getAsInt();
        int d = col.get(1).getAsInt();
        int h = col.get(2).getAsInt();
        int max = o.get("max_search_radius").getAsInt();
        int foreign = o.get("min_distance_foreign_station_core").getAsInt();
        StationData stations = StationData.get(level);
        // SPEC-GAP #96: Suche in Schritten der Säulenbreite (Ringe um X/Z), damit nicht jede Spalte geladen wird
        int step = Math.max(1, w);
        for (int r = 0; r <= max; r += step) {
            for (int dx = -r; dx <= r; dx += step) {
                for (int dz = -r; dz <= r; dz += step) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos at = new BlockPos(x + dx, y, z + dz);
                    if (!stations.foreignWithin(at, foreign, agency) && clear(level, at, w, d, h)) {
                        return at;
                    }
                }
            }
        }
        return new BlockPos(x, y, z);
    }

    /**
     * Stehplatz im Orbit (Befehl {@code goto}, Absturz in einen Orbit): Schwebepunkt mit kleiner Plattform aus
     * generierter Stationshülle ({@code fallback_platform} aus {@code orbit_fall_rules}).
     */
    public static BlockPos standOn(ServerLevel level, int x, int z) {
        BlockPos at = find(level, x, z, null);
        JsonObject fall = BalanceManager.get().entry("stations", "orbit_fall_rules").orElse(new JsonObject());
        JsonObject giants = fall.has("gas_giant_orbits") ? fall.getAsJsonObject("gas_giant_orbits") : new JsonObject();
        JsonObject plat = giants.has("fallback_platform") ? giants.getAsJsonObject("fallback_platform") : new JsonObject();
        int half = (plat.has("size") ? plat.get("size").getAsInt() : 1) / 2;
        var hull = de.infinitehorizon.registry.ModBlocks.STATION_HULL.get().defaultBlockState()
                .setValue(de.infinitehorizon.station.StationHullBlock.GENERATED, true);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos p = at.offset(dx, -1, dz);
                if (level.getBlockState(p).isAir()) {
                    level.setBlockAndUpdate(p, hull);
                }
            }
        }
        return at;
    }

    private static boolean clear(ServerLevel level, BlockPos base, int w, int d, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = -w / 2; dx <= w / 2; dx++) {
                for (int dz = -d / 2; dz <= d / 2; dz++) {
                    if (!level.getBlockState(base.offset(dx, dy, dz)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
