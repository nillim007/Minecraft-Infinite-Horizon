package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.RocketPartRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M4d (12.2, 12.4, 27.7): Treibstoffkosten und Schub-Gewicht mit den Werten aus spec/data. */
class FuelCalculatorTest {

    private static FuelCalculator.Constants constants;
    private static FuelCalculator.Body earth;
    private static FuelCalculator.Body moon;
    private static RocketPartRegistry parts;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("infinite_horizon", path);
    }

    @BeforeAll
    static void load() throws Exception {
        Path data = Path.of(System.getProperty("infinite_horizon.specData"));
        constants = FuelCalculator.Constants.from(BalanceManager.loadDirectory(data.resolve("balance")));
        JsonObject planets = JsonParser.parseString(Files.readString(data.resolve("planets.json"))).getAsJsonObject();
        for (var e : planets.getAsJsonArray("entries")) {
            JsonObject o = e.getAsJsonObject();
            switch (o.get("id").getAsString()) {
                case "minecraft:overworld" -> earth = FuelCalculator.Body.of(o);
                case "infinite_horizon:moon" -> moon = FuelCalculator.Body.of(o);
                default -> {
                }
            }
        }
        JsonObject rp = JsonParser.parseString(Files.readString(data.resolve("rocket_parts.json"))).getAsJsonObject();
        Map<Identifier, JsonObject> p = new LinkedHashMap<>();
        rp.getAsJsonArray("parts").forEach(e -> RocketPartRegistry.mergePart(p, e.getAsJsonObject()));
        Map<Integer, JsonObject> r = new LinkedHashMap<>();
        rp.getAsJsonArray("rules").forEach(e -> RocketPartRegistry.mergeRule(r, e.getAsJsonObject()));
        parts = new RocketPartRegistry(p, r);
    }

    @Test
    void earthToMoon() {
        assertEquals(35_360, FuelCalculator.cost(earth, moon, constants));
    }

    @Test
    void moonToEarth() {
        assertEquals(7_440, FuelCalculator.cost(moon, earth, constants));
    }

    @Test
    void roundTripMoon() {
        assertEquals(42_800, FuelCalculator.roundTrip(earth, moon, constants));
    }

    @Test
    void sameBodyHasNoTransfer() {
        assertEquals(FuelCalculator.launch(earth, constants) + FuelCalculator.landing(earth, constants),
                FuelCalculator.cost(earth, earth, constants));
    }

    /** T1 mit {@code tanks} vollen Tanks und vollem Frachtraum (27 Slots). */
    private static RocketAssembler.Result t1(int tanks, int cargoUsed) {
        List<Identifier> stack = new ArrayList<>();
        stack.add(id("rocket_engine_t1"));
        List<long[]> fuel = new ArrayList<>();
        for (int i = 0; i < tanks; i++) {
            stack.add(id("rocket_tank_t1"));
            fuel.add(new long[]{16_000, 16_000});
        }
        stack.add(id("rocket_cargo_bay"));
        stack.add(id("rocket_capsule_t1"));
        return RocketAssembler.assemble(new RocketAssembler.Snapshot(stack,
                Collections.nCopies(4, id("rocket_fin_t1")), fuel, cargoUsed), parts);
    }

    @Test
    void fourTanksFullCargoAllowed() {
        RocketAssembler.Result r = t1(4, 27);
        assertTrue(r.valid(), "Fehler: " + r.problems());
        assertEquals(27.35, r.config().mass(), 1e-9);
        assertEquals(1.10, r.config().thrustRatio(1.0), 0.005);
        assertTrue(r.config().thrustRatio(1.0) >= 1.0);
    }

    @Test
    void fiveTanksFullCargoRejected() {
        RocketAssembler.Result r = t1(5, 27);
        assertTrue(r.valid(), "Fehler: " + r.problems());
        assertEquals(31.35, r.config().mass(), 1e-9);
        assertEquals(0.96, r.config().thrustRatio(1.0), 0.005);
        assertFalse(r.config().thrustRatio(1.0) >= 1.0);
    }

    @Test
    void threeFinsAreAnError() {
        List<Identifier> stack = List.of(id("rocket_engine_t1"), id("rocket_tank_t1"), id("rocket_tank_t1"),
                id("rocket_tank_t1"), id("rocket_capsule_t1"));
        List<Identifier> fins = new ArrayList<>(Collections.nCopies(3, id("rocket_fin_t1")));
        fins.add(null);
        RocketAssembler.Result r = RocketAssembler.assemble(new RocketAssembler.Snapshot(stack, fins,
                List.of(new long[2], new long[2], new long[2]), 0), parts);
        assertFalse(r.valid());
        assertTrue(r.problems().contains(new RocketAssembler.Problem("fins", 3, 4)), "Fehler: " + r.problems());
    }

    @Test
    void twoTanksAreTooFew() {
        List<Identifier> stack = List.of(id("rocket_engine_t1"), id("rocket_tank_t1"), id("rocket_tank_t1"),
                id("rocket_capsule_t1"));
        RocketAssembler.Result r = RocketAssembler.assemble(new RocketAssembler.Snapshot(stack,
                Collections.nCopies(4, id("rocket_fin_t1")), List.of(new long[2], new long[2]), 0), parts);
        assertTrue(r.problems().contains(new RocketAssembler.Problem("count.tank", 2, 3)), "Fehler: " + r.problems());
    }
}
