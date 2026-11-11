package de.sternenwerft.api.fluid;

import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Eigene Block-Entity mit Fluidtanks (Tanks, Pumpe, später Maschinen mit Fluiden). Rohre verbinden sich mit allen
 * Seiten, an denen {@link #fluidInput(Direction)} oder {@link #providesFluidTo(Direction)} etwas liefert.
 */
public interface FluidNode {

    /** Alle Tanks des Blocks (Reihenfolge = „Tank 1 … n“ im GUI). */
    List<SwFluidTank> fluidTanks();

    /** Empfänger für Fluid, das über {@code side} hereinkommt; null = Seite nimmt nichts an. */
    @Nullable FluidTarget fluidInput(@Nullable Direction side);

    /**
     * Entnimmt über {@code side} bis zu {@code max} mB aus den Ausgangstanks ({@code fluid} null = beliebig).
     * Nur an Seiten, an denen {@link #providesFluidTo(Direction)} gilt.
     */
    SwFluidStack extractFluid(@Nullable Direction side, @Nullable Identifier fluid, long max, boolean simulate);

    /** Gibt der Block über diese Seite Fluid ab (Ausgangsseite, 4.1)? */
    boolean providesFluidTo(@Nullable Direction side);

    /** Nach einer bestätigten Änderung von außen (Speichern markieren). */
    void onFluidChanged();

    /** Ablage für das Adapter-Objekt des Loaders. */
    Object platformFluidHandle(Function<FluidNode, Object> factory);
}
