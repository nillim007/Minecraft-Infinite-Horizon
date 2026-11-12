package de.infinitehorizon.environment;

import de.infinitehorizon.data.BalanceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M3d (9.1): Anzugstufen aus suits.json, Schutz im Mond-Vakuum, O₂-Verbrauch in 200 t. */
class SuitsTest {

    private static BalanceManager previous;
    private static EnvironmentRules rules;

    @BeforeAll
    static void load() throws Exception {
        previous = BalanceManager.get();
        BalanceManager balance = BalanceManager.loadDirectory(Path.of(System.getProperty("infinite_horizon.specData")).resolve("balance"));
        BalanceManager.set(balance);
        rules = EnvironmentRules.from(balance);
    }

    @AfterAll
    static void restore() {
        BalanceManager.set(previous);
    }

    private static Suits.Tier pressureSuit() {
        return Suits.tiers().stream().filter(t -> t.id().equals("pressure_suit")).findFirst().orElseThrow();
    }

    @Test
    void pressureSuitValuesFromSuitsJson() {
        Suits.Tier t = pressureSuit();
        assertEquals(1, t.tier());
        assertEquals(-180.0, t.tempMin(), 1e-9);
        assertEquals(150.0, t.tempMax(), 1e-9);
        assertEquals(EnumSet.of(Pressure.VACUUM, Pressure.LOW, Pressure.NORMAL), t.pressures());
        assertEquals(4, t.pieces().size());
        assertTrue(t.pieces().get(1).endsWith("pressure_suit_chestplate"));
        assertEquals(1200, Suits.supply().capacity());
    }

    @Test
    void suitWithOxygenProtectsInMoonVacuum() {
        Protection p = Suits.protection(pressureSuit(), 600);
        // Mond mittags (120 °C, Vakuum, Strahlung 1) und nachts (−170 °C)
        for (double temp : new double[]{120, -170}) {
            EnvironmentResult r = new EnvironmentResult(false, temp, Pressure.VACUUM, 1, 1.0, "freeze", EnumSet.noneOf(Hazard.class));
            assertTrue(within(p, r), "Schutz bei " + temp + " °C");
        }
        Protection empty = Suits.protection(pressureSuit(), 0);
        assertFalse(empty.suppliesOxygen(), "ohne O₂ entsteht nur die Gefahr Sauerstoff");
        assertTrue(empty.pressures().contains(Pressure.VACUUM));
    }

    private static boolean within(Protection p, EnvironmentResult r) {
        return p.suppliesOxygen() && p.pressures().contains(r.pressure()) && r.temperature() >= p.tempMin()
                && r.temperature() <= p.tempMax() && r.radiation() <= p.radiationMax();
    }

    @Test
    void consumptionIsTenMillibucketsIn200Ticks() {
        // 1 mB je 20 t (T1), Faktor 1: in 200 t also 10 mB = 10.000 Tausendstel
        assertEquals(10_000, Suits.consumptionMilli(pressureSuit(), 200, 1.0));
        assertEquals(500, Suits.consumptionMilli(pressureSuit(), rules.intervalTicks(), 1.0));
        assertEquals(20_000, Suits.consumptionMilli(pressureSuit(), 200, 2.0), "Config oxygen_consumption 2,0");
    }
}
