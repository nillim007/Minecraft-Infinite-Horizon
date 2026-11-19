package de.infinitehorizon.logistics;

/**
 * Regeln des Handelspostens (23.3) als reine Funktionen: Ein Angebot heißt „gib X, erhalte Y“. Der Händler gibt
 * {@code giveCount} Stück X aus seinem Inventar und bekommt {@code getCount} Stück Y aus dem Lager des Postens; X geht
 * ins Lager.
 */
public final class TradingRules {

    private TradingRules() {
    }

    /** Ergebnis eines Tauschs. */
    public enum Trade { OK, NO_OFFER, NO_ITEMS, OUT_OF_STOCK, NO_SPACE }

    /** Wie oft das Lager das Angebot noch bedienen kann (Vorrat von Y geteilt durch die Menge je Tausch). */
    public static long available(long stockOfGet, int getCount) {
        return getCount <= 0 ? 0 : Math.max(0, stockOfGet) / getCount;
    }

    /**
     * Prüft einen Tausch: {@code playerHas} Stück X beim Händler, {@code stockOfGet} Stück Y im Lager, {@code fits} =
     * X passt nach dem Entnehmen von Y ins Lager.
     */
    public static Trade check(int giveCount, int getCount, long playerHas, long stockOfGet, boolean fits) {
        if (giveCount <= 0 || getCount <= 0) {
            return Trade.NO_OFFER;
        }
        if (playerHas < giveCount) {
            return Trade.NO_ITEMS;
        }
        if (stockOfGet < getCount) {
            return Trade.OUT_OF_STOCK;
        }
        return fits ? Trade.OK : Trade.NO_SPACE;
    }

    /**
     * Neues Muster eines Angebots-Slots nach einem Klick (GUI): links mit Item = Kopie des Stapels, rechts mit Item =
     * ein Stück mehr (anderes Item: 1 Stück), links ohne Item = leeren, rechts ohne Item = ein Stück weniger. Gibt die
     * neue Menge zurück (0 = leer); {@code same} = getragenes Item gleicht dem Muster.
     */
    public static int patternCount(int current, boolean right, int carried, boolean same, int maxStack) {
        int n;
        if (carried > 0) {
            n = !right ? carried : same ? current + 1 : 1;
        } else {
            n = right ? current - 1 : 0;
        }
        return Math.max(0, Math.min(maxStack, n));
    }
}
