package de.infinitehorizon.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hitze des Kernreaktors (6.3): +2/t ohne Kühlung, −5/t mit, Notabschaltung bei 1.000, Neustart unter 200. */
class FissionHeatTest {

    private static final FissionHeat.Rules R = new FissionHeat.Rules(0, 1000, -5, 2, 800, 1000, 200);

    @Test
    void uncooledScramsAfterFiveHundredTicks() {
        FissionHeat h = FissionHeat.START;
        int ticks = 0;
        while (!h.scrammed()) {
            h = h.step(true, false, R);
            ticks++;
        }
        assertEquals(500, ticks);
        assertEquals(1000, h.heat());
        assertTrue(h.warning(R));
    }

    @Test
    void cooledStaysColdAndRestartsBelowThreshold() {
        FissionHeat cold = FissionHeat.START.step(true, true, R);
        assertEquals(0, cold.heat());
        FissionHeat h = new FissionHeat(1000, true);
        int ticks = 0;
        while (h.scrammed()) {
            h = h.step(false, true, R);
            ticks++;
        }
        assertTrue(h.heat() < 200);
        assertEquals(161, ticks);
        assertFalse(new FissionHeat(1000, true).step(false, false, R).heat() < 1000, "ohne Kühlung keine Abkühlung");
    }
}
