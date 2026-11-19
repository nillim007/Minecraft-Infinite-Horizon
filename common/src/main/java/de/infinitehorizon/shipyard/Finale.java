package de.infinitehorizon.shipyard;

import com.google.gson.JsonObject;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyAdvancements;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.network.FinalePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.vehicle.StarshipService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finale (21.3): Nach Abschnitt 8 erscheint am Werft-Terminal „Start“, alle Mitglieder im Erdorbit werden
 * benachrichtigt. Der Start spielt für die Mitglieder im Erdorbit die Sequenz (besuchte Planeten, Antwort des Senders,
 * Abspann), vergibt den Erfolg an die ganze Agentur und setzt das Sternenschiff neben die Station.
 */
public final class Finale {

    /** Abstand des Sternenschiffs vor dem Terminal in Blickrichtung (Blöcke). */
    private static final int SHIP_DISTANCE = 8;

    private Finale() {
    }

    public static JsonObject rules() {
        return BalanceManager.get().entry("shipyard", "finale").orElse(new JsonObject());
    }

    public static int sequenceTicks() {
        JsonObject r = rules();
        return r.has("sequence_ticks") ? r.get("sequence_ticks").getAsInt() : 0;
    }

    /** Je Planet {@code sequence_ticks ÷ Anzahl} Ticks (reine Rechnung). */
    public static int perBodyTicks(int sequenceTicks, int bodies) {
        return bodies <= 0 ? sequenceTicks : sequenceTicks / bodies;
    }

    private static @Nullable Agency agency(MinecraftServer server, ShipyardState.Yard y, @Nullable ServerPlayer fallback) {
        AgencyData data = AgencyData.get(server);
        if (y.owner().isPresent()) {
            return data.byId(data.resolve(y.owner().get())).orElse(null);
        }
        return fallback == null ? null : data.agencyOf(fallback.getUUID()).orElse(null);
    }

    /** Mitglieder der Agentur, die gerade in der Dimension der Werft sind. */
    private static List<ServerPlayer> membersHere(ServerLevel level, @Nullable Agency a) {
        List<ServerPlayer> out = new ArrayList<>();
        if (a == null) {
            return out;
        }
        for (Agency.Member m : a.members()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(m.player());
            if (p != null && p.level() == level) {
                out.add(p);
            }
        }
        return out;
    }

    /** Abschnitt 8 fertig: Mitglieder im Erdorbit benachrichtigen (einmal). */
    public static ShipyardState.Yard onComplete(ServerLevel level, ShipyardState.Yard y) {
        if (!y.complete() || y.notified()) {
            return y;
        }
        for (ServerPlayer p : membersHere(level, agency(level.getServer(), y, null))) {
            p.sendSystemMessage(Component.translatable("message.infinite_horizon.shipyard.ready"));
        }
        return ShipyardState.get(level.getServer()).setFlags(y, true, y.launched());
    }

    /** Ergebnis des Starts. */
    public enum Start { OK, NOT_READY, ALREADY, DENIED }

    /** Start am Werft-Terminal (21.3, Schritt 2–4). */
    public static Start start(ServerPlayer player, BlockPos terminal) {
        ServerLevel level = (ServerLevel) player.level();
        ShipyardState data = ShipyardState.get(level.getServer());
        ShipyardState.Yard y = data.yard(level.dimension().identifier(), terminal);
        if (y == null || !y.complete() || !ShipyardService.terminalAllowed(level, terminal)) {
            return Start.NOT_READY;
        }
        if (y.launched()) {
            return Start.ALREADY;
        }
        if (!Permissions.can(player, y.owner().orElse(null), Permissions.Action.LAUNCH)) {
            return Start.DENIED;
        }
        y = data.setFlags(y, true, true);
        Agency a = agency(level.getServer(), y, player);
        UUID agencyId = a == null ? null : a.id();
        List<Identifier> visited = new ArrayList<>(a == null ? List.of() : a.visitedDimensions());
        FinalePayload payload = new FinalePayload(visited, sequenceTicks());
        List<ServerPlayer> watch = membersHere(level, a);
        if (!watch.contains(player)) {
            watch.add(player);
        }
        for (ServerPlayer p : watch) {
            Services.NETWORK.sendToPlayer(p, payload);
        }
        JsonObject r = rules();
        if (r.has("advancement")) {
            // 23.3 „Gemeinsame Werft“: der Besitzer und alle Agenturen, die mindestens 1 Item geliefert haben
            java.util.Set<UUID> award = new java.util.LinkedHashSet<>();
            if (agencyId != null) {
                award.add(agencyId);
            }
            AgencyData agencies = AgencyData.get(level.getServer());
            y.contributors().forEach(c -> award.add(agencies.resolve(c)));
            Identifier adv = Identifier.parse(r.get("advancement").getAsString());
            award.forEach(id -> AgencyAdvancements.awardToAgency(level.getServer(), id, adv));
        }
        Direction facing = level.getBlockState(terminal).hasProperty(MachineBlock.FACING)
                ? level.getBlockState(terminal).getValue(MachineBlock.FACING) : Direction.NORTH;
        StarshipService.spawn(level, terminal.relative(facing, SHIP_DISTANCE), agencyId);
        return Start.OK;
    }
}
