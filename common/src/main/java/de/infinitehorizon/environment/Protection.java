package de.infinitehorizon.environment;

import java.util.Set;

/**
 * Was die Ausrüstung eines Spielers verträgt (8.3, 9.1): Temperaturbereich (inklusiv), Druckstufen, höchste
 * Strahlung, ob sie Sauerstoff liefert (vollständiger Anzug mit O₂ &gt; 0) und ob sie gegen Tiefseedruck schützt.
 * Ohne Anzug gilt {@link EnvironmentRules#noSuit()}; die Anzüge kommen mit M3d.
 */
public record Protection(double tempMin, double tempMax, Set<Pressure> pressures, int radiationMax,
                         boolean suppliesOxygen, boolean deepPressure) {

    public Protection {
        pressures = Set.copyOf(pressures);
    }
}
