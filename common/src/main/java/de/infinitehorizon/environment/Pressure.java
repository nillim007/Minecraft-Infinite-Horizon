package de.infinitehorizon.environment;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/** Druckstufen (8.1, {@code pressure_levels} in environment.json). {@code DEEP} = Tiefseedruck. */
public enum Pressure {
    VACUUM, LOW, NORMAL, HIGH, DEEP;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable Pressure byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (Pressure p : values()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
