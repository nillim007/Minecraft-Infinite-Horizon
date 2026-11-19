package de.infinitehorizon.logistics;

import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.network.LogisticsEditPayload;
import de.infinitehorizon.network.LogisticsStatePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Logistik-Computer (20.4): baut den Zustand für das GUI (Terminals der Agentur, Routen, Raketenstatus, Warnungen) und
 * übernimmt Änderungen an Routen nach Prüfung (Abstand, Rechte, gültige Terminals und Hangar-Slots).
 */
public final class LogisticsService {

    /** Höchstabstand Spieler ↔ Logistik-Computer für Änderungen (3A.5). */
    public static final double MAX_DISTANCE = 8.0;
    /** Höchstzahl der Einträge im Fracht-Filter. */
    public static final int MAX_FILTER = 27;

    private LogisticsService() {
    }

    public static UUID agencyOf(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return AgencyData.get(server).ensureFor(player.getUUID(), server.overworld().getGameTime()).id();
    }

    private static @Nullable ServerLevel level(MinecraftServer server, Identifier dimension) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    /** Terminals der Agentur (oder ohne Besitzer) aus dem Außenposten-Register. */
    public static List<OutpostData.Terminal> terminalsOf(MinecraftServer server, UUID agency) {
        return own(server, agency, OutpostData.get(server).terminals());
    }

    /** Werft-Docks der Agentur (21.1): nur Ziel, nie Start. */
    public static List<OutpostData.Terminal> docksOf(MinecraftServer server, UUID agency) {
        return own(server, agency, OutpostData.get(server).docks());
    }

    /** Handelsposten der Agentur (23.3): nur Ziel. */
    public static List<OutpostData.Terminal> tradingPostsOf(MinecraftServer server, UUID agency) {
        return own(server, agency, OutpostData.get(server).tradingPosts());
    }

