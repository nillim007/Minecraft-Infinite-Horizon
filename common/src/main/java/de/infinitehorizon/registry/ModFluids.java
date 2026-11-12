package de.infinitehorizon.registry;

import com.google.gson.JsonElement;
import de.infinitehorizon.core.fluid.FluidDef;
import de.infinitehorizon.data.GeneratedData;
import de.infinitehorizon.platform.Services;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod-Fluide aus {@code infinite_horizon/fluids.json}. Die Registrierung (Fluid, Block, Eimer; NeoForge zusätzlich
 * FluidType) übernimmt der Loader über {@code IFluidHelper#registerFluid}.
 */
public final class ModFluids {

    private static final Map<String, FluidDef> DEFS = new LinkedHashMap<>();
    private static final Map<String, FluidDef.Handle> HANDLES = new LinkedHashMap<>();

    static {
        for (JsonElement e : GeneratedData.array("fluids", "fluids")) {
            FluidDef def = FluidDef.fromJson(e.getAsJsonObject());
            DEFS.put(def.id(), def);
            FluidDef.Handle h = Services.FLUID.registerFluid(def);
            HANDLES.put(def.id(), h);
            if (h.bucket() != null) {
                ModItems.track(h.bucket());
            }
        }
    }

    private ModFluids() {
    }

    public static void init() {
    }

    /** Registrierte Teile eines Fluids nach Pfad (z. B. {@code "crude_oil"}). */
    public static FluidDef.Handle get(String path) {
        FluidDef.Handle h = HANDLES.get(path);
        if (h == null) {
            throw new IllegalArgumentException("Fluid nicht registriert: " + path);
        }
        return h;
    }

    public static Map<String, FluidDef> defs() {
        return Collections.unmodifiableMap(DEFS);
    }

    public static Map<String, FluidDef.Handle> handles() {
        return Collections.unmodifiableMap(HANDLES);
    }
}
