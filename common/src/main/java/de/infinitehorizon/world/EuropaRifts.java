package de.infinitehorizon.world;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Spalten in der Eisdecke der Europa (13.1): Eine Spalte folgt der Nulllinie eines Rauschens; ihre Breite (3–6 Blöcke)
 * ändert sich langsam mit einem zweiten Rauschen. Abstand zur Nulllinie ≈ |n| / |∇n| in Blöcken. Reine Rechnung.
 */
public final class EuropaRifts {

    /** Maßstab der Spaltenlinien und der Breitenänderung in Blöcken (SPEC-GAP #105). */
    public static final double LINE_SCALE = 48.0;
    public static final double WIDTH_SCALE = 96.0;

    private EuropaRifts() {
    }

    /** Liegt die Spalte {@code x}/{@code z} in einem Riss mit Breite zwischen {@code minWidth} und {@code maxWidth}? */
    public static boolean crack(SimplexNoise lines, SimplexNoise widths, int x, int z, int minWidth, int maxWidth) {
        double s = 1.0 / LINE_SCALE;
        double n = lines.get(x * s, z * s);
        double gx = (lines.get((x + 1) * s, z * s) - lines.get((x - 1) * s, z * s)) / 2;
        double gz = (lines.get(x * s, (z + 1) * s) - lines.get(x * s, (z - 1) * s)) / 2;
        double grad = Math.sqrt(gx * gx + gz * gz);
        if (grad < 1e-12) {
            return false;
        }
        double t = (widths.get(x / WIDTH_SCALE, z / WIDTH_SCALE) + 1) / 2;
        double width = minWidth + Math.clamp(t, 0, 1) * (maxWidth - minWidth);
        return Math.abs(n) / grad < width / 2;
    }
}
