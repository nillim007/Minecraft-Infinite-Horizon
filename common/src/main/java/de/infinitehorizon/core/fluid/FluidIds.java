package de.infinitehorizon.core.fluid;

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

    /** Ist das Fluid ein Gas (5.3, {@code fluid_type} in items.json)? Fremde Fluide gelten als Flüssigkeit. */
    public static boolean isGas(@Nullable Identifier fluid) {
        if (fluid == null || !fluid.getNamespace().equals(de.infinitehorizon.InfiniteHorizon.MOD_ID)) {
            return false;
        }
        FluidDef def = de.infinitehorizon.registry.ModFluids.defs().get(fluid.getPath());
        return def != null && def.gas();
    }

    /**
     * Passt das Fluid zum Feld {@code accepts} eines Speichers (storage.json: {@code liquid}, {@code gas}, sonst alle)?
     * So nimmt der Tank nur Flüssigkeiten, die Druckflasche nur Gase (6.3).
     */
    public static boolean matchesAccepts(@Nullable String accepts, Identifier fluid) {
        if ("liquid".equals(accepts)) {
            return !isGas(fluid);
        }
        if ("gas".equals(accepts)) {
            return isGas(fluid);
        }
        return true;
    }

    /** Fluid-ID eines (Quell- oder Fließ-)Fluids, immer die der Quelle. */
    public static Identifier sourceId(Fluid fluid) {
        Fluid src = fluid instanceof net.minecraft.world.level.material.FlowingFluid ff ? ff.getSource() : fluid;
        return BuiltInRegistries.FLUID.getKey(src);
    }
}
