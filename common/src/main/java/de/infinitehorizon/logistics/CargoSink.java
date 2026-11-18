package de.infinitehorizon.logistics;

import de.infinitehorizon.core.fluid.IhFluidStack;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Ziel für Fracht (20.1): nimmt Items und Fluide an, z. B. das Frachtterminal. */
public interface CargoSink {

    /** Legt {@code stack} ab und gibt den Rest zurück; {@code simulate} ändert nichts. */
    ItemStack insertItem(ItemStack stack, boolean simulate);

    /** Nimmt bis zu {@code offer.amount()} mB an und gibt die Menge zurück; {@code simulate} ändert nichts. */
    long insertFluid(IhFluidStack offer, boolean simulate);

    /** Treibstofftank, aus dem Frachtraketen vor dem Rückflug tanken (20.3), oder null. */
    default de.infinitehorizon.core.fluid.@org.jspecify.annotations.Nullable IhFluidTank fuelTank() {
        return null;
    }

    /** Nimmt das Ziel Fluidfracht an? (Handelsposten nicht, 20.3) */
    default boolean acceptsFluidCargo() {
        return true;
    }

    /** Passen alle Items und Fluide eines Vorgangs (jeweils einzeln geprüft)? */
    default boolean fits(List<ItemStack> items, List<IhFluidStack> fluids) {
        for (ItemStack s : items) {
            if (!insertItem(s, true).isEmpty()) {
                return false;
            }
        }
        for (IhFluidStack f : fluids) {
            if (insertFluid(f, true) < f.amount()) {
                return false;
            }
        }
        return true;
    }
}
