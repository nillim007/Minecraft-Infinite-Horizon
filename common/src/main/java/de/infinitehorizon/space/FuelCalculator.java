package de.infinitehorizon.space;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Treibstoffkosten (12.2) als reine Funktion, je Treibstoffkomponente in mB:
 * {@code kosten(A → B) = start(A) + transfer(A, B) + landung(B)}. Rechnet in {@code double} und rundet jeden der drei
 * Summanden mit {@link Math#round} auf ganze mB ({@code fuel_calculation} in spaceflight.json). A = B ist erlaubt
 * (ohne Transfer).
 */
public final class FuelCalculator {

    /** Konstanten aus spaceflight.json ({@code launch_cost}, {@code transfer_cost}, {@code landing_cost}). */
    public record Constants(double perG, double perDp, double differentBody, double landingFactor, double landingPerG) {

        public static Constants from(BalanceManager b) {
            JsonObject launch = b.entry("spaceflight", "launch_cost").orElse(new JsonObject());
            JsonObject transfer = b.entry("spaceflight", "transfer_cost").orElse(new JsonObject());
            JsonObject landing = b.entry("spaceflight", "landing_cost").orElse(new JsonObject());
            return new Constants(num(launch, "per_g"), num(transfer, "per_dp"), num(transfer, "different_body"),
                    num(landing, "factor"), num(landing, "per_g"));
        }
    }

    /**
     * Ein Ziel (Planeten-Eintrag, 25.1): Schwerkraft, Distanzpunkte, ob die Landung Treibstoff kostet, ggf. ein fester
     * Startwert ({@code launch_cost_override}: Orbits 0, Venus-Wolkenschicht).
     */
    public record Body(Identifier id, double gravity, double distancePoints, boolean landingRequiresFuel,
                       @Nullable Double launchOverride) {

        /** Aus einem Planeten-Eintrag (Laufzeitdatei oder spec/data/planets.json). */
        public static Body of(JsonObject o) {
            Identifier id = Identifier.parse((o.has("dimension") ? o.get("dimension") : o.get("id")).getAsString());
            JsonElement override = o.get("launch_cost_override");
            return new Body(id, num(o, "gravity"), num(o, "distance_points"),
                    !o.has("landing_requires_fuel") || o.get("landing_requires_fuel").getAsBoolean(),
                    override == null || override.isJsonNull() ? null : override.getAsDouble());
        }

        public static Body of(Planet planet) {
            return of(planet.raw());
        }
    }

    private FuelCalculator() {
    }

    /** start(A) = per_g × g(A); Vorrang hat {@code launch_cost_override}. */
    public static long launch(Body a, Constants c) {
        return Math.round(a.launchOverride() != null ? a.launchOverride() : c.perG() * a.gravity());
    }

    /** transfer(A, B) = per_dp × |DP(A) − DP(B)| + different_body (nur bei A ≠ B). */
    public static long transfer(Body a, Body b, Constants c) {
        if (a.id().equals(b.id())) {
            return 0;
        }
        return Math.round(c.perDp() * Math.abs(a.distancePoints() - b.distancePoints()) + c.differentBody());
    }

    /** landung(B) = factor × per_g × g(B); 0, wenn {@code landing_requires_fuel} false ist. */
    public static long landing(Body b, Constants c) {
        return b.landingRequiresFuel() ? Math.round(c.landingFactor() * c.landingPerG() * b.gravity()) : 0;
    }

    /** Kosten A → B je Komponente. */
    public static long cost(Body a, Body b, Constants c) {
        return launch(a, c) + transfer(a, b, c) + landing(b, c);
    }

    /** Landefähre (12.7): Kosten nach 12.2 ohne {@code transfer}. */
    public static long landerCost(Body a, Body b, Constants c) {
        return launch(a, c) + landing(b, c);
    }

    public static Reach landerReach(long fuelPerComponent, Body from, Body to, Constants c) {
        if (fuelPerComponent >= landerCost(from, to, c) + landerCost(to, from, c)) {
            return Reach.BOTH;
        }
        return fuelPerComponent >= landerCost(from, to, c) ? Reach.ONE_WAY : Reach.NONE;
    }

    /** Hin- und Rückflug A → B → A. */
    public static long roundTrip(Body a, Body b, Constants c) {
        return cost(a, b, c) + cost(b, a, c);
    }

    /** Farbe eines Ziels im Raketen-Computer (12.2): grün = hin und zurück, gelb = nur hin, grau = nicht erreichbar. */
    public enum Reach { BOTH, ONE_WAY, NONE }

    public static Reach reach(long fuelPerComponent, Body from, Body to, Constants c) {
        if (fuelPerComponent >= roundTrip(from, to, c)) {
            return Reach.BOTH;
        }
        return fuelPerComponent >= cost(from, to, c) ? Reach.ONE_WAY : Reach.NONE;
    }

    private static double num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsDouble() : 0;
    }
}
