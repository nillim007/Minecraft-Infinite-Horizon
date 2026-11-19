package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyAdvancements;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.VenusHeat;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.Route;
import de.infinitehorizon.logistics.RouteRules;
import de.infinitehorizon.machine.FusionReactorBlockEntity;
import de.infinitehorizon.platform.Services;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GameTests für M9c (24.3): alle Erfolge geladen, Venus-Rückkehr, Fusionsreaktor, Netzwerk. */
public final class IhGameTestsM9c {

    private IhGameTestsM9c() {
    }

    private static boolean done(ServerPlayer p, Identifier id) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(id);
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static boolean pending(Agency a, UUID player, Identifier id) {
        return a.progress().pendingAdvancements().getOrDefault(player, List.of()).contains(id);
    }

    /** 24.3: Alle 14 Erfolge des Reiters sind geladen und hängen am vorgesehenen Elternteil. */
    public static void allLoaded(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        JsonObject byId = BalanceManager.get().entry("misc", "advancements").orElseThrow().getAsJsonObject("by_id");
        h.assertTrue(byId.size() == 14, "Erfolge in misc.json (mit Ergänzungen): " + byId.size());
        for (Map.Entry<String, com.google.gson.JsonElement> e : byId.entrySet()) {
            AdvancementHolder holder = server.getAdvancements().get(Identifier.parse(e.getKey()));
            h.assertTrue(holder != null, "Erfolg nicht geladen: " + e.getKey());
            JsonObject a = e.getValue().getAsJsonObject();
            String parent = a.has("parent") && !a.get("parent").isJsonNull() ? a.get("parent").getAsString() : null;
            String actual = holder.value().parent().map(Identifier::toString).orElse(null);
            h.assertTrue(java.util.Objects.equals(parent, actual), e.getKey() + ": Elternteil " + actual + ", erwartet " + parent);
        }
        h.succeed();
    }

    /** 24.3 „Drei Minuten Hölle“: unter die Wolkenschicht der Venus und wieder hinauf (Werte aus suits.json). */
    public static void venusReturn(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            VenusHeat heat = VenusHeat.rules();
            h.assertTrue(heat != null, "venus_heat_timer fehlt");
            h.assertTrue(h.getLevel().dimension().identifier().equals(heat.dimension()), "Test läuft nicht auf der Venus");
            HazardState state = Services.ATTACHMENTS.hazardState(p);
            Identifier id = AgencyAdvancements.THREE_MINUTES_OF_HELL;
            p.setPos(p.getX(), heat.refillAboveY() + 4, p.getZ());
            HazardTracker.venusDescent(p, state);
            h.assertFalse(done(p, id) || state.venusDescent(), "oben ohne Abstieg");
            p.setPos(p.getX(), heat.belowY() - 20, p.getZ());
            HazardTracker.venusDescent(p, state);
            h.assertTrue(state.venusDescent() && !done(p, id), "Abstieg nicht vermerkt");
            p.setPos(p.getX(), heat.refillAboveY() + 4, p.getZ());
            HazardTracker.venusDescent(p, state);
            h.assertTrue(done(p, id), "Erfolg nach der Rückkehr fehlt");
            h.assertFalse(state.venusDescent(), "Abstieg nicht zurückgesetzt");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** 24.3 „Sonne in der Flasche“: Der Fusionsreaktor erzeugt Strom; alle Mitglieder der Besitzer-Agentur bekommen ihn. */
    public static void sunInABottle(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        UUID leader = UUID.randomUUID();
        Agency a = AgencyData.get(server).ensureFor(leader, server.overworld().getGameTime());
        FusionReactorBlockEntity be = IhGameTestsM6d.buildFusion(h);
        be.setOwner(a.id());
        be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("hydrogen"), 16000));
        be.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("helium"), 16000));
        be.fluidTanks().get(2).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("liquid_helium"), 16000));
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(0));
        h.succeedWhen(() -> h.assertTrue(pending(a, leader, AgencyAdvancements.SUN_IN_A_BOTTLE),
                "Erfolg fehlt; Status " + be.status()));
    }

    /** 24.3 „Netzwerk“: erst mit {@code active_routes} Routen gleichzeitig, jede mit mindestens einer Rakete. */
    public static void network(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        int needed = AgencyAdvancements.networkRoutes();
        h.assertTrue(needed > 0, "active_routes fehlt");
        UUID leader = UUID.randomUUID();
        Agency a = AgencyData.get(server).ensureFor(leader, server.overworld().getGameTime());
        CargoFlights data = CargoFlights.get(server);
        Identifier dim = h.getLevel().dimension().identifier();
        List<UUID> ids = new ArrayList<>();
        try {
            // eine Route ohne Rakete zählt nicht
            Route empty = new Route(UUID.randomUUID(), a.id(), dim, h.absolutePos(new BlockPos(0, 1, 0)), dim,
                    h.absolutePos(new BlockPos(1, 1, 0)), List.of(), RouteRules.Condition.WHEN_FULL.id(), 0, "", List.of(), 0);
            data.putRoute(empty);
            ids.add(empty.id());
            for (int i = 0; i < needed; i++) {
                if (i == needed - 1) {
                    CargoFlights.checkNetwork(server, data);
                    h.assertFalse(pending(a, leader, AgencyAdvancements.NETWORK), "Erfolg schon bei " + i + " Routen");
                }
                Route r = new Route(UUID.randomUUID(), a.id(), dim, h.absolutePos(new BlockPos(0, 1, 0)), dim,
                        h.absolutePos(new BlockPos(1, 1, 0)), List.of(), RouteRules.Condition.WHEN_FULL.id(), 0, "",
                        List.of(i), 0);
                data.putRoute(r);
                ids.add(r.id());
            }
            CargoFlights.checkNetwork(server, data);
            h.assertTrue(pending(a, leader, AgencyAdvancements.NETWORK), "Erfolg fehlt bei " + needed + " Routen");
        } finally {
            ids.forEach(data::removeRoute);
        }
        h.succeed();
    }
}
