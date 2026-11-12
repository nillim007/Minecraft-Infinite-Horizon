package de.infinitehorizon.environment;

import java.util.Set;

/**
 * Ergebnis von {@link EnvironmentCalculator#compute} (8.1): Umweltwerte an der Position und die Gefahren, gegen die
 * die Ausrüstung nicht schützt.
 */
public record EnvironmentResult(boolean breathable, double temperature, Pressure pressure, int radiation,
                                double solarFactor, String waterBehavior, Set<Hazard> hazards) {

    public EnvironmentResult {
        hazards = Set.copyOf(hazards);
    }

    public boolean has(Hazard hazard) {
        return hazards.contains(hazard);
    }
}
