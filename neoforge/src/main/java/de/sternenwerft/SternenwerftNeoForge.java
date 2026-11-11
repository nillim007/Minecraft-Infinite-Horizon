package de.sternenwerft;

import de.sternenwerft.gametest.NeoForgeGameTests;
import de.sternenwerft.platform.NeoForgeEnergyHelper;
import de.sternenwerft.platform.NeoForgeFluidHelper;
import de.sternenwerft.platform.NeoForgeNetworkHelper;
import de.sternenwerft.platform.NeoForgeRegistryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Sternenwerft.MOD_ID)
public class SternenwerftNeoForge {

    public SternenwerftNeoForge(IEventBus modBus) {
        Sternenwerft.init();
        NeoForgeRegistryHelper.registerAll(modBus);
        NeoForgeGameTests.FUNCTIONS.register(modBus);

        modBus.addListener(NeoForgeEnergyHelper::registerCapabilities);
        modBus.addListener(NeoForgeFluidHelper::registerCapabilities);
        modBus.addListener(NeoForgeNetworkHelper::onRegisterPayloads);
        modBus.addListener(NeoForgeGameTests::onRegisterTests);

        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) ->
                Sternenwerft.serverReloadListeners().forEach(e::addListener));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> Sternenwerft.onServerStarting(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                Sternenwerft.onPlayerJoin(player);
            }
        });
    }
}
