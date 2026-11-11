package de.sternenwerft.core.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/** Umrechnung Fluid-ID ↔ Registry-Nummer für die GUI-Synchronisation (0 = leer). */
public final class FluidIds {

    private FluidIds() {
    }

    public static long toNetworkId(@Nullable Identifier fluid) {
        if (fluid == null) {
            return 0;
        }
        Fluid f = BuiltInRegistries.FLUID.getValue(fluid);
        return f == null || f == Fluids.EMPTY ? 0 : BuiltInRegistries.FLUID.getId(f) + 1L;
    }

    public static @Nullable Identifier fromNetworkId(long id) {
        if (id <= 0) {
            return null;
        }
        Fluid f = BuiltInRegistries.FLUID.byId((int) (id - 1));
        return f == null ? null : BuiltInRegistries.FLUID.getKey(f);
    }

    /** Fluid-ID eines (Quell- oder Fließ-)Fluids, immer die der Quelle. */
    public static Identifier sourceId(Fluid fluid) {
        Fluid src = fluid instanceof net.minecraft.world.level.material.FlowingFluid ff ? ff.getSource() : fluid;
        return BuiltInRegistries.FLUID.getKey(src);
    }
}
