package de.infinitehorizon.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.data.ResearchRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M4a (11.4, 25.3): Stufen-Tore mit den Stufen aus spec/data/research.json. */
class ResearchGatesTest {

    private static final Identifier TERMINAL = Identifier.parse("infinite_horizon:research_terminal");
    private static final Identifier MOON = Identifier.parse("infinite_horizon:moon");
    private static final Identifier OVERWORLD = Identifier.parse("minecraft:overworld");

    private static ResearchRegistry registry;

    @BeforeAll
    static void load() throws Exception {
        Path file = Path.of(System.getProperty("infinite_horizon.specData")).resolve("research.json");
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        Map<Integer, JsonObject> tiers = new LinkedHashMap<>();
        root.getAsJsonArray("tiers").forEach(e -> ResearchRegistry.mergeTier(tiers, e.getAsJsonObject()));
        Map<Identifier, JsonObject> nodes = new LinkedHashMap<>();
        root.getAsJsonArray("nodes").forEach(e -> {
            JsonObject o = e.getAsJsonObject();
            ResearchRegistry.mergeNode(nodes, Identifier.parse(o.get("id").getAsString()), o);
        });
        registry = new ResearchRegistry(nodes, tiers);
    }

    @Test
    void sixTiersFromSpecData() {
        assertEquals(6, registry.tiers().size());
        assertEquals(10, registry.tier(1).orElseThrow().costDefault());
        assertEquals(52, registry.nodes().size());
    }

    @Test
    void tier1OpensWithResearchTerminal() {
        assertEquals(Set.of(), ResearchGates.openTiers(registry.tiers().values(), Set.of(),
                ResearchGates.Facts.of(Set.of(), Set.of(OVERWORLD))));
        assertEquals(Set.of(1), ResearchGates.openTiers(registry.tiers().values(), Set.of(),
                ResearchGates.Facts.of(Set.of(TERMINAL), Set.of(OVERWORLD))));
    }

    @Test
    void tier2OnlyWithTier1AndMoonVisited() {
        // Mond besucht, aber Stufe 1 zu (kein Terminal): Stufe 2 bleibt zu
        assertEquals(Set.of(), ResearchGates.openTiers(registry.tiers().values(), Set.of(),
                ResearchGates.Facts.of(Set.of(), Set.of(OVERWORLD, MOON))));
        // Stufe 1 offen, Mond nicht besucht
        assertEquals(Set.of(1), ResearchGates.openTiers(registry.tiers().values(), Set.of(1),
                ResearchGates.Facts.of(Set.of(), Set.of(OVERWORLD))));
        // Stufe 1 offen und Mond besucht
        assertEquals(Set.of(1, 2), ResearchGates.openTiers(registry.tiers().values(), Set.of(1),
                ResearchGates.Facts.of(Set.of(), Set.of(OVERWORLD, MOON))));
        // beides auf einmal (Terminal gesetzt nach Mondbesuch per goto)
        assertEquals(Set.of(1, 2), ResearchGates.openTiers(registry.tiers().values(), Set.of(),
                ResearchGates.Facts.of(Set.of(TERMINAL), Set.of(MOON))));
    }

    @Test
    void tier3StaysClosedInM4() {
        assertFalse(ResearchGates.openTiers(registry.tiers().values(), Set.of(1, 2),
                ResearchGates.Facts.of(Set.of(TERMINAL), Set.of(MOON))).contains(3));
    }

    @Test
    void openTiersNeverClose() {
        assertEquals(Set.of(1, 2), ResearchGates.openTiers(registry.tiers().values(), Set.of(1, 2),
                ResearchGates.Facts.of(Set.of(), Set.of())));
    }

    @Test
    void allGateTypes() {
        JsonObject crafted = JsonParser.parseString(
                "{\"type\":\"crafted_items\",\"items\":[\"a:x\"],\"fluids\":[\"a:f\"]}").getAsJsonObject();
        JsonObject machine = JsonParser.parseString(
                "{\"type\":\"machine_completed\",\"block\":\"a:m\",\"recipe\":null}").getAsJsonObject();
        JsonObject machineRecipe = JsonParser.parseString(
                "{\"type\":\"machine_completed\",\"block\":\"a:m\",\"recipe\":\"a:r\"}").getAsJsonObject();
        JsonObject all = new JsonObject();
        all.addProperty("type", "all");
        com.google.gson.JsonArray gates = new com.google.gson.JsonArray();
        gates.add(crafted);
        gates.add(machine);
        all.add("gates", gates);

        ResearchGates.Facts none = ResearchGates.Facts.of(Set.of(), Set.of());
        ResearchGates.Facts some = new ResearchGates.Facts(Set.of(), Set.of(), Set.of(Identifier.parse("a:x")),
                Set.of(), Set.of("a:m"));
        ResearchGates.Facts full = new ResearchGates.Facts(Set.of(), Set.of(), Set.of(Identifier.parse("a:x")),
                Set.of(Identifier.parse("a:f")), Set.of("a:m", "a:m|a:r"));

        assertFalse(ResearchGates.isMet(crafted, some));
        assertTrue(ResearchGates.isMet(crafted, full));
        assertFalse(ResearchGates.isMet(machine, none));
        assertTrue(ResearchGates.isMet(machine, some));
        assertFalse(ResearchGates.isMet(machineRecipe, some));
        assertTrue(ResearchGates.isMet(machineRecipe, full));
        assertFalse(ResearchGates.isMet(all, some));
        assertTrue(ResearchGates.isMet(all, full));
        assertTrue(ResearchGates.isMet(new JsonObject(), none));
        assertFalse(ResearchGates.isMet(JsonParser.parseString("{\"type\":\"unknown\"}").getAsJsonObject(), full));
    }

    @Test
    void recipeLockLookupFindsSuitNode() {
        assertEquals(Identifier.parse("infinite_horizon:suit_t1"),
                registry.nodesUnlocking(Identifier.parse("infinite_horizon:pressure_suit_helmet")).getFirst().id());
        assertTrue(registry.nodesUnlocking(Identifier.parse("infinite_horizon:circuit")).isEmpty());
    }
}
