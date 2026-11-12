package de.infinitehorizon.registry;

import de.infinitehorizon.platform.Services;
import de.infinitehorizon.world.FluidSphereFeature;
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
