package de.infinitehorizon.space;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Landefähre (12.7): Kosten ohne Transfer, Verbindungen über {@code lander_connects_to}. */
class LanderTest {

    private static final Identifier ORBIT = Identifier.fromNamespaceAndPath("infinite_horizon", "earth_orbit");
    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier MARS = Identifier.fromNamespaceAndPath("infinite_horizon", "mars");

    @Test
    void costWithoutTransfer() {
        FuelCalculator.Constants c = new FuelCalculator.Constants(32000, 6000, 2000, 0.25, 32000);
        FuelCalculator.Body orbit = new FuelCalculator.Body(ORBIT, 0.005, 0, false, 0.0);
        FuelCalculator.Body moon = new FuelCalculator.Body(MOON, 0.17, 1, true, null);
        // Orbit → Mond: Start 0 (Override) + Landung 0,25 × 32.000 × 0,17 = 1.360
        assertEquals(1360, FuelCalculator.landerCost(orbit, moon, c));
        // Mond → Orbit: Start 32.000 × 0,17 = 5.440 + Landung 0 (landing_requires_fuel false)
        assertEquals(5440, FuelCalculator.landerCost(moon, orbit, c));
        assertEquals(FuelCalculator.Reach.BOTH, FuelCalculator.landerReach(6800, orbit, moon, c));
        assertEquals(FuelCalculator.Reach.ONE_WAY, FuelCalculator.landerReach(6799, orbit, moon, c));
    }

    @Test
    void connections() {
        String orbit = ORBIT.toString();
        assertTrue(LanderRoutes.connected(ORBIT, null, MOON, orbit), "Orbit → Mond");
        assertTrue(LanderRoutes.connected(MOON, orbit, ORBIT, null), "Mond → Orbit");
        assertFalse(LanderRoutes.connected(ORBIT, null, MARS, "infinite_horizon:mars_orbit"), "Erdorbit → Mars");
        assertFalse(LanderRoutes.connected(MOON, orbit, MARS, "infinite_horizon:mars_orbit"), "Mond → Mars");
    }
}
