package de.infinitehorizon;

import de.infinitehorizon.gametest.NeoForgeGameTests;
import de.infinitehorizon.platform.NeoForgeEnergyHelper;
import de.infinitehorizon.platform.NeoForgeFluidHelper;
import de.infinitehorizon.platform.NeoForgeNetworkHelper;
import de.infinitehorizon.platform.NeoForgeRegistryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(InfiniteHorizon.MOD_ID)
public class InfiniteHorizonNeoForge {

    public InfiniteHorizonNeoForge(IEventBus modBus) {
        InfiniteHorizon.init();
        NeoForgeRegistryHelper.registerAll(modBus);
        NeoForgeGameTests.FUNCTIONS.register(modBus);
        de.infinitehorizon.platform.NeoForgeAttachmentHelper.TYPES.register(modBus);

        modBus.addListener(NeoForgeEnergyHelper::registerCapabilities);
        modBus.addListener(NeoForgeFluidHelper::registerCapabilities);
        modBus.addListener(NeoForgeNetworkHelper::onRegisterPayloads);
        modBus.addListener(NeoForgeGameTests::onRegisterTests);

        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) ->
                InfiniteHorizon.serverReloadListeners().forEach(e::addListener));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> InfiniteHorizon.onServerStarting(e.getServer()));
        // Maschinenrezepte zum Client schicken (JEI/REI, 3.9); seit 1.21.2 kennt der Client sie sonst nicht
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) -> {
            e.sendRecipes(de.infinitehorizon.compat.ClientRecipes.types());
            e.getRelevantPlayers().forEach(InfiniteHorizon::onDatapackSync);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) ->
                InfiniteHorizon.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                InfiniteHorizon.onRespawn(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                InfiniteHorizon.onPlayerJoin(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                InfiniteHorizon.onDimensionChange(player, e.getTo());
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
                InfiniteHorizon.registerCommands(e.getDispatcher()));
    }
}
