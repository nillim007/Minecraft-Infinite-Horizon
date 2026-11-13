package de.infinitehorizon.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.environment.Pressure;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Laufzeitdaten eines Planeten (25.1) aus {@code data/<ns>/infinite_horizon/planet/*.json}. Nur die Felder, die der
 * Code liest; die ganze Datei bleibt in {@link #raw()} (Himmel, Sonden, Raketen lesen später daraus).
 */
public record Planet(Identifier dimension, String type, @Nullable String name, double gravity, String atmosphere,
                     Pressure pressure, double temperatureDay, double temperatureNight,
                     Map<Identifier, Double> biomeTemperatureOffsets, Map<Identifier, Double> biomeTemperatureOverrides,
                     int radiation, double solarFactor, String waterBehavior, List<Zone> zones, JsonObject raw) {

    public static final String BREATHABLE = "breathable";
    public static final String ORBIT = "orbit";

    /** Zone (25.1): überschreibt einzelne Werte in ihrem Bereich; {@code null} = Planetenwert behalten. */
    public record Zone(@Nullable String id, int yMin, int yMax, @Nullable Identifier biome, @Nullable Identifier nearBlock,
                       @Nullable Double temperature, @Nullable Pressure pressure, @Nullable Integer radiation,
                       @Nullable Double solarFactor, @Nullable String atmosphere, @Nullable String waterBehavior) {

        static Zone fromJson(JsonObject o) {
            return new Zone(str(o, "id"), o.get("y_min").getAsInt(), o.get("y_max").getAsInt(), ident(o, "biome"),
                    ident(o, "near_block"), dbl(o, "temperature"), Pressure.byId(str(o, "pressure")), integer(o, "radiation"),
                    dbl(o, "solar_factor"), str(o, "atmosphere"), str(o, "water_behavior"));
        }

        public boolean containsY(int y) {
            return y >= yMin && y <= yMax;
        }
    }

    /** Ring (12.1) aus dem Eintrag, sonst -1. Bestimmt den FP-Wert von Proben (11.1). */
    public int ring() {
        return raw.has("ring") && raw.get("ring").isJsonPrimitive() ? raw.get("ring").getAsInt() : -1;
    }

    public boolean breathable() {
        return BREATHABLE.equals(atmosphere);
    }

    public boolean isOrbit() {
        return ORBIT.equals(type);
    }

    /** Liest einen Eintrag; Felder wie in 25.1 (auch die vollständigen Einträge aus spec/data/planets.json). */
    public static Planet fromJson(JsonObject o) {
        JsonObject temp = o.getAsJsonObject("temperature");
        List<Zone> zones = new ArrayList<>();
        if (o.has("zones") && o.get("zones").isJsonArray()) {
            o.getAsJsonArray("zones").forEach(z -> zones.add(Zone.fromJson(z.getAsJsonObject())));
        }
        Pressure pressure = Pressure.byId(str(o, "pressure"));
        return new Planet(Identifier.parse(o.get("dimension").getAsString()), strOr(o, "type", "surface"), str(o, "name"),
                o.get("gravity").getAsDouble(), strOr(o, "atmosphere", BREATHABLE),
                pressure == null ? Pressure.NORMAL : pressure,
                temp.get("day").getAsDouble(), temp.get("night").getAsDouble(),
                doubleMap(o, "biome_temperature_offsets"), doubleMap(o, "biome_temperature_overrides"),
                o.get("radiation").getAsInt(), o.get("solar_factor").getAsDouble(), strOr(o, "water_behavior", "normal"),
                List.copyOf(zones), o);
    }

    private static Map<Identifier, Double> doubleMap(JsonObject o, String key) {
        Map<Identifier, Double> map = new LinkedHashMap<>();
        if (o.has(key) && o.get(key).isJsonObject()) {
            o.getAsJsonObject(key).entrySet().forEach(e -> map.put(Identifier.parse(e.getKey()), e.getValue().getAsDouble()));
        }
        return Map.copyOf(map);
    }

    private static boolean present(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && !e.isJsonNull();
    }

    private static @Nullable String str(JsonObject o, String key) {
        return present(o, key) ? o.get(key).getAsString() : null;
    }

    private static String strOr(JsonObject o, String key, String fallback) {
        String s = str(o, key);
        return s == null ? fallback : s;
    }

    private static @Nullable Identifier ident(JsonObject o, String key) {
        String s = str(o, key);
        return s == null ? null : Identifier.parse(s);
    }

    private static @Nullable Double dbl(JsonObject o, String key) {
        return present(o, key) ? o.get(key).getAsDouble() : null;
    }

    private static @Nullable Integer integer(JsonObject o, String key) {
        return present(o, key) ? o.get(key).getAsInt() : null;
    }
}
