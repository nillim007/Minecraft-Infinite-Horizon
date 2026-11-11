package de.sternenwerft;

import de.sternenwerft.platform.FabricEnergyHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;

public class SternenwerftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Sternenwerft.init();
        FabricEnergyHelper.registerTestConsumers();
        Sternenwerft.serverReloadListeners().forEach((id, listener) ->
                DataResourceLoader.get().registerReloadListener(id, listener));
        ServerLifecycleEvents.SERVER_STARTING.register(Sternenwerft::onServerStarting);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Sternenwerft.onPlayerJoin(handler.getPlayer()));
    }
}
