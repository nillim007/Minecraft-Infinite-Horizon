package de.sternenwerft.compat.rei;

import de.sternenwerft.Sternenwerft;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

/**
 * REI-Plugin für beide Seiten: meldet den Serializer der Maschinenanzeigen an (REI speichert Anzeigen, z. B. im Verlauf).
 * Fabric: Entry-Point {@code rei_common}; NeoForge: Unterklasse mit {@code @REIPluginCommon}.
 */
public class SwReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(Sternenwerft.id("machine_recipe"), MachineRecipeDisplay.SERIALIZER);
    }
}
