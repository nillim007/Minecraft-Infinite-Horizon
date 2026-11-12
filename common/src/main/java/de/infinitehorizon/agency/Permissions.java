package de.infinitehorizon.agency;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Rechteprüfung (23.1, 23.2). In M3 ist alles erlaubt; Gast-Rechte und Kuppelschutz wirken ab M4, wenn man am
 * {@code research_terminal} einer Agentur beitreten kann. Alle Stellen, die später geprüft werden, rufen schon jetzt
 * {@link #can} auf.
 */
public final class Permissions {

    /** Was ein Spieler an einem Block oder Fahrzeug tun will. */
    public enum Action { OPEN, USE_SLOTS, BUILD, BREAK, CONFIGURE, LAUNCH }

    private Permissions() {
    }

    /** Darf {@code player} das an etwas tun, das {@code owner} gehört (null = ohne Besitzer)? In M3 immer true. */
    public static boolean can(Player player, @Nullable UUID owner, Action action) {
        return true;
    }
}
