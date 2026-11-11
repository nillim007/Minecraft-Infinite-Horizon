package de.sternenwerft.registry;

import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

/** Eigene Data Components (3A.2). */
public final class ModDataComponents {

    /**
     * {@code sternenwerft:fluid} (4.1): Fluid und Menge in einem Behälter-Item bzw. einem abgebauten Tank.
     * Ein leerer Behälter hat die Komponente nicht.
     */
    public static final Supplier<DataComponentType<SwFluidStack>> FLUID = Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, "fluid",
            () -> DataComponentType.<SwFluidStack>builder()
                    .persistent(SwFluidStack.CODEC)
                    .networkSynchronized(SwFluidStack.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void init() {
    }
}
