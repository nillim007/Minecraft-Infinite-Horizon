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

    /** Ziel beim Fall unter y 0 (14.3): Planet unter einem Orbit ({@code earth_orbit} → Overworld, sonst ohne Suffix). */
    public static String surfaceBelow(String orbitPath) {
        String base = orbitPath.endsWith("_orbit") ? orbitPath.substring(0, orbitPath.length() - "_orbit".length()) : orbitPath;
        return "earth".equals(base) ? "minecraft:overworld" : base;
    }
}
