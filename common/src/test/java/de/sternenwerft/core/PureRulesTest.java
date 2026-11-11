package de.sternenwerft.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.sternenwerft.core.grid.EnergyGrid;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.ServerConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reine Regeln aus M1: Netzverteilung (4.4), Balance-Zusammenführung (25.7), Config-Prüfung (24.4). */
class PureRulesTest {

    private static BalanceManager specBalance() throws Exception {
        return BalanceManager.loadDirectory(Path.of(System.getProperty("sternenwerft.specData"), "balance"));
    }

    @Test
    void gridDistributesEvenlyAndPassesRestOn() {
        // 3 Empfänger, einer will nur 10: der Rest geht gleichmäßig an die anderen
        assertArrayEquals(new long[]{10, 123, 123}, EnergyGrid.distribute(256, new long[]{10, 500, 500}, 3));
        // genug für alle
        assertArrayEquals(new long[]{20, 30}, EnergyGrid.distribute(256, new long[]{20, 30}, 2));
        // niemand will etwas
        assertArrayEquals(new long[]{0, 0}, EnergyGrid.distribute(256, new long[]{0, 0}, 2));
    }

    @Test
    void addonEntriesExtendAndOverride() {
        Map<String, JsonObject> files = new HashMap<>();
        BalanceManager.merge(files, "machines", JsonParser.parseString(
                "{\"entries\":{\"sternenwerft:crusher\":{\"ticks\":200},\"sternenwerft:press\":{\"ticks\":100}}}").getAsJsonObject());
        BalanceManager.merge(files, "machines", JsonParser.parseString(
                "{\"entries\":{\"sternenwerft:crusher\":{\"ticks\":50},\"addon:mill\":{\"ticks\":80}}}").getAsJsonObject());
        BalanceManager b = new BalanceManager(files);
        assertEquals(50, b.entry("machines", "sternenwerft:crusher").orElseThrow().get("ticks").getAsInt());
        assertEquals(100, b.entry("machines", "sternenwerft:press").orElseThrow().get("ticks").getAsInt());
        assertEquals(80, b.entry("machines", "addon:mill").orElseThrow().get("ticks").getAsInt());

        BalanceManager.merge(files, "machines", JsonParser.parseString(
                "{\"replace\":true,\"entries\":{\"addon:mill\":{\"ticks\":1}}}").getAsJsonObject());
        assertEquals(false, new BalanceManager(files).entry("machines", "sternenwerft:crusher").isPresent());
    }

    @Test
    void withValueChangesOnlyTheCopy() throws Exception {
        BalanceManager spec = specBalance();
        int base = spec.machine("sternenwerft:crusher").ticks();
        BalanceManager changed = spec.withValue("machines", "sternenwerft:crusher", "ticks", new JsonPrimitive(base * 2));
        assertEquals(base * 2, changed.machine("sternenwerft:crusher").ticks());
        assertEquals(base, spec.machine("sternenwerft:crusher").ticks());
    }

    @Test
    void generatorHasFuelSlot() throws Exception {
        assertEquals(1, specBalance().generator("sternenwerft:combustion_generator").fuelSlots());
    }

    @Test
    void configValuesAreClampedOrReplaced() throws Exception {
        JsonObject defaults = specBalance().entry("misc", "config_defaults").orElseThrow();
        JsonObject mult = defaults.getAsJsonObject("recipe_amount_multiplier");
        assertEquals(mult.get("max").getAsDouble(), ServerConfig.validate(mult, new JsonPrimitive(99)).getAsDouble());
        assertEquals(mult.get("default").getAsDouble(), ServerConfig.validate(mult, new JsonPrimitive("x")).getAsDouble());
        JsonObject freq = defaults.getAsJsonObject("event_frequency");
        assertEquals("rare", ServerConfig.validate(freq, new JsonPrimitive("rare")).getAsString());
        assertEquals(freq.get("default").getAsString(), ServerConfig.validate(freq, new JsonPrimitive("sometimes")).getAsString());
        JsonObject mobs = defaults.getAsJsonObject("planet_mobs");
        assertEquals(false, ServerConfig.validate(mobs, new JsonPrimitive(false)).getAsBoolean());
    }
}
