package de.infinitehorizon.machine;

import de.infinitehorizon.station.StationRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fusionsreaktor (6.3): Abschaltung nach 40 t ohne Kühlung, Neustart nach 100 t mit Kühlung; Schwerkraftplatte. */
class ReactorSafetyTest {

    @Test
    void shutsDownAfterFortyTicks() {
        ReactorSafety s = ReactorSafety.START;
        for (int i = 0; i < 39; i++) {
            s = s.step(false, 40, 100);
        }
        assertFalse(s.shutdown(), "nach 39 t noch an");
        s = s.step(false, 40, 100);
        assertTrue(s.shutdown(), "nach 40 t aus");
    }

    @Test
    void restartsAfterHundredTicksWithoutInterruption() {
        ReactorSafety s = new ReactorSafety(40, 0, true);
        for (int i = 0; i < 99; i++) {
            s = s.step(true, 40, 100);
        }
        assertTrue(s.shutdown(), "nach 99 t noch aus");
        s = s.step(false, 40, 100);
        for (int i = 0; i < 99; i++) {
            s = s.step(true, 40, 100);
        }
        assertTrue(s.shutdown(), "Unterbrechung setzt zurück");
        s = s.step(true, 40, 100);
        assertFalse(s.shutdown(), "nach 100 t ohne Unterbrechung an");
    }

    @Test
    void gravityPlateArea() {
        // 9 × 9 Grundfläche um die Platte bei 0/0/0, 8 Blöcke hoch
        assertTrue(StationRules.inPlateArea(0, 0, 0, 0.5, 1, 0.5, 9, 9, 8));
        assertTrue(StationRules.inPlateArea(0, 0, 0, 4.9, 8.9, -3.9, 9, 9, 8));
        assertFalse(StationRules.inPlateArea(0, 0, 0, 5.1, 1, 0.5, 9, 9, 8), "seitlich außerhalb");
        assertFalse(StationRules.inPlateArea(0, 0, 0, 0.5, 9, 0.5, 9, 9, 8), "zu hoch");
        assertFalse(StationRules.inPlateArea(0, 0, 0, 0.5, 0.5, 0.5, 9, 9, 8), "in der Platte");
    }
}
