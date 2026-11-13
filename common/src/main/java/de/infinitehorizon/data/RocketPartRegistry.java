package de.infinitehorizon.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Raketenteile (12.4, 25.5) aus {@code data/<ns>/infinite_horizon/rocket_part/} und Regeln je Stufe aus
 * {@code rocket_rules/} (SPEC-GAP #72). Reload-Listener wie {@link PlanetRegistry}: Einträge aller Namespaces werden über
 * {@code block} bzw. {@code tier} zusammengeführt, spätere Felder überschreiben. Der Client bekommt alles über
 * {@code SyncDataPayload}.
 */
public final class RocketPartRegistry {

    public static final String PART_DIRECTORY = InfiniteHorizon.MOD_ID + "/rocket_part";
    public static final String RULE_DIRECTORY = InfiniteHorizon.MOD_ID + "/rocket_rules";

    /** Rolle eines Teils (25.5). */
    public enum Role {
        CAPSULE, TANK, ENGINE, FIN, CARGO, LONG_RANGE;

        public static Role byName(String name) {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Ein Teil (25.5). {@code fuelCapacity} je Komponente. */
    public record Part(Identifier block, Role role, int tier, List<Integer> usableInTiers, double mass, int fuelCapacity,
                       double fuelMassFull, int thrust, int cargoSlots, double massPerSlot, int seats) {

        static Part fromJson(JsonObject o) {
            List<Integer> usable = new ArrayList<>();
            if (o.has("usable_in_tiers")) {
                o.getAsJsonArray("usable_in_tiers").forEach(e -> usable.add(e.getAsInt()));
            } else {
                usable.add(o.get("tier").getAsInt());
            }
            return new Part(Identifier.parse(o.get("block").getAsString()), Role.byName(o.get("role").getAsString()),
                    o.get("tier").getAsInt(), List.copyOf(usable), dbl(o, "mass"), integer(o, "fuel_capacity"),
                    dbl(o, "fuel_mass_full"), integer(o, "thrust"), integer(o, "cargo_slots"), dbl(o, "mass_per_slot"),
                    integer(o, "seats"));
        }
    }

    /** Regeln einer Stufe (rocket_parts.json → rules). Bereiche als {@code [min, max]}. */
    public record Rule(int tier, int thrust, List<Identifier> fuel, int[] capsules, int[] engines, int[] tanks, int fins,
                       int[] cargoBays, int[] longRange, int seats, List<Role> stackOrder) {

        static Rule fromJson(JsonObject o) {
            List<Identifier> fuel = new ArrayList<>();
            o.getAsJsonArray("fuel").forEach(e -> fuel.add(Identifier.parse(e.getAsString())));
            List<Role> order = new ArrayList<>();
            if (o.has("stack_order")) {
                o.getAsJsonArray("stack_order").forEach(e -> order.add(Role.byName(e.getAsString())));
            }
            return new Rule(o.get("tier").getAsInt(), integer(o, "thrust"), List.copyOf(fuel), range(o, "capsules"),
                    range(o, "engines"), range(o, "tanks"), integer(o, "fins"), range(o, "cargo_bays"),
                    range(o, "long_range"), integer(o, "seats"), List.copyOf(order));
        }

        public int[] range(Role role) {
            return switch (role) {
                case CAPSULE -> capsules;
                case ENGINE -> engines;
                case TANK -> tanks;
                case CARGO -> cargoBays;
                case LONG_RANGE -> longRange;
                case FIN -> new int[]{fins, fins};
            };
        }

        private static int[] range(JsonObject o, String key) {
            if (!o.has(key)) {
                return new int[]{0, 0};
            }
            JsonArray a = o.getAsJsonArray(key);
            return new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()};
        }
    }

    private static final RocketPartRegistry EMPTY = new RocketPartRegistry(Map.of(), Map.of());
    private static volatile RocketPartRegistry server = EMPTY;
    private static volatile RocketPartRegistry client = EMPTY;

    private final Map<Identifier, JsonObject> rawParts;
    private final Map<Integer, JsonObject> rawRules;
    private final Map<Identifier, Part> parts = new LinkedHashMap<>();
    private final Map<Integer, Rule> rules = new TreeMap<>();

    public RocketPartRegistry(Map<Identifier, JsonObject> rawParts, Map<Integer, JsonObject> rawRules) {
        this.rawParts = Map.copyOf(rawParts);
        this.rawRules = Map.copyOf(rawRules);
        rawParts.forEach((id, o) -> {
            try {
                parts.put(id, Part.fromJson(o));
            } catch (RuntimeException ex) {
                InfiniteHorizon.LOG.error("Raketenteil {} fehlerhaft: {}", id, ex.toString());
            }
        });
        rawRules.forEach((t, o) -> {
            try {
                rules.put(t, Rule.fromJson(o));
            } catch (RuntimeException ex) {
                InfiniteHorizon.LOG.error("Raketenregel Stufe {} fehlerhaft: {}", t, ex.toString());
            }
        });
    }

    public static RocketPartRegistry get() {
        return server;
    }

    public static void set(RocketPartRegistry registry) {
        server = registry;
    }

    public static RocketPartRegistry client() {
        return client;
    }

    public static void setClient(RocketPartRegistry registry) {
        client = registry;
    }

    public Optional<Part> part(Identifier block) {
        return Optional.ofNullable(parts.get(block));
    }

    public Collection<Part> parts() {
        return List.copyOf(parts.values());
    }

    public Optional<Rule> rule(int tier) {
        return Optional.ofNullable(rules.get(tier));
    }

    public Collection<Rule> rules() {
        return List.copyOf(rules.values());
    }

    // ---------------------------------------------------------------- Zusammenführen und Sync

    public static void mergePart(Map<Identifier, JsonObject> into, JsonObject entry) {
        merge(into, Identifier.parse(entry.get("block").getAsString()), entry);
    }

    public static void mergeRule(Map<Integer, JsonObject> into, JsonObject entry) {
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

    public String toJson() {
        JsonObject root = new JsonObject();
        JsonArray p = new JsonArray();
        rawParts.values().forEach(p::add);
        JsonArray r = new JsonArray();
        new TreeMap<>(rawRules).values().forEach(r::add);
        root.add("parts", p);
        root.add("rules", r);
        return root.toString();
    }

    public static RocketPartRegistry fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return EMPTY;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<Identifier, JsonObject> p = new LinkedHashMap<>();
        Map<Integer, JsonObject> r = new LinkedHashMap<>();
        if (root.has("parts")) {
            for (JsonElement e : root.getAsJsonArray("parts")) {
                mergePart(p, e.getAsJsonObject());
            }
        }
        if (root.has("rules")) {
            for (JsonElement e : root.getAsJsonArray("rules")) {
                mergeRule(r, e.getAsJsonObject());
            }
        }
        return new RocketPartRegistry(p, r);
    }

    private static double dbl(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsDouble() : 0;
    }

    private static int integer(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
    }
}
