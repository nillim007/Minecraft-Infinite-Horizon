package de.infinitehorizon;

import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceReloadListener;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.network.ModPayloads;
import de.infinitehorizon.network.SyncConfigPayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlockEntities;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModCreativeTabs;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.registry.ModWorldgen;
import de.infinitehorizon.registry.ModMenus;
import de.infinitehorizon.registry.ModRecipeSerializers;
import de.infinitehorizon.registry.ModRecipeTypes;
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
public final class InfiniteHorizon {

    public static final String MOD_ID = "infinite_horizon";
    public static final String MOD_NAME = "InfiniteHorizon";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private InfiniteHorizon() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Registriert alles in fester Reihenfolge. */
    public static void init() {
        LOG.info("[InfiniteHorizon] Initialisierung auf {} ({})", Services.PLATFORM.getPlatformName(),
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

    /** Serverstart: Config aus {@code <welt>/serverconfig/infinite_horizon.json} laden (Pfad laut misc.json). */
    public static void onServerStarting(MinecraftServer server) {
        String file = BalanceManager.get().entry("misc", "config_defaults")
                .filter(o -> o.has("file")).map(o -> o.get("file").getAsString())
                .orElse("serverconfig/" + MOD_ID + ".json");
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(file);
        ServerConfig.load(path);
        LOG.info("[InfiniteHorizon] Config geladen: {}", path);
    }

    /** Join: Konfiguration an den Client senden. */
    /** Merkt am Spieler, dass er das Handbuch schon bekommen hat (Entity-Tags werden gespeichert). */
    public static final String HANDBOOK_GIVEN_TAG = MOD_ID + ".handbook_given";

    public static void onPlayerJoin(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, new SyncConfigPayload(ServerConfig.get().toJson()));
        giveHandbookOnce(player);
    }

    /** Ingenieurs-Handbuch beim ersten Join (24.1); genau einmal je Spieler. */
    public static void giveHandbookOnce(net.minecraft.world.entity.player.Player player) {
        if (player.entityTags().contains(HANDBOOK_GIVEN_TAG)) {
            return;
        }
        player.addTag(HANDBOOK_GIVEN_TAG);
        net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(
                de.infinitehorizon.registry.ModItems.ENGINEERS_HANDBOOK.get());
        // ins Inventar, bei vollem Inventar fällt es vor den Spieler
        if (!player.getInventory().add(book) && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            player.spawnAtLocation(sl, book);
        }
    }
}
