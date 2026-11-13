package de.infinitehorizon.environment;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Hitzezeit der Venus-Oberfläche (9.1, suits.json {@code venus_heat_timer}): Mit einem Anzug, der den Timer hat, läuft
 * unterhalb {@code applies_below_y} in der Venus ein Zähler bis {@code duration_ticks}; danach gilt der Spieler als
 * ungeschützt gegen Hitze. Oberhalb von {@code refill_above_y} (und außerhalb der Venus) füllt er sich in
 * {@code refill_ticks} wieder auf.
 */
public record VenusHeat(Identifier dimension, int belowY, int duration, int refillAboveY, int refillTicks) {

    public static @Nullable VenusHeat rules() {
        JsonObject o = BalanceManager.getOrBundled().entry("suits", "venus_heat_timer").orElse(null);
        if (o == null || !o.has("dimension")) {
            return null;
        }
        return new VenusHeat(Identifier.parse(o.get("dimension").getAsString()), o.get("applies_below_y").getAsInt(),
                o.get("duration_ticks").getAsInt(), o.get("refill_above_y").getAsInt(), o.get("refill_ticks").getAsInt());
    }

    /** Läuft der Timer an diesem Ort? */
    public boolean running(@Nullable Identifier planet, int y) {
        return dimension.equals(planet) && y < belowY;
    }

    /**
     * Ein Schritt über {@code ticks} (reine Funktion): unten +ticks bis {@code duration}, sonst Auffüllen um
     * {@code ticks × duration / refill_ticks}, nie unter 0.
     */
    public int step(int used, boolean running, int ticks) {
        if (running) {
            return Math.min(duration, used + ticks);
        }
        int refill = refillTicks <= 0 ? used : (int) Math.ceil((double) ticks * duration / refillTicks);
        return Math.max(0, used - refill);
    }

    public boolean expired(int used) {
        return used >= duration;
    }
}
