package de.infinitehorizon.environment;

import java.util.Locale;

/**
 * Umweltgefahren (8.2). Jede hat eine eigene Belastung; die Werte (Aufbau, Abbau, Schaden) stehen in
 * environment.json unter dem {@link #id()}. {@code DEEP_PRESSURE} (Tiefseedruck, 9.1) nutzt die Schadensart von Druck.
 */
public enum Hazard {
    OXYGEN, PRESSURE, HEAT, COLD, RADIATION, DEEP_PRESSURE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Hazard byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
