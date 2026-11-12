package de.infinitehorizon.environment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3b (27.5): {@link EnvironmentCalculator} für den Mond bei Tag und Nacht, den Polarkrater, die Erde und den Nether;
 * dazu Belastungsschritt (8.2) und Schwerkraft-Modifikatoren (12.9). Daten aus spec/data.
 */
class EnvironmentCalculatorTest {

    private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
    private static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier POLAR = Identifier.fromNamespaceAndPath("infinite_horizon", "moon_polar_crater");
    private static final long NOON = 6000;
    private static final long MIDNIGHT = 18000;

    private static EnvironmentRules rules;
    private static BalanceManager balance;
    private static PlanetRegistry registry;

    @BeforeAll
    static void load() throws Exception {
        Path data = Path.of(System.getProperty("infinite_horizon.specData"));
        balance = BalanceManager.loadDirectory(data.resolve("balance"));
        rules = EnvironmentRules.from(balance);
        JsonObject doc = JsonParser.parseString(Files.readString(data.resolve("planets.json"))).getAsJsonObject();
        Map<Identifier, JsonObject> raw = new LinkedHashMap<>();
        for (JsonElement e : doc.getAsJsonArray("entries")) {
            JsonObject o = e.getAsJsonObject();
            raw.put(Identifier.parse(o.get("dimension").getAsString()), o);
        }
        registry = new PlanetRegistry(raw);
    }

    private static Planet planet(Identifier id) {
        return registry.planet(id).orElseThrow();
    }

    private static EnvironmentResult at(Planet p, Identifier biome, long time, int y) {
        List<Planet.Zone> zones = PlanetResolver.zonesAt(p, y, null, b -> b.equals(biome), b -> false);
        return EnvironmentCalculator.compute(new EnvironmentInput(p, zones, biome, time, 0, false, null, rules.noSuit()), rules);
    }

    @Test
    void moonAtNoon() {
        EnvironmentResult r = at(planet(MOON), Identifier.fromNamespaceAndPath("infinite_horizon", "moon_mare"), NOON, 64);
        assertFalse(r.breathable());
        assertEquals(Pressure.VACUUM, r.pressure());
        assertEquals(120.0, r.temperature(), 1e-9);
        assertEquals(1, r.radiation());
        assertEquals(EnumSet.of(Hazard.OXYGEN, Hazard.PRESSURE, Hazard.HEAT), r.hazards());
    }

    @Test
    void moonAtMidnight() {
        EnvironmentResult r = at(planet(MOON), Identifier.fromNamespaceAndPath("infinite_horizon", "moon_highlands"), MIDNIGHT, 64);
        assertEquals(-170.0, r.temperature(), 1e-9);
        assertEquals(EnumSet.of(Hazard.OXYGEN, Hazard.PRESSURE, Hazard.COLD), r.hazards());
    }

    @Test
    void moonDuskInterpolatesDaylight() {
        // 12.900 t liegt mitten in der Dämmerung (Kurve 12.000 → 13.800): Tageslicht 0,5
        assertEquals(0.5, rules.daylight(12900), 1e-9);
        EnvironmentResult r = at(planet(MOON), null, 12900, 64);
        assertEquals(-25.0, r.temperature(), 1e-9);
        assertTrue(r.has(Hazard.COLD));
    }

    @Test
    void polarCraterIsAlwaysColdAndDark() {
        EnvironmentResult r = at(planet(MOON), POLAR, NOON, 64);
        assertEquals(-190.0, r.temperature(), 1e-9);
        assertEquals(0.0, r.solarFactor(), 1e-9);
        assertTrue(r.has(Hazard.COLD));
        assertFalse(r.has(Hazard.HEAT));
    }

    @Test
    void earthHasNoHazards() {
        EnvironmentResult r = at(planet(OVERWORLD), Identifier.withDefaultNamespace("plains"), NOON, 64);
        assertTrue(r.breathable());
        assertEquals(Pressure.NORMAL, r.pressure());
        assertEquals(20.0, r.temperature(), 1e-9);
        assertTrue(r.hazards().isEmpty());
    }

    @Test
    void netherCountsAsEarth() {
        Planet p = PlanetResolver.planetFor(NETHER, registry);
        assertSame(planet(OVERWORLD), p);
        EnvironmentResult r = at(p, Identifier.withDefaultNamespace("nether_wastes"), MIDNIGHT, 64);
        assertTrue(r.breathable());
        assertTrue(r.hazards().isEmpty());
    }

    @Test
    void radiationCoverStormAndDome() {
        Planet moon = planet(MOON);
        var noon = new EnvironmentInput(moon, List.of(), null, NOON, 3, false, null, rules.noSuit());
        assertEquals(0, EnvironmentCalculator.compute(noon, rules).radiation(), "3 Blöcke Abdeckung: 1 − 2, begrenzt auf 0");
        var storm = new EnvironmentInput(moon, List.of(), null, NOON, 0, true, null, rules.noSuit());
        EnvironmentResult s = EnvironmentCalculator.compute(storm, rules);
        assertEquals(3, s.radiation());
        assertTrue(s.has(Hazard.RADIATION));
        var dome = new EnvironmentInput(moon, List.of(), null, NOON, 0, false, new EnvironmentInput.DomeCover(1, 20.0), rules.noSuit());
        EnvironmentResult d = EnvironmentCalculator.compute(dome, rules);
        assertTrue(d.breathable());
        assertEquals(Pressure.NORMAL, d.pressure());
        assertTrue(d.hazards().isEmpty(), "Kuppel T1 auf dem Mond: " + d.hazards());
    }

    @Test
    void hazardStepReaches100PercentWithTwentiethCheck() {
        EnvironmentRules.HazardRule oxygen = rules.hazards().get(Hazard.OXYGEN);
        int v = 0;
        int checks = 0;
        while (v < oxygen.buildupTicks()) {
            v = HazardState.step(v, true, rules.intervalTicks(), oxygen.buildupTicks(), oxygen.decayTicks());
            checks++;
        }
        assertEquals(20, checks);
        int down = 0;
        while (v > 0) {
            v = HazardState.step(v, false, rules.intervalTicks(), oxygen.buildupTicks(), oxygen.decayTicks());
            down++;
        }
        assertEquals(10, down, "Abbau in 100 t");
    }

    @Test
    void hazardStateSurvivesCodec() {
        HazardState s = new HazardState();
        s.setBuildup(Hazard.OXYGEN, 120);
        s.setBuildup(Hazard.COLD, 40);
        var json = HazardState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, s).getOrThrow();
        HazardState back = HazardState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(120, back.buildup(Hazard.OXYGEN));
        assertEquals(40, back.buildup(Hazard.COLD));
        assertEquals(0, back.buildup(Hazard.HEAT));
    }

    @Test
    void gravityModifiersForTheMoon() {
        GravityService.Rules r = GravityService.Rules.from(balance);
        double g = planet(MOON).gravity();
        double[] m = GravityService.modifierAmounts(g, r);
        // Basiswerte 0,08 / 1 / 3 (12.9): add_multiplied_total mit g − 1, add_value auf 3
        assertEquals(0.0136, 0.08 * (1 + m[0]), 1e-9);
        assertEquals(0.17, 1 * (1 + m[1]), 1e-9);
        assertEquals(17.6, Math.round((3 + m[2]) * 10) / 10.0, 1e-9);
        assertEquals(0.0, GravityService.modifierAmounts(1.0, r)[0], 1e-12);
    }
}
