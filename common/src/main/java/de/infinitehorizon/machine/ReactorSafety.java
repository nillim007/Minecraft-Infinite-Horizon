package de.infinitehorizon.machine;

/**
 * Sicherheit des Fusionsreaktors (6.3, reine Rechnung): Fehlt die Kühlung {@code shutdownAfter} Ticks lang, schaltet er ab;
 * er startet neu, sobald die Kühlung {@code restartAfter} Ticks ohne Unterbrechung läuft.
 *
 * @param withoutCooling Ticks in Folge ohne Kühlung
 * @param withCooling    Ticks in Folge mit Kühlung
 * @param shutdown       abgeschaltet (Status {@code error})
 */
public record ReactorSafety(int withoutCooling, int withCooling, boolean shutdown) {

    public static final ReactorSafety START = new ReactorSafety(0, 0, false);

    /** Ein Tick mit oder ohne Kühlung. */
    public ReactorSafety step(boolean cooled, int shutdownAfter, int restartAfter) {
        int without = cooled ? 0 : withoutCooling + 1;
        int with = cooled ? withCooling + 1 : 0;
        boolean off = shutdown;
        if (!off && without >= shutdownAfter) {
            off = true;
        } else if (off && with >= restartAfter) {
            off = false;
        }
        return new ReactorSafety(without, with, off);
    }
}
