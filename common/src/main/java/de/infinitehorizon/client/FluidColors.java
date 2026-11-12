package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.registry.ModFluids;
import net.minecraft.resources.Identifier;

/**
 * Platzhalterfarben für Fluide in GUIs und Rezeptanzeigen: Mod-Fluide aus {@code fluids.json} (Farbe aus items.json),
 * Vanilla-Wasser und -Lava fest, sonst grau.
 */
public final class FluidColors {

    // Platzhalterfarben der Vanilla-Fluide (GUI)
    private static final int WATER = 0xFF3F76E4;
    private static final int LAVA = 0xFFE05A10;
    private static final int OTHER = 0xFFA0A0A0;

    private FluidColors() {
    }

    /** Kennt diese Klasse eine eigene Farbe für das Fluid (Mod-Fluid, Wasser, Lava)? */
    public static boolean known(Identifier fluid) {
        return (fluid.getNamespace().equals(InfiniteHorizon.MOD_ID) && ModFluids.defs().containsKey(fluid.getPath()))
                || fluid.toString().equals("minecraft:water") || fluid.toString().equals("minecraft:lava");
    }

    public static int of(Identifier fluid) {
        if (fluid.getNamespace().equals(InfiniteHorizon.MOD_ID) && ModFluids.defs().containsKey(fluid.getPath())) {
            return ModFluids.defs().get(fluid.getPath()).color();
        }
        return switch (fluid.toString()) {
            case "minecraft:water" -> WATER;
            case "minecraft:lava" -> LAVA;
            default -> OTHER;
        };
    }
}
