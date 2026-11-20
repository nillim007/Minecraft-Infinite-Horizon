package de.infinitehorizon.recipe;

/**
 * Mengen-Multiplikator für Montagetisch-Rezepte (22.3, 24.4 {@code recipe_amount_multiplier}) als reine Funktion:
 * Menge = max(1, aufgerundet(Menge × Faktor)), nur für stapelbare Zutaten (Stapelgröße > 1). Vergrößert wird höchstens so
 * weit, dass alle Zutaten noch in die Eingabeslots passen; sonst wird der Faktor schrittweise bis 1 verkleinert.
 * Verkleinert wird ein Rezept höchstens bis zu seinem Mindestfaktor ({@code min_multiplier}, von gen_resources.py
 * berechnet), damit keine zwei Montagetisch-Rezepte zutatengleich werden (Entscheidung zu Kapitel 28, 19.11.2026).
 */
public final class AmountMultiplier {

    /** Schrittweite beim Verkleinern des Faktors, bis das Rezept in die Slots passt. */
    private static final double STEP = 0.01;

    private AmountMultiplier() {
    }

    /**
     * @param counts     Rezeptmengen je Zutat
     * @param maxStacks  Stapelgröße je Zutat (1 = unstapelbar, bleibt unverändert)
     * @param multiplier Wert aus der Config (z. B. 2.0)
     * @param slots      Zahl der Eingabeslots (Montagetisch: 9)
     * @return neue Mengen (gleiche Reihenfolge)
     */
    public static int[] scale(int[] counts, int[] maxStacks, double multiplier, int slots) {
        return scale(counts, maxStacks, multiplier, 0.0, slots);
    }

    /**
     * Wie {@link #scale(int[], int[], double, int)}, aber unter 1 nie kleiner als {@code minMultiplier}.
     *
     * @param minMultiplier Mindestfaktor des Rezepts (0 = keiner)
     */
    public static int[] scale(int[] counts, int[] maxStacks, double multiplier, double minMultiplier, int slots) {
        if (multiplier > 0 && multiplier < 1.0) {
            multiplier = Math.min(1.0, Math.max(multiplier, minMultiplier));
        }
        if (multiplier <= 0 || multiplier == 1.0) {
            return counts.clone();
        }
        double f = multiplier;
        int[] scaled = apply(counts, maxStacks, f);
        while (f > 1.0 && slotsNeeded(scaled, maxStacks) > slots) {
            f = Math.max(1.0, f - STEP);
            scaled = apply(counts, maxStacks, f);
        }
        return scaled;
    }

    private static int[] apply(int[] counts, int[] maxStacks, double f) {
        int[] out = new int[counts.length];
        for (int i = 0; i < counts.length; i++) {
            out[i] = maxStacks[i] > 1 ? Math.max(1, (int) Math.ceil(counts[i] * f - 1e-9)) : counts[i];
        }
        return out;
    }

    /** Wie viele Slots belegen diese Mengen mindestens? */
    public static int slotsNeeded(int[] counts, int[] maxStacks) {
        int n = 0;
        for (int i = 0; i < counts.length; i++) {
            int stack = Math.max(1, maxStacks[i]);
            n += (counts[i] + stack - 1) / stack;
        }
        return n;
    }
}
