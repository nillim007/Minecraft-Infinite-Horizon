package de.infinitehorizon.dome;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.network.DomeStatePayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kuppeln je Dimension (3A.3, 10.1): räumlicher Index (Chunk → Kuppeln) über {@link DomeData}; Abfrage, ob eine
 * Position in einer aktiven Kuppel liegt ({@code failing} zählt als aktiv). Überlappende Kuppeln bilden einen
 * gemeinsamen Luftraum. Sync an die Clients über {@link DomeStatePayload}.
 */
public final class DomeManager {

    private record Index(int version, Map<Long, List<DomeData.Entry>> byChunk) {
    }

    private static final Map<ResourceKey<Level>, Index> INDEX = new HashMap<>();

    private DomeManager() {
    }

    /** Werte aus domes.json ({@code dome_rules}). */
    public static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("domes", "dome_rules").orElse(new JsonObject());
    }

    private static int belowCenter() {
        JsonObject r = rules();
        return r.has("below_center_offset") ? r.get("below_center_offset").getAsInt() : 0;
    }

    private static Index index(ServerLevel level) {
        DomeData data = DomeData.get(level);
        Index idx = INDEX.get(level.dimension());
        if (idx != null && idx.version() == data.version()) {
            return idx;
        }
        Map<Long, List<DomeData.Entry>> map = new HashMap<>();
        for (DomeData.Entry e : data.entries()) {
            int r = e.radius();
            ChunkPos min = ChunkPos.containing(e.center().offset(-r, 0, -r));
            ChunkPos max = ChunkPos.containing(e.center().offset(r, 0, r));
            for (int cx = min.x(); cx <= max.x(); cx++) {
                for (int cz = min.z(); cz <= max.z(); cz++) {
                    map.computeIfAbsent(ChunkPos.pack(cx, cz), k -> new ArrayList<>()).add(e);
                }
            }
        }
        idx = new Index(data.version(), map);
        INDEX.put(level.dimension(), idx);
        return idx;
    }

    /** Aktive Kuppel an der Position (die mit der höchsten Stufe), sonst leer. */
    public static Optional<DomeData.Entry> domeAt(ServerLevel level, BlockPos pos) {
        List<DomeData.Entry> list = index(level).byChunk().get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        if (list == null) {
            return Optional.empty();
        }
        int below = belowCenter();
        DomeData.Entry best = null;
        for (DomeData.Entry e : list) {
            if (e.contains(pos, below) && (best == null || e.tier() > best.tier())) {
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean isInsideActiveDome(ServerLevel level, BlockPos pos) {
        return domeAt(level, pos).isPresent();
    }

    /** Kuppeln der Dimension an alle Spieler darin. */
    public static void sync(ServerLevel level) {
        DomeStatePayload payload = DomeStatePayload.of(DomeData.get(level).entries());
        for (ServerPlayer p : level.players()) {
            Services.NETWORK.sendToPlayer(p, payload);
        }
    }

    /** Kuppeln der aktuellen Dimension an einen Spieler (Login, Dimensionswechsel). */
    public static void sync(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, DomeStatePayload.of(DomeData.get(player.level()).entries()));
    }
}
