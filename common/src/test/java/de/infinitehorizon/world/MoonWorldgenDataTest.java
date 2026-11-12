package de.infinitehorizon.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3c (27.5): Biom-Parameter und Erz-Features des Mondes (erzeugt von tools/gen_resources.py) enthalten die Werte aus
 * planets.json und folgen den Regeln aus 13.1.
 */
class MoonWorldgenDataTest {

    private static final String NS = "infinite_horizon";
    private static JsonObject moon;
    private static Path data;

    @BeforeAll
    static void load() throws IOException {
        Path spec = Path.of(System.getProperty("infinite_horizon.specData"));
        JsonObject planets = JsonParser.parseString(Files.readString(spec.resolve("planets.json"))).getAsJsonObject();
        for (JsonElement e : planets.getAsJsonArray("entries")) {
            if (e.getAsJsonObject().get("id").getAsString().equals(NS + ":moon")) {
                moon = e.getAsJsonObject();
            }
        }
        data = spec.getParent().getParent().resolve("common/src/generated/resources/data");
    }

    private static JsonObject json(String rel) throws IOException {
        return JsonParser.parseString(Files.readString(data.resolve(rel))).getAsJsonObject();
    }

    private static double[] range(JsonObject params, String axis) {
        JsonArray a = params.getAsJsonArray(axis);
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()};
    }

    @Test
    void biomeParametersFollowShapes() throws IOException {
        JsonArray biomes = json(NS + "/dimension/moon.json").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonArray("biomes");
        Map<String, JsonObject> byId = new HashMap<>();
        biomes.forEach(b -> byId.put(b.getAsJsonObject().get("biome").getAsString(), b.getAsJsonObject().getAsJsonObject("parameters")));
        JsonArray spec = moon.getAsJsonArray("biomes");
        assertEquals(spec.size(), byId.size());
        Map<String, double[]> erosion = Map.of("mountainous", new double[]{-1.0, -0.2}, "flat", new double[]{0.45, 1.0},
                "hilly", new double[]{-0.2, 0.45});
        for (JsonElement e : spec) {
            JsonObject b = e.getAsJsonObject();
            JsonObject prm = byId.get(b.get("id").getAsString());
            assertTrue(prm != null, "Biom fehlt: " + b.get("id"));
            if ("rare".equals(b.get("rarity").getAsString())) {
                // erstes seltenes Biom über Temperatur < −0,8 (13.1)
                double[] t = range(prm, "temperature");
                assertEquals(-1.0, t[0], 1e-9);
                assertEquals(-0.8, t[1], 1e-9);
            } else {
                double[] want = erosion.get(b.get("shape").getAsString());
                double[] got = range(prm, "erosion");
                assertEquals(want[0], got[0], 1e-9, b.get("id").toString());
                assertEquals(want[1], got[1], 1e-9, b.get("id").toString());
                assertEquals(-0.8, range(prm, "temperature")[0], 1e-9, "Rest der Temperaturachse");
            }
        }
    }

    @Test
    void oreFeaturesMatchPlanetsJson() throws IOException {
        JsonArray ores = moon.getAsJsonArray("ores");
        String host = moon.get("default_block").getAsString();
        for (int i = 0; i < ores.size(); i++) {
            JsonObject ore = ores.get(i).getAsJsonObject();
            String block = ore.get("block").getAsString();
            String name = "moon_ore_" + i + "_" + block.split(":", 2)[1];
            JsonObject feature = json(NS + "/worldgen/feature/" + name + ".json");
            assertEquals(ore.get("vein_size").getAsInt(), feature.get("size").getAsInt(), name);
            JsonObject target = feature.getAsJsonArray("targets").get(0).getAsJsonObject();
            assertEquals(block, target.get("state").getAsString());
            assertEquals(host, target.getAsJsonObject("target").get("block").getAsString());

            JsonArray placement = json(NS + "/worldgen/placed_feature/" + name + ".json").getAsJsonArray("placement");
            JsonObject count = placement.get(0).getAsJsonObject();
            assertEquals(ore.get("per_chunk").getAsInt(), count.get("count").getAsInt(), name);
            JsonObject height = placement.get(2).getAsJsonObject().getAsJsonObject("height");
            assertEquals(ore.get("y_min").getAsInt(), height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
            assertEquals(ore.get("y_max").getAsInt(), height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());

            for (JsonElement b : ore.getAsJsonArray("biomes")) {
                JsonArray steps = json(NS + "/worldgen/biome/" + b.getAsString().split(":", 2)[1] + ".json").getAsJsonArray("features");
                List<String> oresStep = new ArrayList<>();
                steps.get(6).getAsJsonArray().forEach(f -> oresStep.add(f.getAsString()));
                assertTrue(oresStep.contains(NS + ":" + name), name + " fehlt im Biom " + b);
            }
        }
    }

    @Test
    void noiseSettingsUsePlanetBlocks() throws IOException {
        JsonObject ns = json(NS + "/worldgen/noise_settings/moon.json");
        assertEquals(moon.get("default_block").getAsString(), ns.get("default_block").getAsString());
        assertEquals(moon.get("sea_level").getAsInt(), ns.get("sea_level").getAsInt());
        assertFalse(ns.has("aquifers"), "keine Aquifere (13.1)");
    }
}
