package de.infinitehorizon.client;

import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.ResearchRegistry;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.network.AgencyStatePayload;
import de.infinitehorizon.recipe.ResearchRecipeLock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Stand der eigenen Agentur auf dem Client ({@link AgencyStatePayload}, 23.1): Forschungsbaum, Handbuch-Kapitel
 * ({@code unlock}, 24.1) und Schloss in JEI/REI. Nur Anzeige, der Server entscheidet.
 */
public final class AgencyClientState {

    private static volatile @Nullable AgencyStatePayload state;

    private AgencyClientState() {
    }

    public static void receive(AgencyStatePayload payload) {
        state = payload;
    }

    /** Letzter Stand oder null (noch nicht empfangen, z. B. im Hauptmenü). */
    public static @Nullable AgencyStatePayload get() {
        return state;
    }

    public static void clear() {
        state = null;
    }

    public static Set<Identifier> researched() {
        AgencyStatePayload s = state;
        return s == null ? Set.of() : Set.copyOf(s.researched());
    }

    public static Set<Integer> openTiers() {
        AgencyStatePayload s = state;
        return s == null ? Set.of() : Set.copyOf(s.openTiers());
    }

    /** FP in Hundertsteln. */
    public static int fp() {
        AgencyStatePayload s = state;
        return s == null ? 0 : s.fp();
    }

    /** Anzeigename der Agentur. */
    public static Component name() {
        AgencyStatePayload s = state;
        if (s == null) {
            return Component.empty();
        }
        return s.name().isEmpty() ? Component.translatable(AgencyManager.DEFAULT_NAME_KEY, s.founder()) : Component.literal(s.name());
    }

    /** Ist die Forschungssperre auf dem Server aktiv (Config {@code research_required}, 24.4)? */
    public static boolean researchRequired() {
        return ServerConfig.client().getBoolean(ResearchRecipeLock.CONFIG_KEY);
    }

    /**
     * Handbuch-Freischaltung (24.1, 25.6): {@code null} = offen; {@code <ns>:research_tier/<n>} = Stufe n offen; sonst
     * ein Forschungsknoten. Mit {@code research_required = false} ist alles offen.
     */
    public static boolean isUnlocked(@Nullable String unlock) {
        if (unlock == null || unlock.isEmpty() || !researchRequired()) {
            return true;
        }
        Identifier id = Identifier.tryParse(unlock);
        if (id == null) {
            return true;
        }
        String path = id.getPath();
        if (path.startsWith("research_tier/")) {
            try {
                return openTiers().contains(Integer.parseInt(path.substring("research_tier/".length())));
            } catch (NumberFormatException e) {
                return true;
            }
        }
        // unbekannte Knoten (noch nicht sichtbar) sperren nicht
        return ResearchRegistry.client().node(id).isEmpty() || researched().contains(id);
    }

    /** Ist das Item für die eigene Agentur herstellbar (Schloss in JEI/REI, 11.3)? */
    public static boolean isItemUnlocked(Identifier item) {
        return !researchRequired() || ResearchRecipeLock.isUnlocked(Set.of(item), ResearchRegistry.client(), researched());
    }
}
