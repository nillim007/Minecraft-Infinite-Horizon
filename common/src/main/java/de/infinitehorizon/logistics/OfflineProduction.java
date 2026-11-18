package de.infinitehorizon.logistics;

/**
 * Offline-Produktion eines Außenpostens (20.1), reine Funktion: {@code vergangene_Ticks × Rate × Faktor}, begrenzt durch
 * die vorhandenen Eingaben und den freien Platz. Gezählt wird nur ein Produzent, der beim Entladen {@code running} war.
 * „Rate“ = Ausgabe pro Vorgang ÷ Vorgangsdauer (6.1).
 */
public final class OfflineProduction {

    /** Keine Grenze (Eingaben oder Platz). */
    public static final long UNLIMITED = -1;

    /**
     * Zustand eines Produzenten beim Entladen.
     *
     * @param running         Status {@code running} beim Entladen
     * @param outputPerCycle  Ausgabe je Vorgang (Items oder mB)
     * @param cycleTicks      Vorgangsdauer in Ticks
     * @param availableCycles Vorgänge, für die die Eingaben reichen ({@link #UNLIMITED} ohne Eingaben)
     * @param freeSpace       freier Platz in Einheiten der Ausgabe ({@link #UNLIMITED} = unbegrenzt)
     */
    public record Snapshot(boolean running, long outputPerCycle, int cycleTicks, long availableCycles, long freeSpace) {

        /** Ausgabe je Tick. */
        public double rate() {
            return cycleTicks <= 0 ? 0 : (double) outputPerCycle / cycleTicks;
        }
    }

    private OfflineProduction() {
    }

    /** Ganze Vorgänge in {@code ticks}, gekürzt auf Eingaben und Platz. */
    public static long cycles(Snapshot s, double factor, long ticks) {
        if (!s.running() || s.cycleTicks() <= 0 || s.outputPerCycle() <= 0 || ticks <= 0 || factor <= 0) {
            return 0;
        }
        // kleiner Zuschlag gegen Rundungsfehler bei Faktoren wie 0,1
        long n = (long) Math.floor(ticks * factor / s.cycleTicks() + 1e-9);
        if (s.availableCycles() != UNLIMITED) {
            n = Math.min(n, Math.max(0, s.availableCycles()));
        }
        if (s.freeSpace() != UNLIMITED) {
            n = Math.min(n, Math.max(0, s.freeSpace()) / s.outputPerCycle());
        }
        return n;
    }

    /** Ausbeute in Einheiten der Ausgabe (Items oder mB). */
    public static long compute(Snapshot s, double factor, long ticks) {
        return cycles(s, factor, ticks) * s.outputPerCycle();
    }

    /**
     * Kurzform wie in der Abnahme (27, M8): {@code compute(running, Rate = outputPerCycle / cycleTicks, Faktor, Ticks)}
     * ohne Grenzen, z. B. {@code compute(true, 1, 100, 0.5, 24000) = 120}.
     */
    public static long compute(boolean running, long outputPerCycle, int cycleTicks, double factor, long ticks) {
        return compute(new Snapshot(running, outputPerCycle, cycleTicks, UNLIMITED, UNLIMITED), factor, ticks);
    }
}
