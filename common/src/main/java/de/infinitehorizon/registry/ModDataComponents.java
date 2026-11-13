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

    /** {@code infinite_horizon:rover_data} (16): Energie, O₂, Module und Fracht des abgebauten Rovers. */
    public static final Supplier<DataComponentType<de.infinitehorizon.vehicle.RoverData>> ROVER_DATA =
            Services.REGISTRY.register(BuiltInRegistries.DATA_COMPONENT_TYPE, "rover_data",
                    () -> DataComponentType.<de.infinitehorizon.vehicle.RoverData>builder()
                            .persistent(de.infinitehorizon.vehicle.RoverData.CODEC)
                            .networkSynchronized(de.infinitehorizon.vehicle.RoverData.STREAM_CODEC)
                            .build());

    /** {@code infinite_horizon:nutrition} (17): Nahrung und Sättigung der Weltraumration. */
    public static final Supplier<DataComponentType<de.infinitehorizon.food.Nutrition>> NUTRITION =
            Services.REGISTRY.register(BuiltInRegistries.DATA_COMPONENT_TYPE, "nutrition",
                    () -> DataComponentType.<de.infinitehorizon.food.Nutrition>builder()
                            .persistent(de.infinitehorizon.food.Nutrition.CODEC)
                            .networkSynchronized(de.infinitehorizon.food.Nutrition.STREAM_CODEC)
                            .build());

    /** {@code infinite_horizon:suit_modules} (9.2): eingebaute Module eines Anzugteils. */
    public static final Supplier<DataComponentType<java.util.List<net.minecraft.resources.Identifier>>> SUIT_MODULES =
            Services.REGISTRY.register(BuiltInRegistries.DATA_COMPONENT_TYPE, "suit_modules",
                    () -> DataComponentType.<java.util.List<net.minecraft.resources.Identifier>>builder()
                            .persistent(net.minecraft.resources.Identifier.CODEC.listOf())
                            .networkSynchronized(net.minecraft.resources.Identifier.STREAM_CODEC.apply(
                                    net.minecraft.network.codec.ByteBufCodecs.list()))
                            .build());

    /** {@code infinite_horizon:sample} (11.2): Block und Dimension einer Probe. */
    public static final Supplier<DataComponentType<de.infinitehorizon.research.SampleData>> SAMPLE = Services.REGISTRY.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, "sample",
            () -> DataComponentType.<de.infinitehorizon.research.SampleData>builder()
                    .persistent(de.infinitehorizon.research.SampleData.CODEC)
                    .networkSynchronized(de.infinitehorizon.research.SampleData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void init() {
    }
}
