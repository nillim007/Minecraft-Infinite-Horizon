package de.infinitehorizon.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M4b (11.1, 27.7): FP-Buchhaltung mit den Werten aus spec/data/balance/research.json. */
class FpLedgerTest {

    private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier BAUXITE = Identifier.fromNamespaceAndPath("infinite_horizon", "bauxite_ore");
    private static final Identifier LAVA_TUBE = Identifier.fromNamespaceAndPath("infinite_horizon", "lava_tube");

    private static FpLedger.Rules rules;
    private static PlanetRegistry planets;

    @BeforeAll
    static void load() throws Exception {
        Path data = Path.of(System.getProperty("infinite_horizon.specData"));
        rules = FpLedger.Rules.from(BalanceManager.loadDirectory(data.resolve("balance")));
        JsonObject root = JsonParser.parseString(Files.readString(data.resolve("planets.json"))).getAsJsonObject();
        Map<Identifier, JsonObject> entries = new LinkedHashMap<>();
        root.getAsJsonArray("entries").forEach(e -> {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            if (id.equals(OVERWORLD.toString()) || id.equals(MOON.toString())) {
                PlanetRegistry.merge(entries, o);
            }
        });
        planets = new PlanetRegistry(entries);
    }

    private static int ring(Identifier dimension) {
        return FpLedger.ringOf(dimension, planets, rules).orElseThrow();
    }

    @Test
    void tenRing0SamplesGive1000ThenTenth() {
        AgencyProgress p = new AgencyProgress();
        String key = FpLedger.sampleKey(BAUXITE, OVERWORLD, rules);
        for (int i = 0; i < 10; i++) {
            FpLedger.creditSample(p, key, ring(OVERWORLD), rules);
        }
        assertEquals(1000, p.fp());
        assertEquals(10, FpLedger.creditSample(p, key, ring(OVERWORLD), rules));
        assertEquals(1010, p.fp());
    }

    @Test
    void moonSampleGives500() {
        AgencyProgress p = new AgencyProgress();
        assertEquals(1, ring(MOON));
        assertEquals(500, FpLedger.creditSample(p, FpLedger.sampleKey(
                Identifier.fromNamespaceAndPath("infinite_horizon", "moon_regolith"), MOON, rules), ring(MOON), rules));
        assertEquals(500, p.fp());
    }

    @Test
    void firstLavaTubeGives2000ThenDiminishes() {
        AgencyProgress p = new AgencyProgress();
        assertEquals(2000, FpLedger.creditStructure(p, LAVA_TUBE, rules));
        for (int i = 1; i < 10; i++) {
            FpLedger.creditStructure(p, LAVA_TUBE, rules);
        }
        assertEquals(200, FpLedger.creditStructure(p, LAVA_TUBE, rules));
    }

    @Test
    void deepslateCountsAsSameType() {
        Identifier deep = Identifier.withDefaultNamespace("deepslate_iron_ore");
        Identifier normal = Identifier.withDefaultNamespace("iron_ore");
        assertEquals(FpLedger.sampleKey(normal, OVERWORLD, rules), FpLedger.sampleKey(deep, OVERWORLD, rules));
        assertFalse(FpLedger.sampleKey(normal, OVERWORLD, rules).equals(FpLedger.sampleKey(normal, MOON, rules)));
    }

    @Test
    void netherAndUnknownDimensionsGiveNothing() {
        assertEquals(OptionalInt.empty(), FpLedger.ringOf(Identifier.withDefaultNamespace("the_nether"), planets, rules));
        assertFalse(FpLedger.givesFp(new SampleData(BAUXITE, Identifier.withDefaultNamespace("the_end")), planets, rules));
        assertTrue(FpLedger.givesFp(new SampleData(BAUXITE, OVERWORLD), planets, rules));
    }

    @Test
    void formatsHundredths() {
        assertEquals("12.34", FpLedger.format(1234));
        assertEquals("0.05", FpLedger.format(5));
    }
}
