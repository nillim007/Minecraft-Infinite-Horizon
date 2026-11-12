package de.infinitehorizon.api.energy;

/** Ein Empfänger, in den ein Netz oder Erzeuger Energie schiebt (Push-Prinzip 4.4). */
@FunctionalInterface
public interface EnergyTarget {

    /** Nimmt bis zu {@code amount} J auf und gibt die angenommene Menge zurück. */
    long insert(long amount, boolean simulate);
}
