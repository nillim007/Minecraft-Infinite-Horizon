package de.infinitehorizon.registry;

import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

/** Eigene Data Components (3A.2). */
public final class ModDataComponents {

    /**
     * {@code infinite_horizon:fluid} (4.1): Fluid und Menge in einem Behälter-Item bzw. einem abgebauten Tank.
     * Ein leerer Behälter hat die Komponente nicht.
     */
    public static final Supplier<DataComponentType<IhFluidStack>> FLUID = Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, "fluid",
            () -> DataComponentType.<IhFluidStack>builder()
                    .persistent(IhFluidStack.CODEC)
                    .networkSynchronized(IhFluidStack.STREAM_CODEC)
                    .build());

    /**
     * {@code infinite_horizon:energy} (4.1): gespeicherte Energie in J, z. B. auf einem abgebauten Akku. Ein leerer Speicher
     * hat die Komponente nicht.
     */
    public static final Supplier<DataComponentType<Long>> ENERGY = Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, "energy",
            () -> DataComponentType.<Long>builder()
                    .persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG)
                    .build());

    /**
     * {@code infinite_horizon:oxygen} (9.1): O₂-Vorrat einer Anzug-Brustplatte in mB. Eine neue Brustplatte hat die
     * Komponente nicht (0 mB).
     */
    public static final Supplier<DataComponentType<Integer>> OXYGEN = Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, "oxygen",
            () -> DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }

    public static void init() {
    }
}
