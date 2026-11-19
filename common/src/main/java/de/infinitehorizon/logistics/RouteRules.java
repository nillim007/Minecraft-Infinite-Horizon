package de.infinitehorizon.logistics;

/**
 * Startbedingungen der Routen (20.4), reine Funktionen. Ohne passende Fracht startet keine Route.
 */
public final class RouteRules {

    /** Startbedingung; {@code id} wie in {@code logistics_computer.start_conditions}. */
    public enum Condition {
        WHEN_FULL("when_full"),
        EVERY_N_TICKS("every_n_ticks"),
        WHEN_TARGET_BELOW("when_target_below"),
        UNTIL_DEMAND_MET("until_shipyard_demand_met");

        private final String id;

        Condition(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Condition byId(String id) {
            for (Condition c : values()) {
                if (c.id.equals(id)) {
                    return c;
                }
            }
            return EVERY_N_TICKS;
        }
    }

    /**
     * Lage beim Prüfen.
     *
     * @param now             Overworld-Zeit
     * @param lastStart       letzter Start der Route
     * @param amount          N der Bedingung (Ticks bzw. Menge)
     * @param cargoStacks     passende Item-Stapel im Start-Terminal
     * @param rocketSlots     Item-Slots der Rakete
     * @param cargoFluidHolds passende gefüllte Frachttanks im Start-Terminal
     * @param rocketFluidHolds Fluid-Laderäume der Rakete
     * @param targetCount     Menge von X im Ziel
     * @param demand          offener Bedarf des Ziels für die Fracht der Route (Werft)
     */
    public record Context(long now, long lastStart, long amount, int cargoStacks, int rocketSlots, int cargoFluidHolds,
                          int rocketFluidHolds, long targetCount, long demand) {
    }

    private RouteRules() {
    }

    /** Startet die Route jetzt? */
    public static boolean shouldStart(Condition c, Context x) {
        if (x.cargoStacks() <= 0 && x.cargoFluidHolds() <= 0) {
            return false;
        }
        return switch (c) {
            // „wenn voll“: die Fracht füllt alle Item- oder alle Fluid-Laderäume der Rakete
            case WHEN_FULL -> x.rocketSlots() > 0 && x.cargoStacks() >= x.rocketSlots()
                    || x.rocketFluidHolds() > 0 && x.cargoFluidHolds() >= x.rocketFluidHolds();
            // eine Route, die noch nie gestartet ist (lastStart 0), startet sofort
            case EVERY_N_TICKS -> x.lastStart() <= 0 || x.now() - x.lastStart() >= Math.max(1, x.amount());
            case WHEN_TARGET_BELOW -> x.targetCount() < x.amount();
            // Abnahme 27: „bis der Bedarf gedeckt ist“ stoppt bei 0 Bedarf
            case UNTIL_DEMAND_MET -> x.demand() > 0;
        };
    }

    /**
     * Warnung beim Anlegen (20.3): Hin- plus Rückflug übersteigen den Tank und am Ziel ist kein Methan gelagert.
     */
    public static boolean fuelWarning(long legOut, long legBack, long tankCapacity, boolean targetHasFuel) {
        return tankCapacity > 0 && legOut + legBack > tankCapacity && !targetHasFuel;
    }
}
