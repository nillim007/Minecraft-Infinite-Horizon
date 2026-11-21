package de.infinitehorizon.recipe;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Exakter Abgleich des Montagetischs (22.3) als reine Funktion: Jeder belegte Slot gehört zu genau einer Zutat, und
 * die Summe je Zutat ist genau die geforderte Menge. Eine Zutat darf mehrere Slots belegen; Überschuss oder fremde
 * Items verhindern den Abgleich. Beispiel: 4 Eisenplatten + 1 Kupferrohr passen auf „4 + 1“, nicht auf „2 + 1“.
 */
public final class ExactMatch {

    private ExactMatch() {
    }

    /**
     * Ordnet die belegten Slots den Zutaten zu.
     *
     * @param slots    Slot-Inhalte (leere Slots werden über {@code isEmpty} übersprungen)
     * @param required geforderte Zutaten
     * @param matches  passt der Slot-Inhalt zur Zutat?
     * @param count    Menge im Slot
     * @param amount   geforderte Menge der Zutat
     * @return Zutat-Index je Slot (-1 = leerer Slot) oder null, wenn kein exakter Abgleich möglich ist
     */
    public static <S, R> int @Nullable [] assign(List<S> slots, List<R> required, Predicate<S> isEmpty,
                                                  BiPredicate<R, S> matches, ToIntFunction<S> count, ToIntFunction<R> amount) {
        int[] result = new int[slots.size()];
        Arrays.fill(result, -1);
        List<Integer> used = new ArrayList<>();
        long have = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (!isEmpty.test(slots.get(i))) {
                used.add(i);
                have += count.applyAsInt(slots.get(i));
            }
        }
        int[] remaining = new int[required.size()];
        long need = 0;
        for (int r = 0; r < required.size(); r++) {
            remaining[r] = amount.applyAsInt(required.get(r));
            need += remaining[r];
        }
        // Schnelltest: gleiche Gesamtsumme, sonst kann es nicht exakt passen
        if (have != need || (used.isEmpty() && need > 0)) {
            return null;
        }
        return place(0, used, slots, required, matches, count, remaining, result) ? result : null;
    }

    private static <S, R> boolean place(int k, List<Integer> used, List<S> slots, List<R> required, BiPredicate<R, S> matches,
                                        ToIntFunction<S> count, int[] remaining, int[] result) {
        if (k == used.size()) {
            for (int r : remaining) {
                if (r != 0) {
                    return false;
                }
            }
            return true;
        }
        int slot = used.get(k);
        S s = slots.get(slot);
        int c = count.applyAsInt(s);
        for (int r = 0; r < required.size(); r++) {
            if (remaining[r] >= c && matches.test(required.get(r), s)) {
                remaining[r] -= c;
                result[slot] = r;
                if (place(k + 1, used, slots, required, matches, count, remaining, result)) {
                    return true;
                }
                remaining[r] += c;
                result[slot] = -1;
            }
        }
        return false;
    }

    /**
     * Abgleich „mindestens“ (Montagetisch, Ä31): Jeder belegte Slot wird einer passenden Zutat zugeordnet, und jede
     * Zutat bekommt zusammen mindestens ihre Menge. Fremde Items in einem Slot verhindern den Abgleich.
     *
     * @return Zutat-Index je Slot (-1 = leerer Slot) oder null
     */
    public static <S, R> int @Nullable [] assignAtLeast(List<S> slots, List<R> required, Predicate<S> isEmpty,
                                                         BiPredicate<R, S> matches, ToIntFunction<S> count, ToIntFunction<R> amount) {
        int[] result = new int[slots.size()];
        Arrays.fill(result, -1);
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!isEmpty.test(slots.get(i))) {
                used.add(i);
            }
        }
        if (used.isEmpty()) {
            return required.isEmpty() ? result : null;
        }
        long[] have = new long[required.size()];
        return placeAtLeast(0, used, slots, required, matches, count, amount, have, result) ? result : null;
    }

    private static <S, R> boolean placeAtLeast(int k, List<Integer> used, List<S> slots, List<R> required,
                                               BiPredicate<R, S> matches, ToIntFunction<S> count, ToIntFunction<R> amount,
                                               long[] have, int[] result) {
        if (k == used.size()) {
            for (int r = 0; r < required.size(); r++) {
                if (have[r] < amount.applyAsInt(required.get(r))) {
                    return false;
                }
            }
            return true;
        }
        int slot = used.get(k);
        S s = slots.get(slot);
        int c = count.applyAsInt(s);
        for (int r = 0; r < required.size(); r++) {
            if (matches.test(required.get(r), s)) {
                have[r] += c;
                result[slot] = r;
                if (placeAtLeast(k + 1, used, slots, required, matches, count, amount, have, result)) {
                    return true;
                }
                have[r] -= c;
                result[slot] = -1;
            }
        }
        return false;
    }
}
