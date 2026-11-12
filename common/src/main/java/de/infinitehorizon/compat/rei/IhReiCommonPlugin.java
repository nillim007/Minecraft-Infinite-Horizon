package de.infinitehorizon.compat.rei;

import de.infinitehorizon.InfiniteHorizon;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

/**
 * REI-Plugin für beide Seiten: meldet den Serializer der Maschinenanzeigen an (REI speichert Anzeigen, z. B. im Verlauf).
 * Fabric: Entry-Point {@code rei_common}; NeoForge: Unterklasse mit {@code @REIPluginCommon}.
 */
public class IhReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(InfiniteHorizon.id("machine_recipe"), MachineRecipeDisplay.SERIALIZER);
    }
}
