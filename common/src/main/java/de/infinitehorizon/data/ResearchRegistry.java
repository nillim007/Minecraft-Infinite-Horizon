package de.infinitehorizon.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Forschungs-Registry (11.4, 25.3): Knoten aus {@code data/<ns>/infinite_horizon/research/} und Stufen aus
 * {@code research_tier/}. Reload-Listener wie {@link PlanetRegistry}, keine dynamische Registry von Minecraft.
 * <p>
 * <b>Zusammenführen (Addons):</b> Die Knoten-ID ist der Dateipfad ({@code <ns>:<pfad>}) oder ein Feld {@code "id"} in
 * der Datei. Spätere Dateien (andere Namespaces, alphabetisch nach {@code infinite_horizon}) überschreiben einzelne
 * Felder früherer mit derselben ID; {@code "replace": true} ersetzt den ganzen Eintrag. Stufen werden ebenso über
 * {@code "tier"} zusammengeführt. Der Client bekommt alles über {@code SyncDataPayload}.
 */
public final class ResearchRegistry {

    public static final String NODE_DIRECTORY = InfiniteHorizon.MOD_ID + "/research";
    public static final String TIER_DIRECTORY = InfiniteHorizon.MOD_ID + "/research_tier";

    /** Äste in Spaltenreihenfolge (25.3, {@code position.column}). */
    public static final List<String> BRANCHES = List.of("industry", "spaceflight", "life_support", "logistics");

    /** Eine Freischaltung: {@code item}, {@code probe_target} oder {@code multiblock} (25.3). */
    public record Unlock(String type, Identifier id) {
        public static final String ITEM = "item";
        public static final String PROBE_TARGET = "probe_target";
    }

    /** Ein Knoten (25.3). {@code cost} in ganzen FP; ohne Angabe gilt {@code cost_default} der Stufe. */
    public record Node(Identifier id, int tier, String branch, int cost, List<Identifier> requires, List<Unlock> unlocks,
                       Identifier icon, int column, int row, int slot) {

        public boolean unlocksItem(Identifier item) {
            return unlocks.stream().anyMatch(u -> u.type().equals(Unlock.ITEM) && u.id().equals(item));
        }

        /** Übersetzungsschlüssel des Namens: {@code research.<ns>.<pfad>}. */
        public String nameKey() {
            return "research." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        }
    }

    /** Eine Stufe mit Tor (25.3); das Tor bleibt JSON, {@code ResearchGates} wertet es aus. */
    public record Tier(int tier, int costDefault, JsonObject gate) {

        public String nameKey() {
            return tierNameKey(tier);
        }
    }

    /** Übersetzungsschlüssel des Stufennamens: {@code research.infinite_horizon.tier.<n>}. */
    public static String tierNameKey(int tier) {
        return "research." + InfiniteHorizon.MOD_ID + ".tier." + tier;
    }

    private static final ResearchRegistry EMPTY = new ResearchRegistry(Map.of(), Map.of());
    private static volatile ResearchRegistry server = EMPTY;
    private static volatile ResearchRegistry client = EMPTY;

    private final Map<Identifier, JsonObject> rawNodes;
    private final Map<Integer, JsonObject> rawTiers;
    private final SortedMap<Integer, Tier> tiers = new TreeMap<>();
    private final Map<Identifier, Node> nodes = new LinkedHashMap<>();
    private final Map<Identifier, List<Node>> nodesByItem = new LinkedHashMap<>();

    public ResearchRegistry(Map<Identifier, JsonObject> rawNodes, Map<Integer, JsonObject> rawTiers) {
        this.rawNodes = Map.copyOf(rawNodes);
        this.rawTiers = Map.copyOf(rawTiers);
        rawTiers.forEach((n, o) -> {
            try {
                JsonObject gate = o.has("gate") && o.get("gate").isJsonObject() ? o.getAsJsonObject("gate") : new JsonObject();
                tiers.put(n, new Tier(n, o.has("cost_default") ? o.get("cost_default").getAsInt() : 0, gate));
            } catch (RuntimeException ex) {
                InfiniteHorizon.LOG.error("Forschungsstufe {} fehlerhaft: {}", n, ex.toString());
            }
        });
        List<Node> parsed = new ArrayList<>();
        rawNodes.forEach((id, o) -> {
            try {
                parsed.add(parseNode(id, o));
            } catch (RuntimeException ex) {
                InfiniteHorizon.LOG.error("Forschungsknoten {} fehlerhaft: {}", id, ex.toString());
            }
        });
        // feste Reihenfolge: Stufe, Ast, Slot, ID (Baum, Tooltips, Rezeptsperre)
        parsed.sort(Comparator.comparingInt(Node::row).thenComparingInt(Node::column).thenComparingInt(Node::slot)
                .thenComparing(n -> n.id().toString()));
        parsed.forEach(n -> {
            nodes.put(n.id(), n);
            n.unlocks().stream().filter(u -> u.type().equals(Unlock.ITEM))
                    .forEach(u -> nodesByItem.computeIfAbsent(u.id(), k -> new ArrayList<>()).add(n));
        });
    }

