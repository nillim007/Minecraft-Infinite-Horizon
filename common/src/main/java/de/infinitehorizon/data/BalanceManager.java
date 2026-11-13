package de.infinitehorizon.data;

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
 * {@code data/infinite_horizon/infinite_horizon/balance/*.json}, die aus {@code spec/data/balance} erzeugt werden.
 * Keine Zahl der Spezifikation steht im Code; Klassen fragen hier nach.
 */
public final class BalanceManager {

    private static volatile BalanceManager current = new BalanceManager(Map.of());

    private final Map<String, JsonObject> files;
    /**
     * Zwischenspeicher der typisierten Zugriffe: Maschinen fragen ihre Werte jeden Tick ab, das JSON ändert sich aber
     * nur mit einem neuen BalanceManager (/reload). Leistungstest 27.2.
     */
    private final Map<String, Object> memo = new java.util.concurrent.ConcurrentHashMap<>();

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

    @SuppressWarnings("unchecked")
    private <T> T memo(String key, java.util.function.Supplier<T> compute) {
        Object v = memo.get(key);
        if (v == null) {
            v = compute.get();
            memo.put(key, v);
        }
        return (T) v;
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    // ------------------------------------------------------------------ typisierte Zugriffe

    public MachineStats machine(String id) {
        return memo("machine:" + id, () -> computeMachine(id));
    }

    private MachineStats computeMachine(String id) {
        JsonObject e = entry("machines", id).orElseThrow(() -> missing("machines", id));
        long energy = e.get("energy_per_tick").getAsLong();
        Integer ticks = e.has("ticks") && !e.get("ticks").isJsonNull() ? e.get("ticks").getAsInt() : null;
        long buffer = e.has("buffer") && !e.get("buffer").isJsonNull()
                ? e.get("buffer").getAsLong()
                : bufferFormula().apply(energy, ticks);
        JsonObject slots = e.has("item_slots") ? e.getAsJsonObject("item_slots") : new JsonObject();
        int input = slots.has("input") ? slots.get("input").getAsInt() : 0;
        int output = slots.has("output") ? slots.get("output").getAsInt() : 0;
        int mold = slots.has("mold") ? slots.get("mold").getAsInt() : 0;
        int upgrades = e.has("upgrade_slots") ? e.get("upgrade_slots").getAsInt() : 0;
        return new MachineStats(id, energy, ticks == null ? 0 : ticks, buffer, input, output, upgrades, mold);
    }

    /**
     * Tanks eines Blocks aus {@code fluid_tanks} ({@code role} input/output, {@code capacity}, {@code fluids} = erlaubte
     * Fluide oder null). Leer, wenn der Eintrag keine Tanks hat.
     */
    public java.util.List<TankSpec> tanks(String file, String id) {
        return memo("tanks:" + file + ":" + id, () -> java.util.List.copyOf(computeTanks(file, id)));
    }

    private java.util.List<TankSpec> computeTanks(String file, String id) {
        java.util.List<TankSpec> out = new java.util.ArrayList<>();
        entry(file, id).filter(e -> e.has("fluid_tanks") && e.get("fluid_tanks").isJsonArray()).ifPresent(e -> {
            for (JsonElement t : e.getAsJsonArray("fluid_tanks")) {
                JsonObject o = t.getAsJsonObject();
                java.util.List<String> fluids = null;
                if (o.has("fluids") && o.get("fluids").isJsonArray()) {
                    fluids = new java.util.ArrayList<>();
                    for (JsonElement f : o.getAsJsonArray("fluids")) {
                        fluids.add(f.getAsString());
                    }
                }
                out.add(new TankSpec("output".equals(o.has("role") ? o.get("role").getAsString() : "input"),
                        o.get("capacity").getAsLong(), fluids));
            }
        });
        return out;
    }

    /** Füll- und Leerslots aus {@code container_slots} ({@code fill}, {@code drain}); {0, 0} ohne Eintrag. */
    public int[] containerSlots(String file, String id) {
        return memo("containers:" + file + ":" + id, () -> computeContainerSlots(file, id)).clone();
    }

    private int[] computeContainerSlots(String file, String id) {
        JsonObject e = entry(file, id).orElse(new JsonObject());
        JsonObject s = e.has("container_slots") && e.get("container_slots").isJsonObject()
                ? e.getAsJsonObject("container_slots") : new JsonObject();
        return new int[]{s.has("fill") ? s.get("fill").getAsInt() : 0, s.has("drain") ? s.get("drain").getAsInt() : 0};
    }

    /** Upgrade-Slot (1, 2, …), in den dieses Item gehört; 0 = kein Upgrade (machines.json → Feld {@code slot}). */
    public int upgradeSlotOf(String itemId) {
        return memo("upgradeSlot:" + itemId, () -> computeUpgradeSlotOf(itemId));
    }

    private int computeUpgradeSlotOf(String itemId) {
        return entry("machines", itemId).filter(o -> o.has("slot") && o.has("max_stack"))
                .map(o -> o.get("slot").getAsInt()).orElse(0);
    }

    public GeneratorStats generator(String id) {
        return memo("generator:" + id, () -> computeGenerator(id));
    }

    private GeneratorStats computeGenerator(String id) {
        JsonObject e = entry("generators", id).orElseThrow(() -> missing("generators", id));
        JsonObject slots = e.has("item_slots") && e.get("item_slots").isJsonObject() ? e.getAsJsonObject("item_slots") : new JsonObject();
        int fuel = slots.has("fuel") ? slots.get("fuel").getAsInt() : 0;
        return new GeneratorStats(id, e.get("output_per_tick").getAsLong(), e.get("buffer").getAsLong(), fuel);
    }

    /**
     * Kopie mit einem geänderten Feld {@code file → key → field} (z. B. für GameTests oder Werkzeuge).
     * Der Wert wird wie aus JSON gelesen behandelt.
     */
    public BalanceManager withValue(String file, String key, String field, JsonElement value) {
        Map<String, JsonObject> copy = new HashMap<>();
        files.forEach((k, v) -> copy.put(k, v.deepCopy()));
        JsonObject doc = copy.computeIfAbsent(file, k -> new JsonObject());
        if (!doc.has("entries")) {
            doc.add("entries", new JsonObject());
        }
        JsonObject entries = doc.getAsJsonObject("entries");
        if (!entries.has(key)) {
            entries.add(key, new JsonObject());
        }
        entries.getAsJsonObject(key).add(field, value);
        return new BalanceManager(copy);
    }

    /**
     * Führt Balance-Dateien zusammen: spätere Dokumente ergänzen bzw. überschreiben Einträge früherer
     * (gleicher Dateiname, Eintrag für Eintrag). Mit {@code "replace": true} ersetzt ein Dokument die Datei ganz.
     */
    public static Map<String, JsonObject> merge(Map<String, JsonObject> into, String file, JsonObject doc) {
        JsonObject existing = into.get(file);
        boolean replace = doc.has("replace") && doc.get("replace").getAsBoolean();
        if (existing == null || replace) {
            into.put(file, doc.deepCopy());
            return into;
        }
        JsonObject merged = existing.deepCopy();
        if (doc.has("entries") && doc.get("entries").isJsonObject()) {
            if (!merged.has("entries")) {
                merged.add("entries", new JsonObject());
            }
            JsonObject target = merged.getAsJsonObject("entries");
            doc.getAsJsonObject("entries").entrySet().forEach(en -> target.add(en.getKey(), en.getValue().deepCopy()));
        }
        into.put(file, merged);
        return into;
    }

    public long cableThroughput(String id) {
        return entry("transport", id).orElseThrow(() -> missing("transport", id)).get("throughput").getAsLong();
    }

    public UpgradeStats upgrade(String id) {
        return memo("upgrade:" + id, () -> computeUpgrade(id));
    }

    private UpgradeStats computeUpgrade(String id) {
        JsonObject e = entry("machines", id).orElseThrow(() -> missing("machines", id));
        double ticks = e.has("ticks_multiplier") ? e.get("ticks_multiplier").getAsDouble() : 1.0;
        double energy = e.has("energy_per_tick_multiplier") ? e.get("energy_per_tick_multiplier").getAsDouble() : 1.0;
        int maxStack = e.has("max_stack") ? e.get("max_stack").getAsInt() : 1;
        return new UpgradeStats(id, ticks, energy, maxStack);
    }

    public BufferFormula bufferFormula() {
        return memo("bufferFormula", () -> computeBufferFormula());
    }

    private BufferFormula computeBufferFormula() {
        JsonObject f = entry("machines", "energy_buffer_formula").orElseThrow(() -> missing("machines", "energy_buffer_formula"));
        return new BufferFormula(f.get("factor").getAsLong(), f.get("duration_cap_ticks").getAsInt(),
                f.get("default_duration_ticks").getAsInt(), f.get("min").getAsLong(), f.get("max").getAsLong());
    }

    private static IllegalStateException missing(String file, String key) {
        return new IllegalStateException("Balance-Wert fehlt: balance/" + file + ".json → " + key);
    }

    // ------------------------------------------------------------------ Laden

    private static volatile BalanceManager bundled;

    /**
     * Die Werte aus dem Mod-Jar (ohne Datapacks). Für den Client, der keine Balance vom Server bekommt, z. B. für die
     * Geisterprojektion (4.1). Auf dem Server immer {@link #get()} verwenden.
     */
    public static BalanceManager bundled() {
        BalanceManager b = bundled;
        if (b == null) {
            Map<String, JsonObject> map = new HashMap<>();
            for (String name : new String[]{"machines", "misc", "storage", "generators", "transport", "suits", "environment", "items", "spaceflight", "vehicles", "creatures", "research", "agriculture"}) {
                String path = "/data/infinite_horizon/infinite_horizon/balance/" + name + ".json";
                try (java.io.InputStream in = BalanceManager.class.getResourceAsStream(path)) {
                    if (in != null) {
                        try (Reader r = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
                            map.put(name, JsonParser.parseReader(r).getAsJsonObject());
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    // fehlt die Datei, bleibt der Eintrag leer
                }
            }
            b = new BalanceManager(map);
            bundled = b;
        }
        return b;
    }

    /** {@link #get()}, falls geladen (Server, Einzelspieler), sonst {@link #bundled()}. */
    public static BalanceManager getOrBundled() {
        BalanceManager b = current;
        return b.isEmpty() ? bundled() : b;
    }

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
                               int upgradeSlots, int moldSlots) {

        public MachineStats withProcess(long energyPerTick, int ticks) {
            return new MachineStats(id, energyPerTick, ticks, buffer, inputSlots, outputSlots, upgradeSlots, moldSlots);
        }
    }

    public record GeneratorStats(String id, long outputPerTick, long buffer, int fuelSlots) {
    }

    /** Ein Tank laut Balance: Ausgangstank ({@code output}) oder Eingangstank; {@code fluids} null = laut Rezepten. */
    public record TankSpec(boolean output, long capacity, java.util.@org.jspecify.annotations.Nullable List<String> fluids) {
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
