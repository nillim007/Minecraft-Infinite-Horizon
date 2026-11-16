package de.infinitehorizon.world;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Risse der Eisdecke (13.1): Spalten 3–6 Blöcke breit, nur ein kleiner Teil der Fläche offen. */
class EuropaRiftsTest {

    private static final SimplexNoise LINES = new SimplexNoise(new XoroshiroRandomSource(7L));
    private static final SimplexNoise WIDTHS = new SimplexNoise(new XoroshiroRandomSource(11L));

    private static boolean crack(int x, int z) {
        return EuropaRifts.crack(LINES, WIDTHS, x, z, 3, 6);
    }

    private static int run(int x, int z, int dx, int dz) {
        int n = 1;
        for (int k = 1; crack(x + k * dx, z + k * dz) && k < 100; k++) {
            n++;
        }
        for (int k = 1; crack(x - k * dx, z - k * dz) && k < 100; k++) {
            n++;
        }
        return n;
    }

    @Test
    void widthAndShare() {
        int open = 0;
        int total = 0;
        List<Integer> widths = new ArrayList<>();
        for (int x = -600; x < 600; x += 3) {
            for (int z = -600; z < 600; z += 3) {
                total++;
                if (crack(x, z)) {
                    open++;
                    if (x % 30 == 0 && z % 30 == 0) {
                        widths.add(Math.min(run(x, z, 1, 0), run(x, z, 0, 1)));
                    }
                }
            }
        }
        double share = (double) open / total;
        assertTrue(share > 0.02 && share < 0.35, "Anteil offen " + share);
        assertTrue(widths.size() > 10, "zu wenige Proben " + widths.size());
        Collections.sort(widths);
        int median = widths.get(widths.size() / 2);
        assertTrue(median >= 3 && median <= 8, "mittlere Breite " + median + " " + widths);
    }
}
