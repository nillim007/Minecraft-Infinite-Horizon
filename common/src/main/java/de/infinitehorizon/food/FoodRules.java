package de.infinitehorizon.food;

import java.util.List;

/** Reine Regeln der Nahrung (17, 3A.1): Ration packen, Wachstum im Hydrokultur-Becken. */
public final class FoodRules {

    private FoodRules() {
    }

    /** Nahrungswert einer Eingabe (Vanilla {@code food}: Nahrung und Sättigung). */
    public record Food(int nutrition, float saturation) {
    }

    /** {@code space_ration}: Nahrung und Sättigung = Summe der Eingaben, je höchstens {@code max}. */
    public static Nutrition pack(List<Food> inputs, int nutritionMax, float saturationMax) {
        int n = 0;
        float s = 0;
        for (Food f : inputs) {
            n += f.nutrition();
            s += f.saturation();
        }
        return new Nutrition(Math.min(n, nutritionMax), Math.min(s, saturationMax));
    }

    /** Wächst die Pflanze? Licht reicht und (außer auf der Erde) eine aktive Kuppel ist da (8.5). */
    public static boolean canGrow(int light, int lightMin, boolean airless, boolean requiresDome) {
        return light >= lightMin && !(requiresDome && airless);
    }

    /** Ticks je Altersstufe: gedüngt (ammonia_fertilizer, 17) schneller. */
    public static int ticksPerStage(int normal, int fertilized, boolean isFertilized) {
        return isFertilized ? fertilized : normal;
    }

    /**
     * Stamm zum Setzling (17: „4 Stämme der Holzart“): {@code oak_sapling} → {@code oak_log},
     * {@code mangrove_propagule} → {@code mangrove_log}.
     */
    public static String logFor(String saplingPath) {
        // SPEC-GAP: Zuordnung über den Namen; ohne passenden Stamm gilt oak_log (Aufrufer prüft)
        for (String suffix : new String[]{"_sapling", "_propagule"}) {
            if (saplingPath.endsWith(suffix)) {
                return saplingPath.substring(0, saplingPath.length() - suffix.length()) + "_log";
            }
        }
        return "oak_log";
    }
}
