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
    /** Rolle des Ziels wechseln: Ingenieur ↔ Gast (nur Leiter, ab M9). */
    public static final String ROLE = "role";
    /** Leitung an das Ziel übergeben (ab M9). */
    public static final String LEADER = "leader";
    /** Partnerschaft anbieten oder bestätigen ({@code target} = Agentur, 23.3). */
    public static final String PARTNER = "partner";
    /** Partnerschaft beenden, Anfrage zurückziehen oder ablehnen ({@code target} = Agentur). */
    public static final String UNPARTNER = "unpartner";

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
        boolean wasPartner = before != null && before.relations().isPartner(target);
        AgencyData.ActionResult r = switch (action) {
            case INVITE -> invite(server, data, player, target);
            case ACCEPT -> data.accept(player.getUUID(), target, tick);
            case DECLINE -> data.decline(player.getUUID(), target);
            case LEAVE -> data.leave(player.getUUID(), tick);
            case ROLE -> {
                Role current = before == null ? null : before.roleOf(target).orElse(null);
                yield data.setRole(player.getUUID(), target, current == Role.GUEST ? Role.ENGINEER : Role.GUEST);
            }
            case LEADER -> data.transferLeader(player.getUUID(), target);
            case PARTNER -> data.requestPartner(player.getUUID(), target);
            case UNPARTNER -> data.endPartner(player.getUUID(), target);
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
            case ROLE, LEADER -> {
                ServerPlayer t = server.getPlayerList().getPlayer(target);
                Role now = after == null ? null : after.roleOf(target).orElse(null);
                if (t != null && now != null) {
                    t.sendSystemMessage(Component.translatable(MSG + "role_changed",
                            Component.translatable("gui.infinite_horizon.agency.role." + now.getSerializedName())));
                }
            }
            case PARTNER, UNPARTNER -> partnerMessages(server, data, player, action, before, target, wasPartner);
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
        if (action.equals(PARTNER) || action.equals(UNPARTNER)) {
            data.byId(target).ifPresent(other -> ResearchService.syncAgency(server, other));
        }
        ResearchService.sendState(player);
        return r;
    }

    /** Umbenennen (23.1, ab M9); alle Mitglieder online erfahren den neuen Namen. */
    public static AgencyData.ActionResult rename(ServerPlayer player, String name) {
        MinecraftServer server = player.level().getServer();
        AgencyData data = AgencyData.get(server);
        AgencyData.ActionResult r = data.rename(player.getUUID(), name);
        if (r != AgencyData.ActionResult.OK) {
            player.sendOverlayMessage(Component.translatable(MSG + "denied." + r.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.RED));
            return r;
        }
        Agency a = data.agencyOf(player.getUUID()).orElse(null);
        if (a != null) {
            Component msg = Component.translatable(MSG + "renamed", AgencyManager.displayName(server, a));
            forEachOnline(server, a, sp -> sp.sendSystemMessage(msg));
            ResearchService.syncAgency(server, a);
        }
        ResearchService.sendState(player);
        return r;
    }

    /** Nachrichten zu Partnerschaften: Anfrage an die andere Agentur, Bestätigung bzw. Ende an beide. */
    private static void partnerMessages(MinecraftServer server, AgencyData data, ServerPlayer player, String action,
                                        @org.jspecify.annotations.Nullable Agency own, UUID target, boolean wasPartner) {
        Agency other = data.byId(target).orElse(null);
        if (own == null || other == null) {
            return;
        }
        Component ownName = AgencyManager.displayName(server, own);
        Component otherName = AgencyManager.displayName(server, other);
        if (action.equals(PARTNER) && !own.relations().isPartner(other.id())) {
            forEachOnline(server, other, sp -> sp.sendSystemMessage(Component.translatable(MSG + "partner_request", ownName)
                    .withStyle(ChatFormatting.AQUA)));
            player.sendOverlayMessage(Component.translatable(MSG + "partner_sent", otherName));
            return;
        }
        if (action.equals(UNPARTNER) && !wasPartner) {
            player.sendOverlayMessage(Component.translatable(MSG + "partner_withdrawn", otherName));
            return;
        }
        String key = action.equals(PARTNER) ? "partner_confirmed" : "partner_ended";
        forEachOnline(server, own, sp -> sp.sendSystemMessage(Component.translatable(MSG + key, otherName)));
        forEachOnline(server, other, sp -> sp.sendSystemMessage(Component.translatable(MSG + key, ownName)));
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
