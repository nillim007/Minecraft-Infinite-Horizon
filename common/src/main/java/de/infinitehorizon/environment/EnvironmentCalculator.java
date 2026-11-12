package de.infinitehorizon.environment;

import de.infinitehorizon.data.Planet;

import java.util.EnumSet;
import java.util.Set;

/**
 * Umweltwerte als reine Funktion (8.1). Vorrang der Werte (25.1): Planet → {@code biome_temperature_overrides} → alle
 * passenden Zonen in Listenreihenfolge (spätere Nicht-null-Werte gewinnen) → aktive Kuppel.
 */
public final class EnvironmentCalculator {

    private EnvironmentCalculator() {
    }

    public static EnvironmentResult compute(EnvironmentInput in, EnvironmentRules rules) {
        Planet planet = in.planet();
        if (planet == null) {
            // ohne Planetendaten gilt die Erde ohne Gefahren (8.1); Temperatur in der Mitte des Schutzbereichs ohne Anzug
            double mid = (rules.noSuit().tempMin() + rules.noSuit().tempMax()) / 2;
            return result(true, mid, Pressure.NORMAL, rules.radiation().min(), 1.0, "normal", in.protection(), rules);
        }
        boolean breathable = planet.breathable();
        Pressure pressure = planet.pressure();
        int radiation = planet.radiation();
        double solar = planet.solarFactor();
        String water = planet.waterBehavior();

        double daylight = rules.daylight(in.dayTime());
        double temperature = planet.temperatureNight() + (planet.temperatureDay() - planet.temperatureNight()) * daylight;
        if (in.biome() != null) {
            temperature += planet.biomeTemperatureOffsets().getOrDefault(in.biome(), 0.0);
            Double fixed = planet.biomeTemperatureOverrides().get(in.biome());
            if (fixed != null) {
                temperature = fixed;
            }
        }
        for (Planet.Zone z : in.zones()) {
            if (z.temperature() != null) {
                temperature = z.temperature();
            }
            if (z.pressure() != null) {
                pressure = z.pressure();
            }
            if (z.radiation() != null) {
                radiation = z.radiation();
            }
            if (z.solarFactor() != null) {
                solar = z.solarFactor();
            }
            if (z.atmosphere() != null) {
                breathable = Planet.BREATHABLE.equals(z.atmosphere());
            }
            if (z.waterBehavior() != null) {
                water = z.waterBehavior();
            }
        }

        EnvironmentRules.RadiationRules r = rules.radiation();
        if (in.solarStorm()) {
            radiation += r.solarStormBonus();
        }
        if (in.coverBlocks() >= r.coverBlocks()) {
            radiation -= r.coverReduction();
        }

        EnvironmentInput.DomeCover dome = in.dome();
        if (dome != null) {
            breathable = true;
            pressure = Pressure.NORMAL;
            if (dome.temperature() != null) {
                temperature = dome.temperature();
            }
            if (dome.tier() >= 3) {
                radiation = r.domeT3Value();
            }
        }
        radiation = Math.clamp(radiation, r.min(), r.max());
        return result(breathable, temperature, pressure, radiation, solar, water, in.protection(), rules);
    }

    private static EnvironmentResult result(boolean breathable, double temperature, Pressure pressure, int radiation,
                                            double solar, String water, Protection p, EnvironmentRules rules) {
        Set<Hazard> hazards = EnumSet.noneOf(Hazard.class);
        if (!breathable && !p.suppliesOxygen()) {
            hazards.add(Hazard.OXYGEN);
        }
        if (pressure == Pressure.DEEP) {
            if (!p.deepPressure()) {
                hazards.add(Hazard.DEEP_PRESSURE);
            }
        } else if (!p.pressures().contains(pressure)) {
            hazards.add(Hazard.PRESSURE);
        }
        if (temperature > p.tempMax()) {
            hazards.add(Hazard.HEAT);
        }
        if (temperature < p.tempMin()) {
            hazards.add(Hazard.COLD);
        }
        if (radiation > p.radiationMax()) {
            hazards.add(Hazard.RADIATION);
        }
        return new EnvironmentResult(breathable, temperature, pressure, radiation, solar, water, hazards);
    }
}
