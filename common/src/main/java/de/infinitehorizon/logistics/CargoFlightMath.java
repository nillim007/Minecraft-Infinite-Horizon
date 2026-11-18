package de.infinitehorizon.logistics;

/**
 * Frachtflüge (20.3), reine Funktionen: Treibstoff je Strecke {@code base + per_delta_dp × |ΔDP|} (auf ganze mB
 * gerundet) und Flugzeit {@code (base_ticks + per_delta_dp_ticks × |ΔDP|) ÷ Divisor des besten Antriebs}.
 */
public final class CargoFlightMath {

    private CargoFlightMath() {
    }

    /** Treibstoff für eine Strecke (hin oder zurück) in mB. */
    public static long fuelPerLeg(long base, long perDeltaDp, double dpFrom, double dpTo) {
        return Math.round(base + perDeltaDp * Math.abs(dpFrom - dpTo));
    }

    /** Flugzeit einer Strecke in Ticks; {@code divisor} ≤ 1 heißt ohne Antrieb. */
    public static long flightTicks(long baseTicks, long perDeltaDpTicks, double dpFrom, double dpTo, double divisor) {
        double t = baseTicks + perDeltaDpTicks * Math.abs(dpFrom - dpTo);
        return Math.round(t / Math.max(1.0, divisor));
    }
}
