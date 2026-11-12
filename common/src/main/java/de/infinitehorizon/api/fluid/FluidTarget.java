package de.infinitehorizon.api.fluid;

import de.infinitehorizon.core.fluid.IhFluidStack;

/** Ein Empfänger, in den ein Rohrnetz oder eine Ausgabeseite Fluid schiebt (Push-Prinzip 4.4). */
@FunctionalInterface
public interface FluidTarget {

    /** Nimmt bis zu {@code offer.amount()} mB auf und gibt die angenommene Menge zurück. */
    long insert(IhFluidStack offer, boolean simulate);
}
