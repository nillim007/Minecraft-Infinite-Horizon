package de.infinitehorizon.station;

/** Reine Regeln der Station und der Schwerelosigkeit (14.1, 14.3, 12.9). */
public final class StationRules {

    private StationRules() {
    }

    /** Eingebaute Solarzellen des Kerns: Panels × Leistung × Solarfaktor (Erdorbit: 4 × 20 × 1 = 80 J/t). */
    public static double builtinSolar(int panels, double perPanel, double solarFactor) {
        return panels * perPanel * solarFactor;
    }

    /**
     * Schwerkraft (12.9, 14.3): im Orbit mit {@code magnet_boots} auf festem Grund {@code bootsG}, sonst g des Planeten.
     */
    public static double gravity(double planetG, boolean orbit, boolean magnetBoots, boolean blockBelow, double bootsG) {
        return orbit && magnetBoots && blockBelow ? bootsG : planetG;
    }

    /**
     * Wirkbereich der Schwerkraftplatte (stations.json {@code area}, {@code above_plate}): Grundfläche
     * {@code width} × {@code depth} um die Platte, {@code height} Blöcke über ihr.
     */
    public static boolean inPlateArea(int px, int py, int pz, double x, double y, double z, int width, int depth, int height) {
        double hw = width / 2.0;
        double hd = depth / 2.0;
        double cx = px + 0.5;
        double cz = pz + 0.5;
        return x >= cx - hw && x < cx + hw && z >= cz - hd && z < cz + hd && y >= py + 1 && y < py + 1 + height;
    }

    /** Ziel beim Fall unter y 0 (14.3): Planet unter einem Orbit ({@code earth_orbit} → Overworld, sonst ohne Suffix). */
    public static String surfaceBelow(String orbitPath) {
        String base = orbitPath.endsWith("_orbit") ? orbitPath.substring(0, orbitPath.length() - "_orbit".length()) : orbitPath;
        return "earth".equals(base) ? "minecraft:overworld" : base;
    }
}
