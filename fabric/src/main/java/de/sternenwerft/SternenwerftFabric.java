package de.sternenwerft;

import de.sternenwerft.platform.FabricEnergyHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.sternenwerft.data.GeneratedData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Locale;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;

public class SternenwerftFabric implements ModInitializer {

    /** Worldgen-Ergaenzungen (13.4) aus {@code sternenwerft/biome_features.json}; NeoForge nutzt Biome-Modifier-JSONs. */
    private static void addBiomeFeatures() {
        for (JsonElement e : GeneratedData.array("biome_features", "features")) {
            JsonObject o = e.getAsJsonObject();
            TagKey<Biome> biomes = TagKey.create(Registries.BIOME, Identifier.parse(o.get("biomes").getAsString()));
            ResourceKey<PlacedFeature> feature = ResourceKey.create(Registries.PLACED_FEATURE,
                    Identifier.parse(o.get("feature").getAsString()));
            GenerationStep.Decoration step = GenerationStep.Decoration.valueOf(o.get("step").getAsString().toUpperCase(Locale.ROOT));
            BiomeModifications.addFeature(BiomeSelectors.tag(biomes), step, feature);
        }
    }

    @Override
    public void onInitialize() {
        Sternenwerft.init();
        FabricEnergyHelper.registerTestConsumers();
        addBiomeFeatures();
        Sternenwerft.serverReloadListeners().forEach((id, listener) ->
                DataResourceLoader.get().registerReloadListener(id, listener));
        ServerLifecycleEvents.SERVER_STARTING.register(Sternenwerft::onServerStarting);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Sternenwerft.onPlayerJoin(handler.getPlayer()));
    }
}
