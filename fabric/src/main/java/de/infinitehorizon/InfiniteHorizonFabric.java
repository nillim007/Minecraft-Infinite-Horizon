package de.infinitehorizon;

import de.infinitehorizon.platform.FabricEnergyHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.GeneratedData;
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

public class InfiniteHorizonFabric implements ModInitializer {

    /** Worldgen-Ergaenzungen (13.4) aus {@code infinite_horizon/biome_features.json}; NeoForge nutzt Biome-Modifier-JSONs. */
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
        InfiniteHorizon.init();
        FabricEnergyHelper.registerTestConsumers();
        addBiomeFeatures();
        InfiniteHorizon.serverReloadListeners().forEach((id, listener) ->
                DataResourceLoader.get().registerReloadListener(id, listener));
        ServerLifecycleEvents.SERVER_STARTING.register(InfiniteHorizon::onServerStarting);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> InfiniteHorizon.onDatapackSync(player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(InfiniteHorizon::onServerTick);
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> InfiniteHorizon.onRespawn(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> InfiniteHorizon.onPlayerJoin(handler.getPlayer()));
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> InfiniteHorizon.onDimensionChange(player, destination.dimension()));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> InfiniteHorizon.registerCommands(dispatcher));
    }
}
