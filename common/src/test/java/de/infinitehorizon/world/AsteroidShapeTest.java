package de.infinitehorizon.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Asteroiden (13.4) und feste Strukturlage. */
class AsteroidShapeTest {

    @Test
    void ellipsoid() {
        assertTrue(AsteroidShape.inside(0, 0, 0, 3, 3, 3));
        assertTrue(AsteroidShape.inside(3, 0, 0, 3, 3, 3));
        assertFalse(AsteroidShape.inside(3, 1, 0, 3, 3, 3));
        assertTrue(AsteroidShape.inside(0, 0, 5, 3, 2, 5), "lange Achse");
        assertFalse(AsteroidShape.inside(0, 3, 0, 3, 2, 5), "kurze Achse");
    }

    @Test
    void orePick() {
        double[] fr = {0.04, 0.02, 0.06};
        assertEquals(0, AsteroidShape.pick(0.01, fr), "Platin");
        assertEquals(1, AsteroidShape.pick(0.05, fr), "Iridium");
        assertEquals(2, AsteroidShape.pick(0.11, fr), "Nickel");
        assertEquals(-1, AsteroidShape.pick(0.12, fr), "Gestein");
    }

    @Test
    void fixedChunk() {
        assertTrue(XBands.fixedChunk(0, 0, 0, 0));
        assertTrue(XBands.fixedChunk(-1, 17, -1, 1));
        assertFalse(XBands.fixedChunk(0, 0, 1, 0));
    }
}
