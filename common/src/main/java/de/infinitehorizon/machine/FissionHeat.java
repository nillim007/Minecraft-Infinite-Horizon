package de.infinitehorizon.machine;

/**
 * Hitze des Kernreaktors (6.3, reine Rechnung): mit Kühlung {@code cooled} je Tick (negativ), ohne Kühlung bei laufender
 * Spaltung {@code uncooled} je Tick. Ab {@code scramAt} Notabschaltung, Neustart sobald die Hitze unter
 * {@code restartBelow} liegt. Ohne Spaltung und ohne Kühlung bleibt die Hitze (keine eigene Abkühlung, SPEC-GAP #110).
 *
 * @param heat      Hitze zwischen {@code min} und {@code max}
 * @param scrammed  notabgeschaltet
 */
public record FissionHeat(int heat, boolean scrammed) {

    public static final FissionHeat START = new FissionHeat(0, false);

    /** Werte aus {@code safety} in generators.json. */
    public record Rules(int min, int max, int cooled, int uncooled, int warningAt, int scramAt, int restartBelow) {
    }

    /** Ein Tick: {@code burning} = Kernspaltung läuft, {@code cooled} = Kühlmittel fließt in diesem Tick. */
    public FissionHeat step(boolean burning, boolean cooled, Rules r) {
        int delta = cooled && (burning || heat > r.min()) ? r.cooled() : burning ? r.uncooled() : 0;
        int h = Math.clamp(heat + delta, r.min(), r.max());
        boolean s = scrammed;
        if (!s && h >= r.scramAt()) {
            s = true;
        } else if (s && h < r.restartBelow()) {
            s = false;
        }
        return new FissionHeat(h, s);
    }

    /** Ab {@code warningAt} warnt der Reaktor (Ton, blinkende Statuslampe). */
    public boolean warning(Rules r) {
        return heat >= r.warningAt();
    }
}
