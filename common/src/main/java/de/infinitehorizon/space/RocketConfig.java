package de.infinitehorizon.space;

import de.infinitehorizon.data.RocketPartRegistry;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Zusammengesetzte Rakete (12.4) als Wertobjekt: Teile von unten nach oben (ohne Flossen), Stufe, Masse, Schub,
 * Treibstoff je Komponente.
 *
 * @param stack            Teile von unten (Triebwerk) nach oben (Kapsel)
 * @param fins             Zahl der Flossen
 * @param dryMass          Masse aller Teile ohne Treibstoff und Fracht (t)
 * @param fuelMass         Treibstoffmasse (t), linear mit dem Füllstand bis {@code fuel_mass_full}
 * @param cargoMass        Frachtmasse (t), {@code mass_per_slot} je belegtem Slot
 * @param fuelIds          Treibstoffkomponenten der Stufe (T1: Kerosin, Sauerstoff)
 * @param fuel             Treibstoff je Komponente (mB)
 * @param capacity         Fassungsvermögen je Komponente (mB)
 */
public record RocketConfig(int tier, List<RocketPartRegistry.Part> stack, int fins, double dryMass, double fuelMass,
                           double cargoMass, int thrust, List<Identifier> fuelIds, long[] fuel, long capacity, int seats) {

    /** Gesamtmasse in t. */
    public double mass() {
        return dryMass + fuelMass + cargoMass;
    }

    /** Schub / (Masse × g); bei g = 0 unendlich. */
    public double thrustRatio(double gravity) {
        return gravity <= 0 ? Double.POSITIVE_INFINITY : thrust / (mass() * gravity);
    }

    /** Kleinster Vorrat aller Komponenten (mB); der Flug braucht von jeder gleich viel. */
    public long usableFuel() {
        long min = Long.MAX_VALUE;
        for (long f : fuel) {
            min = Math.min(min, f);
        }
        return fuel.length == 0 ? 0 : min;
    }

    /** Höhe in Blöcken (gestapelte Teile ohne Flossen), zugleich die nötige Turmhöhe (4.3). */
    public int height() {
        return stack.size();
    }
}
