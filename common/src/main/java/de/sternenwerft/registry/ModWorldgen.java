package de.sternenwerft.registry;

import de.sternenwerft.platform.Services;
import de.sternenwerft.world.FluidSphereFeature;
import net.minecraft.core.registries.BuiltInRegistries;

/** Eigene Worldgen-Typen (Feature-Codecs). Die Features selbst stehen als JSON unter worldgen/feature. */
public final class ModWorldgen {

    static {
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "fluid_sphere", () -> FluidSphereFeature.CODEC);
    }

    private ModWorldgen() {
    }

    public static void init() {
    }
}
