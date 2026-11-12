package de.infinitehorizon.environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regeln der Umweltrechnung aus environment.json (8.1–8.3): Tageslichtkurve, Strahlungsregeln, Schutz ohne Anzug,
 * Prüftakt und je Gefahr Aufbau, Abbau und Schaden. Reine Daten, damit {@link EnvironmentCalculator} ohne Server
 * testbar bleibt.
 */
public record EnvironmentRules(List<double[]> daylightCurve, RadiationRules radiation, Protection noSuit,
                               int intervalTicks, Map<Hazard, HazardRule> hazards, Map<Hazard, Identifier> damageTypes) {

    private static final String FILE = "environment";

    /** Strahlung (8.1): Grenzen, Abdeckung, Sonnensturm, Kuppel T3. */
    public record RadiationRules(int min, int max, int coverBlocks, int coverReduction, int solarStormBonus, int domeT3Value) {
    }

    /** Eine Gefahr (8.2): Belastung in Ticks, Schaden bei 100 %; {@code overrides} für Hitze über X °C und Strahlungsstufen. */
    public record HazardRule(int buildupTicks, int decayTicks, float damage, int damageInterval,
                             List<DamageOverride> overrides) {

        /** Abweichender Schaden: ab Temperatur ({@code aboveTemperature}) oder ab Strahlungsstufe ({@code level}). */
        public record DamageOverride(Double aboveTemperature, Integer level, float damage, int damageInterval) {
        }

        /** Schaden und Takt für diesen Zustand (Hitze über 300 °C, Strahlung 3 …). */
        public DamageOverride damageFor(double temperature, int radiationLevel) {
            DamageOverride best = new DamageOverride(null, null, damage, damageInterval);
            for (DamageOverride o : overrides) {
                if (o.aboveTemperature() != null && temperature > o.aboveTemperature()) {
                    best = o;
                }
                if (o.level() != null && radiationLevel >= o.level()) {
                    best = o;
                }
            }
            return best;
        }
    }

    public static EnvironmentRules from(BalanceManager balance) {
        JsonObject curve = entry(balance, "daylight_curve");
        List<double[]> points = new ArrayList<>();
        for (JsonElement p : curve.getAsJsonArray("points")) {
            JsonArray a = p.getAsJsonArray();
            points.add(new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()});
        }
        JsonObject r = entry(balance, "radiation_rules");
        RadiationRules radiation = new RadiationRules(r.get("min").getAsInt(), r.get("max").getAsInt(),
                r.get("cover_blocks").getAsInt(), r.get("cover_reduction").getAsInt(), r.get("solar_storm_bonus").getAsInt(),
                r.get("dome_t3_value").getAsInt());
        JsonObject t = entry(balance, "no_suit_tolerance");
        Set<Pressure> pressures = EnumSet.noneOf(Pressure.class);
        t.getAsJsonArray("pressure").forEach(e -> pressures.add(Pressure.byId(e.getAsString())));
        Protection noSuit = new Protection(t.get("temp_min").getAsDouble(), t.get("temp_max").getAsDouble(), pressures,
                t.get("radiation_max").getAsInt(), false, false);
        int interval = entry(balance, "environment_update").get("interval_ticks").getAsInt();
        Map<Hazard, HazardRule> hazards = new EnumMap<>(Hazard.class);
        for (Hazard h : Hazard.values()) {
            JsonObject o = entry(balance, h.id());
            List<HazardRule.DamageOverride> overrides = new ArrayList<>();
            if (o.has("damage_overrides")) {
                for (JsonElement e : o.getAsJsonArray("damage_overrides")) {
                    JsonObject d = e.getAsJsonObject();
                    overrides.add(new HazardRule.DamageOverride(d.get("above_temperature").getAsDouble(), null,
                            d.get("damage").getAsFloat(), d.get("damage_interval").getAsInt()));
                }
            }
            if (o.has("damage_by_level")) {
                o.getAsJsonObject("damage_by_level").entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> overrides.add(new HazardRule.DamageOverride(null, Integer.parseInt(e.getKey()),
                                e.getValue().getAsJsonObject().get("damage").getAsFloat(),
                                e.getValue().getAsJsonObject().get("damage_interval").getAsInt())));
            }
            hazards.put(h, new HazardRule(o.get("buildup_ticks").getAsInt(), o.get("decay_ticks").getAsInt(),
                    o.get("damage").getAsFloat(), o.get("damage_interval").getAsInt(), List.copyOf(overrides)));
        }
        Map<Hazard, Identifier> damageTypes = new EnumMap<>(Hazard.class);
        entry(balance, "hazard_rules").getAsJsonObject("damage_types").entrySet()
                .forEach(e -> damageTypes.put(Hazard.byId(e.getKey()), Identifier.parse(e.getValue().getAsString())));
        return new EnvironmentRules(List.copyOf(points), radiation, noSuit, interval, Map.copyOf(hazards),
                Map.copyOf(damageTypes));
    }

    private static JsonObject entry(BalanceManager balance, String key) {
        return balance.entry(FILE, key).orElseThrow(() -> new IllegalStateException("environment.json: " + key + " fehlt"));
    }

    /** Tageslicht 0–1 nach der Kurve (linear), Tageszeit der Overworld modulo Kurvenlänge. */
    public double daylight(long dayTime) {
        double length = daylightCurve.getLast()[0];
        double t = Math.floorMod(dayTime, (long) length);
        for (int i = 1; i < daylightCurve.size(); i++) {
            double[] a = daylightCurve.get(i - 1);
            double[] b = daylightCurve.get(i);
            if (t <= b[0]) {
                double span = b[0] - a[0];
                return span <= 0 ? b[1] : a[1] + (b[1] - a[1]) * (t - a[0]) / span;
            }
        }
        return daylightCurve.getLast()[1];
    }
}