    /** Ziele bei Partnern (23.3): freigegebene Terminals und Werft-Docks, alle Handelsposten. */
    public static List<OutpostData.Terminal> partnerTargets(MinecraftServer server, UUID agency, List<OutpostData.Terminal> all) {
        AgencyData agencies = AgencyData.get(server);
        List<OutpostData.Terminal> out = new ArrayList<>();
        for (OutpostData.Terminal t : all) {
            if (t.owner().isPresent() && t.shared() && agencies.arePartners(agency, t.owner().get())) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Darf eine Rakete der Agentur {@code agency} an {@code pos} liefern (20.4, 23.3)? Eigene Ziele und Ziele ohne
     * Besitzer immer; fremde nur, wenn sie freigegeben sind (Handelsposten immer) und die Agenturen Partner sind. Ein
     * Ziel, das (noch) nicht im Register steht, gilt als erlaubt; {@code agency} null (Terminal ohne Besitzer) ebenso.
     */
    public static boolean mayDeliver(MinecraftServer server, @Nullable UUID agency, Identifier dimension, BlockPos pos) {
        OutpostData.Terminal t = OutpostData.get(server).entry(dimension, pos);
        if (agency == null || t == null || t.owner().isEmpty()) {
            return true;
        }
        AgencyData agencies = AgencyData.get(server);
        UUID owner = agencies.resolve(t.owner().get());
        return owner.equals(agencies.resolve(agency)) || t.shared() && agencies.arePartners(agency, owner);
    }

    /** Besitzer-Agentur eines eingetragenen Terminals (aufgelöst) oder null. */
    public static @Nullable UUID ownerOf(MinecraftServer server, Identifier dimension, BlockPos pos) {
        OutpostData.Terminal t = OutpostData.get(server).entry(dimension, pos);
        return t == null || t.owner().isEmpty() ? null : AgencyData.get(server).resolve(t.owner().get());
    }

    private static List<OutpostData.Terminal> own(MinecraftServer server, UUID agency, List<OutpostData.Terminal> all) {
        AgencyData agencies = AgencyData.get(server);
        List<OutpostData.Terminal> out = new ArrayList<>();
        for (OutpostData.Terminal t : all) {
            if (t.owner().isEmpty() || agencies.resolve(t.owner().get()).equals(agency)) {
                out.add(t);
            }
        }
        return out;
    }

    private static @Nullable CargoTerminalBlockEntity load(MinecraftServer server, Identifier dimension, BlockPos pos) {
        ServerLevel level = level(server, dimension);
        if (level == null) {
            return null;
        }
        level.getChunkAt(pos);
        return level.getBlockEntity(pos) instanceof CargoTerminalBlockEntity t ? t : null;
    }

    public static LogisticsStatePayload state(ServerPlayer player, BlockPos computer, String message) {
        MinecraftServer server = player.level().getServer();
        UUID agency = agencyOf(player);
        List<LogisticsStatePayload.Terminal> terminals = new ArrayList<>();
        for (OutpostData.Terminal t : terminalsOf(server, agency)) {
            CargoTerminalBlockEntity be = load(server, t.dimension(), t.pos());
            if (be == null) {
                continue;
            }
            List<String> hangar = new ArrayList<>();
            for (int i = 0; i < be.hangarSlots(); i++) {
                ItemStack s = be.hangar(i);
                hangar.add(s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            }
            terminals.add(new LogisticsStatePayload.Terminal(t.dimension(), t.pos(), hangar,
                    LogisticsStatePayload.Terminal.TERMINAL));
        }
        for (OutpostData.Terminal d : docksOf(server, agency)) {
            ServerLevel dl = level(server, d.dimension());
            if (dl != null) {
                dl.getChunkAt(d.pos());
                if (dl.getBlockEntity(d.pos()) instanceof de.infinitehorizon.shipyard.ShipyardDockBlockEntity) {
                    terminals.add(new LogisticsStatePayload.Terminal(d.dimension(), d.pos(), List.of(),
                            LogisticsStatePayload.Terminal.DOCK));
                }
            }
        }
        // Handelsposten (eigene) und Partner-Ziele (23.3) aus dem Register, ohne Chunks zu laden
        OutpostData outposts = OutpostData.get(server);
        tradingPostsOf(server, agency).forEach(t -> terminals.add(new LogisticsStatePayload.Terminal(t.dimension(), t.pos(),
                List.of(), LogisticsStatePayload.Terminal.TRADING_POST)));
        String p = LogisticsStatePayload.Terminal.PARTNER;
        partnerTargets(server, agency, outposts.terminals()).forEach(t -> terminals.add(new LogisticsStatePayload.Terminal(
                t.dimension(), t.pos(), List.of(), p + LogisticsStatePayload.Terminal.TERMINAL)));
        partnerTargets(server, agency, outposts.docks()).forEach(t -> terminals.add(new LogisticsStatePayload.Terminal(
                t.dimension(), t.pos(), List.of(), p + LogisticsStatePayload.Terminal.DOCK)));
        partnerTargets(server, agency, outposts.tradingPosts()).forEach(t -> terminals.add(new LogisticsStatePayload.Terminal(
                t.dimension(), t.pos(), List.of(), p + LogisticsStatePayload.Terminal.TRADING_POST)));
        CargoFlights data = CargoFlights.get(server);
        List<Route> routes = data.routesOf(agency);
        List<CargoFlights.RocketStatus> statuses = new ArrayList<>();
        List<UUID> warnings = new ArrayList<>();
        for (Route r : routes) {
            for (int slot : r.rockets()) {
                statuses.add(CargoFlights.status(server, r, slot));
            }
            if (fuelWarning(server, r)) {
                warnings.add(r.id());
            }
        }
        return new LogisticsStatePayload(computer, terminals, routes, statuses, warnings, message);
    }

    /** 20.3: Warnung, wenn Hin- plus Rückflug den Tank übersteigen und am Ziel kein Methan lagert. */
    public static boolean fuelWarning(MinecraftServer server, Route r) {
        CargoTerminalBlockEntity home = load(server, r.homeDimension(), r.home());
        if (home == null) {
            return false;
        }
        for (int slot : r.rockets()) {
            ItemStack rocket = home.hangar(slot);
            var tank = CargoRockets.fuelTank(rocket);
            if (tank == null) {
                continue;
            }
            double a = CargoFlights.dp(r.homeDimension());
            double b = CargoFlights.dp(r.targetDimension());
            ServerLevel tl = level(server, r.targetDimension());
            boolean targetFuel = false;
            if (tl != null) {
                tl.getChunkAt(r.target());
                if (tl.getBlockEntity(r.target()) instanceof CargoSink sink) {
                    String fuel = tank.get("fluid").getAsString();
                    targetFuel = sink.fuelTank() != null && !sink.fuelTank().stack().isEmpty() || sink.amountOf(fuel) > 0;
                }
            }
            if (RouteRules.fuelWarning(CargoRockets.fuelPerLeg(rocket, a, b), CargoRockets.fuelPerLeg(rocket, b, a),
                    tank.get("capacity").getAsLong(), targetFuel)) {
                return true;
            }
        }
        return false;
    }

    public static void send(ServerPlayer player, BlockPos computer, String message) {
        Services.NETWORK.sendToPlayer(player, state(player, computer, message));
    }

    /** Änderung aus dem GUI übernehmen; Rückmeldung als Übersetzungsschlüssel (leer = gespeichert). */
    public static void apply(ServerPlayer player, LogisticsEditPayload p) {
        if (player.distanceToSqr(Vec3.atCenterOf(p.computer())) > MAX_DISTANCE * MAX_DISTANCE
                || !player.level().getBlockState(p.computer()).is(ModBlocks.LOGISTICS_COMPUTER.get())) {
            return;
        }
        String message = p.action() == LogisticsEditPayload.REFRESH ? "" : edit(player, p.action(), p.route());
        send(player, p.computer(), message);
    }

    /** Prüft und speichert bzw. löscht; gibt den Schlüssel der Rückmeldung zurück. */
    public static String edit(ServerPlayer player, int action, Route in) {
        MinecraftServer server = player.level().getServer();
        UUID agency = agencyOf(player);
        if (!Permissions.can(player, agency, Permissions.Action.CONFIGURE)) {
            return "gui.infinite_horizon.logistics.denied";
        }
        CargoFlights data = CargoFlights.get(server);
        Route old = data.route(in.id()).filter(r -> r.agency().equals(agency)).orElse(null);
        if (action == LogisticsEditPayload.DELETE) {
            if (old != null) {
                data.removeRoute(old.id());
            }
            return "gui.infinite_horizon.logistics.deleted";
        }
        List<OutpostData.Terminal> own = terminalsOf(server, agency);
        boolean homeOk = own.stream().anyMatch(t -> t.dimension().equals(in.homeDimension()) && t.pos().equals(in.home()));
        // Ziel: eingetragen und erlaubt (eigen oder freigegeben bei einem Partner, 23.3)
        boolean targetOk = OutpostData.get(server).entry(in.targetDimension(), in.target()) != null
                && mayDeliver(server, agency, in.targetDimension(), in.target());
        if (!homeOk || !targetOk || in.homeDimension().equals(in.targetDimension()) && in.home().equals(in.target())) {
            return "gui.infinite_horizon.logistics.bad_terminal";
        }
        CargoTerminalBlockEntity home = load(server, in.homeDimension(), in.home());
        Set<Integer> slots = new HashSet<>(in.rockets());
        for (int s : slots) {
            if (home == null || s < 0 || s >= home.hangarSlots()) {
                return "gui.infinite_horizon.logistics.bad_rocket";
            }
        }
        // eine Rakete gehört höchstens zu einer Route
        for (Route r : data.routesOf(agency)) {
            if (!r.id().equals(in.id()) && r.homeDimension().equals(in.homeDimension()) && r.home().equals(in.home())
                    && r.rockets().stream().anyMatch(slots::contains)) {
                return "gui.infinite_horizon.logistics.rocket_taken";
            }
        }
        List<String> filter = new ArrayList<>();
        for (String f : in.filter()) {
            Identifier id = Identifier.tryParse(f.trim());
            if (id != null && !filter.contains(id.toString()) && filter.size() < MAX_FILTER) {
                filter.add(id.toString());
            }
        }
        Identifier item = in.item().isBlank() ? null : Identifier.tryParse(in.item().trim());
        RouteRules.Condition c = RouteRules.Condition.byId(in.condition());
        // neue Route: die ID des Clients übernehmen (sein Editor bleibt dann auf der gespeicherten Route), falls frei
        UUID id = old != null ? old.id() : data.route(in.id()).isEmpty() ? in.id() : UUID.randomUUID();
        data.putRoute(new Route(id, agency, in.homeDimension(), in.home(), in.targetDimension(), in.target(), filter, c.id(),
                Math.max(0, in.amount()), item == null ? "" : item.toString(), slots.stream().sorted().toList(),
                old != null ? old.lastStart() : 0));
        return "gui.infinite_horizon.logistics.saved";
    }
}
