package de.infinitehorizon.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Planeten-Registry (13.1): Reload-Listener wie {@link BalanceManager}, keine dynamische Registry von Minecraft.
 * Einträge aus allen Namespaces werden je {@code dimension} zusammengeführt: Felder späterer Dateien (andere
 * Namespaces, alphabetisch nach {@code infinite_horizon}) überschreiben einzelne Felder früherer; {@code "replace": true}
 * ersetzt den ganzen Eintrag. Der Client bekommt die Einträge über {@code SyncDataPayload}.
 */
public final class PlanetRegistry {

    public static final String DIRECTORY = InfiniteHorizon.MOD_ID + "/planet";

    private static volatile PlanetRegistry server = new PlanetRegistry(Map.of());
    private static volatile PlanetRegistry client = new PlanetRegistry(Map.of());

    private final Map<Identifier, JsonObject> raw;
    private final Map<Identifier, Planet> planets;

    public PlanetRegistry(Map<Identifier, JsonObject> raw) {
        this.raw = Map.copyOf(raw);
        Map<Identifier, Planet> parsed = new LinkedHashMap<>();
        raw.forEach((id, o) -> {
            try {
                parsed.put(id, Planet.fromJson(o));
            } catch (RuntimeException ex) {
                InfiniteHorizon.LOG.error("Planet {} fehlerhaft: {}", id, ex.toString());
            }
        });
        this.planets = Map.copyOf(parsed);
    }

    /** Planeten des Servers (nach Serverstart und {@code /reload}). */
    public static PlanetRegistry get() {
        return server;
    }

    public static void set(PlanetRegistry registry) {
        server = registry;
    }

    /** Planeten, die der Server geschickt hat (Himmel, HUD). */
    public static PlanetRegistry client() {
        return client;
    }

    public static void setClient(PlanetRegistry registry) {
        client = registry;
    }

    public Optional<Planet> planet(Identifier dimension) {
        return Optional.ofNullable(planets.get(dimension));
    }

    public Collection<Planet> all() {
        return planets.values();
    }

    public boolean isEmpty() {
        return planets.isEmpty();
    }

    /** Führt eine Datei in {@code into} ein (Reihenfolge der Aufrufe = Vorrang, spätere gewinnen). */
    public static void merge(Map<Identifier, JsonObject> into, JsonObject entry) {
        Identifier dim = Identifier.parse(entry.get("dimension").getAsString());
        JsonObject existing = into.get(dim);
        if (existing == null || (entry.has("replace") && entry.get("replace").getAsBoolean())) {
            JsonObject copy = entry.deepCopy();
            copy.remove("replace");
            into.put(dim, copy);
            return;
        }
        JsonObject merged = existing.deepCopy();
        entry.entrySet().forEach(e -> merged.add(e.getKey(), e.getValue().deepCopy()));
        into.put(dim, merged);
    }

    /** Alle Einträge als JSON-Liste (Sync an den Client). */
    public String toJson() {
        JsonArray array = new JsonArray();
        raw.values().forEach(array::add);
        return array.toString();
    }

    public static PlanetRegistry fromJson(String json) {
        Map<Identifier, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
            merge(map, e.getAsJsonObject());
        }
        return new PlanetRegistry(map);
    }
}
