package de.sternenwerft;

import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.BalanceReloadListener;
import de.sternenwerft.data.ServerConfig;
import de.sternenwerft.network.ModPayloads;
import de.sternenwerft.network.SyncConfigPayload;
import de.sternenwerft.platform.Services;
import de.sternenwerft.registry.ModBlockEntities;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModCreativeTabs;
import de.sternenwerft.registry.ModDataComponents;
import de.sternenwerft.registry.ModFluids;
import de.sternenwerft.registry.ModItems;
import de.sternenwerft.registry.ModWorldgen;
import de.sternenwerft.registry.ModMenus;
import de.sternenwerft.registry.ModRecipeSerializers;
import de.sternenwerft.registry.ModRecipeTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

/**
 * Einstieg der Mod im gemeinsamen Code (3A.2). Die Loader rufen {@link #init()} auf und leiten
 * Serverstart, Join und Reload-Listener an die Hooks unten weiter.
 */
public final class Sternenwerft {

    public static final String MOD_ID = "sternenwerft";
    public static final String MOD_NAME = "Sternenwerft";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Sternenwerft() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Registriert alles in fester Reihenfolge. */
    public static void init() {
        LOG.info("[Sternenwerft] Initialisierung auf {} ({})", Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName());
        ModDataComponents.init();
        ModBlocks.init();
        ModItems.init();
        ModFluids.init();
        ModWorldgen.init();
        ModBlockEntities.init();
        ModRecipeTypes.init();
        ModRecipeSerializers.init();
        ModMenus.init();
        ModCreativeTabs.init();
        ModPayloads.init();
    }

    /** Server-Reload-Listener (Datapack-Daten). */
    public static Map<Identifier, PreparableReloadListener> serverReloadListeners() {
        return Map.of(BalanceReloadListener.ID, new BalanceReloadListener());
    }

    /** Serverstart: Config aus {@code <welt>/serverconfig/sternenwerft.json} laden (Pfad laut misc.json). */
    public static void onServerStarting(MinecraftServer server) {
        String file = BalanceManager.get().entry("misc", "config_defaults")
                .filter(o -> o.has("file")).map(o -> o.get("file").getAsString())
                .orElse("serverconfig/" + MOD_ID + ".json");
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(file);
        ServerConfig.load(path);
        LOG.info("[Sternenwerft] Config geladen: {}", path);
    }

    /** Join: Konfiguration an den Client senden. */
    public static void onPlayerJoin(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, new SyncConfigPayload(ServerConfig.get().toJson()));
    }
}
