package de.infinitehorizon.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Startbedingungen der Routen (20.4) und Treibstoff-Warnung (20.3). */
class RouteRulesTest {

    private static RouteRules.Context ctx(long now, long last, long amount, int stacks, int fluids, long targetCount, long demand) {
        return new RouteRules.Context(now, last, amount, stacks, 108, fluids, 4, targetCount, demand);
    }

    @Test
    void demandRouteStopsAtZero() {
        assertFalse(RouteRules.shouldStart(RouteRules.Condition.UNTIL_DEMAND_MET, ctx(0, 0, 0, 10, 0, 0, 0)));
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.UNTIL_DEMAND_MET, ctx(0, 0, 0, 10, 0, 0, 1)));
    }

    @Test
    void noCargoNoStart() {
        for (RouteRules.Condition c : RouteRules.Condition.values()) {
            assertFalse(RouteRules.shouldStart(c, ctx(10000, 0, 1, 0, 0, 0, 100)), c.id());
        }
    }

    @Test
    void conditions() {
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.EVERY_N_TICKS, ctx(1300, 100, 1200, 1, 0, 0, 0)));
        assertFalse(RouteRules.shouldStart(RouteRules.Condition.EVERY_N_TICKS, ctx(1299, 100, 1200, 1, 0, 0, 0)));
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.EVERY_N_TICKS, ctx(5, 0, 1200, 1, 0, 0, 0)), "neue Route sofort");
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.WHEN_TARGET_BELOW, ctx(0, 0, 20, 1, 0, 10, 0)));
        assertFalse(RouteRules.shouldStart(RouteRules.Condition.WHEN_TARGET_BELOW, ctx(0, 0, 10, 1, 0, 10, 0)));
        assertFalse(RouteRules.shouldStart(RouteRules.Condition.WHEN_FULL, ctx(0, 0, 0, 107, 3, 0, 0)));
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.WHEN_FULL, ctx(0, 0, 0, 108, 0, 0, 0)));
        assertTrue(RouteRules.shouldStart(RouteRules.Condition.WHEN_FULL, ctx(0, 0, 0, 0, 4, 0, 0)));
    }

    @Test
    void fuelWarning() {
        // Pluto: 2 × 29.000 = 58.000 passt in 64.000
        assertFalse(RouteRules.fuelWarning(29000, 29000, 64000, false));
        assertTrue(RouteRules.fuelWarning(33000, 33000, 64000, false));
        assertFalse(RouteRules.fuelWarning(33000, 33000, 64000, true));
    }

    @Test
    void networkCountsRoutesWithRocketsAndFuel() {
        assertTrue(RouteRules.active(2, 0));
        assertFalse(RouteRules.active(0, 0), "ohne Rakete");
        assertFalse(RouteRules.active(3, 1), "eine Rakete ohne Treibstoff");
        assertFalse(RouteRules.network(4, 5));
        assertTrue(RouteRules.network(5, 5));
        assertFalse(RouteRules.network(9, 0), "Erfolg ohne Wert");
    }
}
