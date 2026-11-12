package de.infinitehorizon.environment;

import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Umwelt-Override für GameTests (Einleitung Kapitel 27): ordnet der Box eines Testbereichs einen Planeten, eine Zone
 * und eine Ortsbindung zu. {@link PlanetResolver} fragt ihn zuerst. Nur im GameTest-Betrieb aktiv.
 */
public final class EnvironmentOverrides {

    /**
     * Ein Eintrag; {@code zone} = Zonen-ID des Planeten (nur diese Zone gilt), {@code binding} = Ortsbindung
     * (z. B. „gilt als Orbit“, ab M4). Läuft spätestens nach {@code expiresAt} (Spielzeit) ab, falls der Test abbricht.
     */
    public record Entry(ResourceKey<Level> level, AABB box, Identifier planet, @Nullable String zone,
                        @Nullable String binding, long expiresAt) {
    }

    private static final List<Entry> ACTIVE = new CopyOnWriteArrayList<>();

    private EnvironmentOverrides() {
    }

    /**
     * Setzt den Override für den Testbereich von {@code h}. Am Ende {@link #remove} aufrufen; bricht der Test ab,
     * verfällt der Eintrag nach {@code maxTicks}.
     */
    public static Entry set(GameTestHelper h, Identifier planet, @Nullable String zone, @Nullable String binding, int maxTicks) {
        ServerLevel level = h.getLevel();
        Entry e = new Entry(level.dimension(), h.getBounds(), planet, zone, binding, level.getGameTime() + maxTicks);
        if (Services.PLATFORM.isGameTest()) {
            ACTIVE.add(e);
        }
        return e;
    }

    public static void remove(Entry e) {
        ACTIVE.remove(e);
    }

    /** Gibt es gerade überhaupt einen Override (schnelle Vorprüfung)? */
    public static boolean anyActive() {
        return !ACTIVE.isEmpty();
    }

    /** Override an der Position, falls einer gilt. */
    public static Optional<Entry> at(ServerLevel level, BlockPos pos) {
        if (ACTIVE.isEmpty()) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        ACTIVE.removeIf(e -> e.expiresAt() < now);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        return ACTIVE.stream()
                .filter(e -> e.level().equals(level.dimension()) && e.box().contains(x, y, z))
                .findFirst();
    }
}
