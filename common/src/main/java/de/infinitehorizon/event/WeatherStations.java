package de.infinitehorizon.event;

import de.infinitehorizon.agency.AgencyData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Weltraumwetter-Stationen (6.1, 19): Laufende Stationen melden sich jeden Tick; die Agentur bekommt dann in derselben
 * Dimension die frühe Warnung ({@code warning_ticks_with_station}). Nur Laufzeitdaten, nichts wird gespeichert.
 */
public final class WeatherStations {

    /** Letzter Tick, in dem eine Station lief: Dimension → (Position → Agentur, Tick). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Seen>> ACTIVE = new ConcurrentHashMap<>();
    /** Agenturen, die für das geplante Ereignis (Start-Tick) schon früh gewarnt wurden. */
    private static final Map<ResourceKey<Level>, Map<Long, Set<UUID>>> WARNED = new ConcurrentHashMap<>();

    private record Seen(UUID agency, long tick) {
    }

    private WeatherStations() {
    }

    /** Eine Station mit Strom läuft in diesem Tick. */
    public static void running(ServerLevel level, BlockPos pos, @Nullable UUID agency) {
        Map<BlockPos, Seen> m = ACTIVE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (agency == null) {
            m.remove(pos);
        } else {
            m.put(pos.immutable(), new Seen(agency, level.getGameTime()));
        }
    }

    public static void stopped(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Seen> m = ACTIVE.get(level.dimension());
        if (m != null) {
            m.remove(pos);
        }
    }

    /** Agenturen mit einer laufenden Station in dieser Dimension (zuletzt vor höchstens 1 Tick gemeldet). */
    public static Set<UUID> agencies(ServerLevel level) {
        Set<UUID> out = new HashSet<>();
        Map<BlockPos, Seen> m = ACTIVE.get(level.dimension());
        if (m != null) {
            long now = level.getGameTime();
            m.values().removeIf(s -> s.tick() < now - 1);
            m.values().forEach(s -> out.add(s.agency()));
        }
        return out;
    }

    static @Nullable UUID agencyOf(ServerPlayer player) {
        return AgencyData.get(player.level().getServer()).agencyOf(player.getUUID()).map(a -> a.id()).orElse(null);
    }

    /** Wurde die Agentur des Spielers für das Ereignis mit Start {@code start} schon früh gewarnt? */
    public static boolean warned(ServerPlayer player, long start) {
        UUID a = agencyOf(player);
        Map<Long, Set<UUID>> m = WARNED.get(player.level().dimension());
        return a != null && m != null && m.getOrDefault(start, Set.of()).contains(a);
    }

    static boolean markWarned(ServerLevel level, long start, UUID agency) {
        Map<Long, Set<UUID>> m = WARNED.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        m.keySet().removeIf(s -> s < level.getGameTime() - 1);
        return m.computeIfAbsent(start, k -> new HashSet<>()).add(agency);
    }

    /** Für Tests: alle Laufzeitdaten der Dimension vergessen. */
    public static void reset(ServerLevel level) {
        ACTIVE.remove(level.dimension());
        WARNED.remove(level.dimension());
    }
}
