package de.infinitehorizon.agency;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Agentur-Aktionen am Forschungsterminal (23.1, Reiter „Agentur“, {@code AgencyActionPayload}): Einladen (nur der
 * Leiter, nur Spieler online), Annehmen und Ablehnen (an jedem Terminal), Austreten. Der Server prüft alles; danach
 * bekommen alle Betroffenen ihren Agentur-Stand.
 */
public final class AgencyActions {

    public static final String INVITE = "invite";
    public static final String ACCEPT = "accept";
    public static final String DECLINE = "decline";
    public static final String LEAVE = "leave";

    private static final String MSG = "message." + InfiniteHorizon.MOD_ID + ".agency.";

    private AgencyActions() {
    }

    /** Führt eine Aktion aus; {@code target} = eingeladener Spieler bzw. einladende Agentur. */
    public static AgencyData.ActionResult perform(ServerPlayer player, String action, UUID target) {
        MinecraftServer server = player.level().getServer();
        AgencyData data = AgencyData.get(server);
        long tick = server.overworld().getGameTime();
        AgencyManager.ensureFor(player);
        Agency before = data.agencyOf(player.getUUID()).orElse(null);
        AgencyData.ActionResult r = switch (action) {
            case INVITE -> invite(server, data, player, target);
            case ACCEPT -> data.accept(player.getUUID(), target, tick);
            case DECLINE -> data.decline(player.getUUID(), target);
            case LEAVE -> data.leave(player.getUUID(), tick);
            default -> AgencyData.ActionResult.UNKNOWN_AGENCY;
        };
        if (r != AgencyData.ActionResult.OK) {
            player.sendOverlayMessage(Component.translatable(MSG + "denied." + r.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.RED));
            return r;
        }
        Agency after = data.agencyOf(player.getUUID()).orElse(null);
        switch (action) {
            case ACCEPT -> {
                if (after != null) {
                    Component msg = Component.translatable(MSG + "joined", player.getGameProfile().name(),
                            AgencyManager.displayName(server, after));
                    forEachOnline(server, after, sp -> sp.sendSystemMessage(msg));
                }
            }
            case LEAVE -> {
                player.sendSystemMessage(Component.translatable(MSG + "left", before == null ? Component.empty()
                        : AgencyManager.displayName(server, before)));
                // neue Agentur: aktuelle Dimension als besucht merken, Tore prüfen
                AgencyManager.onDimensionChange(player, player.level().dimension());
                ResearchService.onMemberMoved(player);
            }
            default -> {
            }
        }
        if (before != null) {
            ResearchService.syncAgency(server, before);
        }
        if (after != null && after != before) {
            ResearchService.syncAgency(server, after);
        }
        ResearchService.sendState(player);
        return r;
    }

    private static AgencyData.ActionResult invite(MinecraftServer server, AgencyData data, ServerPlayer leader, UUID target) {
        ServerPlayer invited = server.getPlayerList().getPlayer(target);
        if (invited == null) {
            return AgencyData.ActionResult.UNKNOWN_AGENCY;
        }
        AgencyData.ActionResult r = data.invite(leader.getUUID(), target);
        if (r == AgencyData.ActionResult.OK) {
            Agency a = data.agencyOf(leader.getUUID()).orElseThrow();
            invited.sendSystemMessage(Component.translatable(MSG + "invited", leader.getGameProfile().name(),
                    AgencyManager.displayName(server, a)).withStyle(ChatFormatting.AQUA));
            leader.sendOverlayMessage(Component.translatable(MSG + "invite_sent", invited.getGameProfile().name()));
            ResearchService.sendState(invited);
        }
        return r;
    }

    private static void forEachOnline(MinecraftServer server, Agency agency, java.util.function.Consumer<ServerPlayer> action) {
        for (Agency.Member m : agency.members()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(m.player());
            if (sp != null) {
                action.accept(sp);
            }
        }
    }
}
