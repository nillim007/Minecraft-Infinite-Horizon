package de.sternenwerft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Hält alle Balance-Werte (Kapitel 25.7). Quelle sind die Dateien
 * {@code data/sternenwerft/sternenwerft/balance/*.json}, die aus {@code spec/data/balance} erzeugt werden.
 * Keine Zahl der Spezifikation steht im Code; Klassen fragen hier nach.
 */
public final class BalanceManager {

    private static volatile BalanceManager current = new BalanceManager(Map.of());

    private final Map<String, JsonObject> files;

    public BalanceManager(Map<String, JsonObject> files) {
        this.files = Map.copyOf(files);
    }

    public static BalanceManager get() {
        return current;
    }

    public static void set(BalanceManager manager) {
        current = manager;
    }

    /** Liest die Datei (z. B. "machines") und gibt deren "entries" zurück. */
    public Optional<JsonObject> entries(String file) {
        JsonObject doc = files.get(file);
        if (doc == null || !doc.has("entries")) {
            return Optional.empty();
        }
        return Optional.of(doc.getAsJsonObject("entries"));
    }

    public Optional<JsonObject> entry(String file, String key) {
        return entries(file).map(e -> e.get(key)).filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    // ------------------------------------------------------------------ typisierte Zugriffe

    public MachineStats machine(String id) {
        JsonObject e = entry("machines", id).orElseThrow(() -> missing("machines", id));
        long energy = e.get("energy_per_tick").getAsLong();
        Integer ticks = e.has("ticks") && !e.get("ticks").isJsonNull() ? e.get("ticks").getAsInt() : null;
        long buffer = e.has("buffer") && !e.get("buffer").isJsonNull()
                ? e.get("buffer").getAsLong()
                : bufferFormula().apply(energy, ticks);
        JsonObject slots = e.has("item_slots") ? e.getAsJsonObject("item_slots") : new JsonObject();
        int input = slots.has("input") ? slots.get("input").getAsInt() : 0;
        int output = slots.has("output") ? slots.get("output").getAsInt() : 0;
        int upgrades = e.has("upgrade_slots") ? e.get("upgrade_slots").getAsInt() : 0;
        return new MachineStats(id, energy, ticks == null ? 0 : ticks, buffer, input, output, upgrades);
    }

    public GeneratorStats generator(String id) {
        JsonObject e = entry("generators", id).orElseThrow(() -> missing("generators", id));
        return new GeneratorStats(id, e.get("output_per_tick").getAsLong(), e.get("buffer").getAsLong());
    }

    public long cableThroughput(String id) {
        return entry("transport", id).orElseThrow(() -> missing("transport", id)).get("throughput").getAsLong();
    }

    public UpgradeStats upgrade(String id) {
        JsonObject e = entry("machines", id).orElseThrow(() -> missing("machines", id));
        double ticks = e.has("ticks_multiplier") ? e.get("ticks_multiplier").getAsDouble() : 1.0;
        double energy = e.has("energy_per_tick_multiplier") ? e.get("energy_per_tick_multiplier").getAsDouble() : 1.0;
        int maxStack = e.has("max_stack") ? e.get("max_stack").getAsInt() : 1;
        return new UpgradeStats(id, ticks, energy, maxStack);
    }

    public BufferFormula bufferFormula() {
        JsonObject f = entry("machines", "energy_buffer_formula").orElseThrow(() -> missing("machines", "energy_buffer_formula"));
        return new BufferFormula(f.get("factor").getAsLong(), f.get("duration_cap_ticks").getAsInt(),
                f.get("default_duration_ticks").getAsInt(), f.get("min").getAsLong(), f.get("max").getAsLong());
    }

    private static IllegalStateException missing(String file, String key) {
        return new IllegalStateException("Balance-Wert fehlt: balance/" + file + ".json → " + key);
    }

    // ------------------------------------------------------------------ Laden

    /** Lädt alle *.json eines Verzeichnisses (für Tests und Werkzeuge). */
    public static BalanceManager loadDirectory(Path dir) throws IOException {
        Map<String, JsonObject> map = new HashMap<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json"))::iterator) {
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    String name = p.getFileName().toString();
                    map.put(name.substring(0, name.length() - ".json".length()), JsonParser.parseReader(r).getAsJsonObject());
                }
            }
        }
        return new BalanceManager(map);
    }

    // ------------------------------------------------------------------ Datentypen

    public record MachineStats(String id, long energyPerTick, int ticks, long buffer, int inputSlots, int outputSlots,
                               int upgradeSlots) {
    }

    public record GeneratorStats(String id, long outputPerTick, long buffer) {
    }

    public record UpgradeStats(String id, double ticksMultiplier, double energyMultiplier, int maxStack) {
    }

    /** Puffer = factor × Verbrauch × min(Dauer, cap), begrenzt auf [min, max] (Kapitel 4.1). */
    public record BufferFormula(long factor, int durationCap, int defaultDuration, long min, long max) {
        public long apply(long energyPerTick, Integer ticks) {
            int duration = ticks == null || ticks <= 0 ? defaultDuration : Math.min(ticks, durationCap);
            long raw = factor * energyPerTick * duration;
            return Math.max(min, Math.min(max, raw));
        }
    }
}
