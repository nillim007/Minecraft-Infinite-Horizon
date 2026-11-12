package de.infinitehorizon.agency;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;
import java.util.UUID;

/**
 * Zugang zu den Agenturen der laufenden Welt (3A.2): Login, Dimensionswechsel, Anzeigename. Die Daten selbst liegen in
 * {@link AgencyData}.
 */
public final class AgencyManager {

    public static final String DEFAULT_NAME_KEY = "agency.infinite_horizon.default_name";

    private AgencyManager() {
    }

    public static AgencyData data(MinecraftServer server) {
        return AgencyData.get(server);
    }

    /** Agentur des Spielers, bei Bedarf neu (23.1). Beitritts-Tick = Spielzeit der Overworld. */
    public static Agency ensureFor(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return data(server).ensureFor(player.getUUID(), server.overworld().getGameTime());
    }

    /** Login: Agentur sicherstellen und die aktuelle Dimension als besucht merken. */
    public static void onLogin(ServerPlayer player) {
        ensureFor(player);
        onDimensionChange(player, player.level().dimension());
    }

    /** Dimensionswechsel (auch per Befehl): Dimension für die Agentur als besucht merken (23.1). */
    public static void onDimensionChange(ServerPlayer player, ResourceKey<Level> dimension) {
        ensureFor(player);
        data(player.level().getServer()).visit(player.getUUID(), dimension.identifier());
    }

    /** Anzeigename: eigener Name oder „Agentur von &lt;Gründer&gt;“ ({@value #DEFAULT_NAME_KEY}). */
    public static Component displayName(MinecraftServer server, Agency agency) {
        if (agency.name() != null) {
            return Component.literal(agency.name());
        }
        String founder = agency.founder().map(m -> playerName(server, m.player())).orElse("?");
        return Component.translatable(DEFAULT_NAME_KEY, founder);
    }

    /** Name eines Spielers: online, sonst aus dem Namens-Cache des Servers, sonst die UUID. */
    public static String playerName(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return online.getGameProfile().name();
        }
        Optional<NameAndId> cached = server.services().nameToIdCache().get(player);
        return cached.map(NameAndId::name).orElse(player.toString());
    }

    /** Dimension als Identifier (für Tore ab M4). */
    public static boolean visited(MinecraftServer server, UUID agency, Identifier dimension) {
        return data(server).byId(agency).map(a -> a.visitedDimensions().contains(dimension)).orElse(false);
    }
}
