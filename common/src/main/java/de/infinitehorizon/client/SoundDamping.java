package de.infinitehorizon.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.Pressure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Dämpfung im Vakuum (24.6, {@code misc.json → vacuum_sound}): Im Vakuum ohne Kuppel spielt der Client alle Geräusche
 * mit {@code volume_multiplier}, außer Anzug- und Warntönen ({@code exempt}: {@code suit_sounds} = {@code suit.*},
 * {@code warning_sounds} = {@code warning.*}). Musik und Menütöne bleiben unverändert (SPEC-GAP #121). Ob Vakuum
 * herrscht, kommt mit dem Umweltzustand vom Server (eine Kuppel setzt den Druck auf normal).
 */
public final class SoundDamping {

    private SoundDamping() {
    }

    /** Ist der Spieler gerade im Vakuum ohne Kuppel? */
    public static boolean vacuum() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator()) {
            return false;
        }
        var st = HazardHud.state();
        return st != null && st.pressure() == Pressure.VACUUM.ordinal();
    }

    private static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("misc", "vacuum_sound").orElse(new JsonObject());
    }

    /** Ausgenommene Klänge als Präfix ({@code suit_sounds} → {@code suit.}). */
    private static List<String> exemptPrefixes(JsonObject r) {
        List<String> out = new ArrayList<>();
        if (r.has("exempt") && r.get("exempt").isJsonArray()) {
            JsonArray a = r.getAsJsonArray("exempt");
            a.forEach(e -> out.add(e.getAsString().replace("_sounds", ".")));
        }
        return out;
    }

    /** Faktor für einen Klang (1 = ungedämpft). */
    public static float factor(SoundInstance sound) {
        if (!vacuum()) {
            return 1f;
        }
        JsonObject r = rules();
        double m = r.has("volume_multiplier") ? r.get("volume_multiplier").getAsDouble() : 1.0;
        return (float) factor(true, sound.getIdentifier(), sound.getSource(), m, exemptPrefixes(r));
    }

    /** Reine Regel: Faktor für Klang {@code id} aus {@code source}. */
    public static double factor(boolean vacuum, Identifier id, SoundSource source, double multiplier, List<String> exempt) {
        if (!vacuum || source == SoundSource.MUSIC || source == SoundSource.UI || source == SoundSource.MASTER) {
            return 1.0;
        }
        if (InfiniteHorizon.MOD_ID.equals(id.getNamespace())) {
            for (String prefix : exempt) {
                if (id.getPath().startsWith(prefix)) {
                    return 1.0;
                }
            }
        }
        return multiplier;
    }
}
