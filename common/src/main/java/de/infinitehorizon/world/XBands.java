package de.infinitehorizon.world;

/** Reine Regeln der Biomquelle {@code x_bands} (13.4 Merkur); ohne Minecraft-Klassen testbar. */
public final class XBands {

    private XBands() {
    }

    /** Erstes Band mit {@code min ≤ x < max}; keins passt: das letzte. */
    public static int bandIndex(int x, int[] mins, int[] maxs) {
        for (int k = 0; k < mins.length; k++) {
            if (x >= mins[k] && x < maxs[k]) {
                return k;
            }
        }
        return mins.length - 1;
    }

    /**
     * Liegt (x, z) in einem Einsprengsel? Die Zelle wird über einen festen Hash gewählt (unabhängig vom Weltseed,
     * SPEC-GAP #101); die Scheibe sitzt in der Zellmitte.
     */
    public static boolean inclusionAt(int x, int z, int cell, int radius, double chance) {
        int cx = Math.floorDiv(x, cell);
        int cz = Math.floorDiv(z, cell);
        long h = cx * 0x9E3779B97F4A7C15L ^ cz * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        if ((h >>> 11) * 0x1.0p-53 >= chance) {
            return false;
        }
        double dx = x - (cx * (double) cell + cell / 2.0);
        double dz = z - (cz * (double) cell + cell / 2.0);
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    /**
     * Blick auf ein Ziel (Spiegelkäfer, 18): Abstand unter {@code maxDistance} und Kosinus zwischen Blickrichtung und
     * Richtung zum Ziel über {@code minCos}. Vektoren als {x, y, z}.
     */
    public static boolean looksAt(double[] eye, double[] view, double[] target, double maxDistance, double minCos) {
        double tx = target[0] - eye[0];
        double ty = target[1] - eye[1];
        double tz = target[2] - eye[2];
        double d = Math.sqrt(tx * tx + ty * ty + tz * tz);
        double v = Math.sqrt(view[0] * view[0] + view[1] * view[1] + view[2] * view[2]);
        return d > 0 && v > 0 && d < maxDistance && (view[0] * tx + view[1] * ty + view[2] * tz) / (d * v) > minCos;
    }

    /** Feste Strukturlage (13.4): Ist (chunkX, chunkZ) der Chunk des Blocks (x, z)? */
    public static boolean fixedChunk(int x, int z, int chunkX, int chunkZ) {
        return Math.floorDiv(x, 16) == chunkX && Math.floorDiv(z, 16) == chunkZ;
    }

    /** Sondenwahl (12.3): Feld {@code probe} des Planeten; die erweiterte Sonde erreicht auch Ziele der einfachen. */
    public static boolean probeFits(String probePath, String probeId, String required) {
        return required == null || required.equals(probeId) || probePath.equals("advanced_probe") && required.endsWith(":probe");
    }
}
