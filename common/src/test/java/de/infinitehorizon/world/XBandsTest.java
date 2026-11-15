package de.infinitehorizon.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Merkur-Biome nach X (13.4), Sondenwahl (12.3), Blick auf den Spiegelkäfer (18). */
class XBandsTest {

    private static final int[] MINS = {Integer.MIN_VALUE, -2000, 2000};
    private static final int[] MAXS = {-2000, 2000, Integer.MAX_VALUE};

    @Test
    void bandsByX() {
        assertEquals(0, XBands.bandIndex(-2001, MINS, MAXS), "Nachtseite");
        assertEquals(1, XBands.bandIndex(-2000, MINS, MAXS), "Gürtel ab −2.000");
        assertEquals(1, XBands.bandIndex(0, MINS, MAXS));
        assertEquals(2, XBands.bandIndex(2000, MINS, MAXS), "Tagseite ab 2.000");
    }

    @Test
    void inclusionsAreRareAndStable() {
        int hits = 0;
        int cells = 0;
        for (int cx = -50; cx < 50; cx++) {
            for (int cz = -50; cz < 50; cz++) {
                cells++;
                if (XBands.inclusionAt(cx * 256 + 128, cz * 256 + 128, 256, 48, 0.08)) {
                    hits++;
                }
            }
        }
        double share = (double) hits / cells;
        assertTrue(share > 0.05 && share < 0.11, "Anteil " + share);
        assertEquals(XBands.inclusionAt(1000, 2000, 256, 48, 0.08),
                XBands.inclusionAt(1000, 2000, 256, 48, 0.08));
        assertFalse(XBands.inclusionAt(0, 0, 256, 48, 0.0), "Anteil 0");
    }

    @Test
    void probeFits() {
        String probe = "infinite_horizon:probe";
        String advanced = "infinite_horizon:advanced_probe";
        assertTrue(XBands.probeFits("probe", probe, probe));
        assertFalse(XBands.probeFits("probe", probe, advanced));
        assertTrue(XBands.probeFits("advanced_probe", advanced, advanced));
        assertTrue(XBands.probeFits("advanced_probe", advanced, probe), "erweiterte Sonde auch für Ring 1");
        assertTrue(XBands.probeFits("probe", probe, null));
    }

    @Test
    void gaze() {
        double[] eye = {0, 1.6, 0};
        assertTrue(XBands.looksAt(eye, new double[]{0, 0, 1}, new double[]{0, 1.6, 10}, 16, 0.975));
        assertFalse(XBands.looksAt(eye, new double[]{1, 0, 0}, new double[]{0, 1.6, 10}, 16, 0.975), "schaut weg");
        assertFalse(XBands.looksAt(eye, new double[]{0, 0, 1}, new double[]{0, 1.6, 20}, 16, 0.975), "zu weit");
    }
}