    private Node parseNode(Identifier id, JsonObject o) {
        int tier = o.get("tier").getAsInt();
        String branch = o.has("branch") ? o.get("branch").getAsString() : BRANCHES.getFirst();
        Tier t = tiers.get(tier);
        int cost = o.has("cost") && !o.get("cost").isJsonNull() ? o.get("cost").getAsInt() : t == null ? 0 : t.costDefault();
        List<Identifier> requires = new ArrayList<>();
        if (o.has("requires")) {
            o.getAsJsonArray("requires").forEach(e -> requires.add(Identifier.parse(e.getAsString())));
        }
        List<Unlock> unlocks = new ArrayList<>();
        if (o.has("unlocks")) {
            for (JsonElement e : o.getAsJsonArray("unlocks")) {
                JsonObject u = e.getAsJsonObject();
                unlocks.add(new Unlock(u.get("type").getAsString(), Identifier.parse(u.get("id").getAsString())));
            }
        }
        Identifier icon = o.has("icon") ? Identifier.parse(o.get("icon").getAsString())
                : unlocks.isEmpty() ? Identifier.withDefaultNamespace("book") : unlocks.getFirst().id();
        JsonObject pos = o.has("position") ? o.getAsJsonObject("position") : new JsonObject();
        int column = pos.has("column") ? pos.get("column").getAsInt() : Math.max(0, BRANCHES.indexOf(branch));
        int row = pos.has("row") ? pos.get("row").getAsInt() : tier;
        int slot = pos.has("slot") ? pos.get("slot").getAsInt() : 0;
        return new Node(id, tier, branch, cost, List.copyOf(requires), List.copyOf(unlocks), icon, column, row, slot);
    }

    /** Forschung des Servers (nach Serverstart und {@code /reload}). */
    public static ResearchRegistry get() {
        return server;
    }

    public static void set(ResearchRegistry registry) {
        server = registry;
    }

    /** Vom Server geschickte Forschung (Forschungsbaum, Handbuch, JEI/REI). */
    public static ResearchRegistry client() {
        return client;
    }

    public static void setClient(ResearchRegistry registry) {
        client = registry;
    }

    public SortedMap<Integer, Tier> tiers() {
        return new TreeMap<>(tiers);
    }

    public Optional<Tier> tier(int tier) {
        return Optional.ofNullable(tiers.get(tier));
    }

    public Collection<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    public Optional<Node> node(Identifier id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /** Knoten, die dieses Item freischalten (für die Rezeptsperre, 11.3). */
    public List<Node> nodesUnlocking(Identifier item) {
        return nodesByItem.getOrDefault(item, List.of());
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && tiers.isEmpty();
    }

    // ---------------------------------------------------------------- Zusammenführen und Sync

    /** Führt eine Knoten-Datei ein ({@code fileId} aus dem Pfad; ein Feld {@code "id"} hat Vorrang). */
    public static void mergeNode(Map<Identifier, JsonObject> into, Identifier fileId, JsonObject entry) {
        Identifier id = entry.has("id") ? Identifier.parse(entry.get("id").getAsString()) : fileId;
        merge(into, id, entry);
    }

    /** Führt eine Stufen-Datei über {@code "tier"} ein. */
    public static void mergeTier(Map<Integer, JsonObject> into, JsonObject entry) {
        merge(into, entry.get("tier").getAsInt(), entry);
    }

    private static <K> void merge(Map<K, JsonObject> into, K key, JsonObject entry) {
        JsonObject existing = into.get(key);
        if (existing == null || (entry.has("replace") && entry.get("replace").getAsBoolean())) {
            JsonObject copy = entry.deepCopy();
            copy.remove("replace");
            into.put(key, copy);
            return;
        }
        JsonObject merged = existing.deepCopy();
        entry.entrySet().forEach(e -> {
            if (!e.getKey().equals("replace")) {
                merged.add(e.getKey(), e.getValue().deepCopy());
            }
        });
        into.put(key, merged);
    }

    /** Alles als JSON (Sync an den Client): {@code {"nodes": {id: …}, "tiers": [ … ]}}. */
    public String toJson() {
        JsonObject root = new JsonObject();
        JsonObject n = new JsonObject();
        rawNodes.forEach((id, o) -> n.add(id.toString(), o));
        JsonArray t = new JsonArray();
        new TreeMap<>(rawTiers).values().forEach(t::add);
        root.add("nodes", n);
        root.add("tiers", t);
        return root.toString();
    }

    public static ResearchRegistry fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return EMPTY;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<Identifier, JsonObject> nodes = new LinkedHashMap<>();
        if (root.has("nodes")) {
            root.getAsJsonObject("nodes").entrySet().forEach(e ->
                    nodes.put(Identifier.parse(e.getKey()), e.getValue().getAsJsonObject()));
        }
        Map<Integer, JsonObject> tiers = new LinkedHashMap<>();
        if (root.has("tiers")) {
            root.getAsJsonArray("tiers").forEach(e -> mergeTier(tiers, e.getAsJsonObject()));
        }
        return new ResearchRegistry(nodes, tiers);
    }
}
