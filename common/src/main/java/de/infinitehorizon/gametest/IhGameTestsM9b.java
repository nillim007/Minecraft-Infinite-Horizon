package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.logistics.LogisticsService;
import de.infinitehorizon.logistics.OutpostManager;
import de.infinitehorizon.logistics.Route;
import de.infinitehorizon.logistics.RouteRules;
import de.infinitehorizon.logistics.TradingPostBlockEntity;
import de.infinitehorizon.logistics.TradingRules;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.network.LogisticsEditPayload;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.shipyard.Finale;
import de.infinitehorizon.shipyard.ShipyardDockBlockEntity;
import de.infinitehorizon.shipyard.ShipyardState;
import de.infinitehorizon.shipyard.ShipyardTerminalBlockEntity;
import de.infinitehorizon.station.StationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** GameTests für M9b (23.3, 20.3, 20.4): Partnerschaft, Handelsposten, Lieferungen an Partner, gemeinsame Werft. */
public final class IhGameTestsM9b {

    private static final BlockPos A = new BlockPos(1, 1, 2);
    private static final BlockPos B = new BlockPos(6, 1, 2);
    private static final BlockPos POST = new BlockPos(3, 1, 5);

    private IhGameTestsM9b() {
    }

    private static Agency agency(MinecraftServer server, UUID leader) {
        return AgencyData.get(server).ensureFor(leader, server.overworld().getGameTime());
    }

    private static void partner(AgencyData data, UUID leaderA, Agency a, UUID leaderB, Agency b) {
        data.requestPartner(leaderA, b.id());
        data.requestPartner(leaderB, a.id());
    }

    /** 23.3: Partnerschaft erst, wenn beide Leiter bestätigt haben; nur Leiter; jeder Leiter kann sie beenden. */
    public static void partnership(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        AgencyData data = AgencyData.get(server);
        long tick = server.overworld().getGameTime();
        UUID la = UUID.randomUUID();
        UUID lb = UUID.randomUUID();
        UUID engineer = UUID.randomUUID();
        Agency a = agency(server, la);
        Agency b = agency(server, lb);
        data.ensureFor(engineer, tick);
        data.invite(la, engineer);
        data.accept(engineer, a.id(), tick);
        h.assertTrue(data.requestPartner(engineer, b.id()) == AgencyData.ActionResult.NOT_LEADER, "Ingenieur bietet an");
        h.assertTrue(data.requestPartner(la, a.id()) == AgencyData.ActionResult.UNKNOWN_AGENCY, "Partner mit sich selbst");
        h.assertTrue(data.requestPartner(la, b.id()) == AgencyData.ActionResult.OK, "Angebot");
        h.assertFalse(data.arePartners(a.id(), b.id()), "Partner nach einem Leiter");
        h.assertTrue(data.partnerRequestsTo(b.id()).contains(a), "Angebot nicht bei B");
        h.assertTrue(data.requestPartner(lb, a.id()) == AgencyData.ActionResult.OK, "Bestätigung");
        h.assertTrue(data.arePartners(a.id(), b.id()) && data.arePartners(b.id(), a.id()), "keine Partner nach zwei Leitern");
        h.assertTrue(data.requestPartner(la, b.id()) == AgencyData.ActionResult.ALREADY_PARTNER, "doppelt");
        // Partner sind keine Mitglieder: Gast-Rechte bleiben
        h.assertFalse(Permissions.allowed(b.roleOf(la).orElse(null), Permissions.Action.USE_SLOTS), "Partner darf Slots");
        h.assertTrue(data.endPartner(lb, a.id()) == AgencyData.ActionResult.OK, "Beenden");
        h.assertFalse(data.arePartners(a.id(), b.id()), "Partner nach dem Beenden");
        h.succeed();
    }

    private static TradingPostBlockEntity post(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.TRADING_POST.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return h.getBlockEntity(pos, TradingPostBlockEntity.class);
    }

    /** 23.3: Jeder Spieler tauscht, auch Nicht-Mitglieder; X ins Lager, Y aus dem Lager; Fluidfracht nimmt er nicht. */
    public static void trade(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            MinecraftServer server = h.getLevel().getServer();
            Agency owner = agency(server, UUID.randomUUID());
            agency(server, p.getUUID());
            TradingPostBlockEntity t = post(h, POST);
            t.setOwner(owner.id());
            h.assertTrue(TradingPostBlockEntity.maxOffers() > 0 && t.getContainerSize() == TradingPostBlockEntity.storageSlots(),
                    "Lager " + t.getContainerSize());
            h.assertFalse(Permissions.can(p, owner.id(), Permissions.Action.USE_SLOTS), "Fremder darf ins Lager");
            t.setOffer(0, new ItemStack(Items.IRON_INGOT, 4));
            t.setOffer(1, new ItemStack(Items.DIAMOND, 1));
            t.insertItem(new ItemStack(Items.DIAMOND, 1), false);
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 8));
            h.assertTrue(t.available(0) == 1, "Vorrat " + t.available(0));
            h.assertTrue(t.trade(p, 1) == TradingRules.Trade.NO_OFFER, "leeres Angebot");
            h.assertTrue(t.trade(p, 0) == TradingRules.Trade.OK, "Tausch abgelehnt");
            h.assertTrue(p.getInventory().countItem(Items.IRON_INGOT) == 4 && p.getInventory().countItem(Items.DIAMOND) == 1,
                    "Inventar: Eisen " + p.getInventory().countItem(Items.IRON_INGOT) + ", Diamant "
                            + p.getInventory().countItem(Items.DIAMOND));
            h.assertTrue(t.amountOf("minecraft:iron_ingot") == 4 && t.amountOf("minecraft:diamond") == 0, "Lager nach dem Tausch");
            h.assertTrue(t.trade(p, 0) == TradingRules.Trade.OUT_OF_STOCK, "Tausch ohne Vorrat");
            t.insertItem(new ItemStack(Items.DIAMOND, 1), false);
            p.getInventory().clearContent();
            h.assertTrue(t.trade(p, 0) == TradingRules.Trade.NO_ITEMS, "Tausch ohne Eisen");
            h.assertFalse(t.acceptsFluidCargo(), "Handelsposten nimmt Fluidfracht");
            h.assertTrue(t.insertFluid(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 1000), false) == 0, "Fluid angenommen");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    private static CargoTerminalBlockEntity terminal(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.CARGO_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CargoTerminalBlockEntity t = h.getBlockEntity(pos, CargoTerminalBlockEntity.class);
        t.refreshLayout();
        return t;
    }

    private static CargoFlights.Flight flyNow(GameTestHelper h, CargoTerminalBlockEntity from, BlockPos to) {
        CargoFlights.Launch r = CargoFlights.launch(h.getLevel(), from, 0, h.getLevel().dimension().identifier(),
                h.absolutePos(to), s -> true, f -> true);
        h.assertTrue(r == CargoFlights.Launch.OK, "Start " + r);
        CargoFlights data = CargoFlights.get(h.getLevel().getServer());
        CargoFlights.Flight f = data.flightOf(h.getLevel().dimension().identifier(), from.getBlockPos(), 0).orElseThrow();
        data.arriveNow(f.id(), h.getLevel().getServer().overworld().getGameTime());
        CargoFlights.process(h.getLevel().getServer());
        return data.flightOf(h.getLevel().dimension().identifier(), from.getBlockPos(), 0).orElse(null);
    }

    private static void home(GameTestHelper h, CargoFlights.Flight back) {
        CargoFlights data = CargoFlights.get(h.getLevel().getServer());
        data.arriveNow(back.id(), h.getLevel().getServer().overworld().getGameTime());
        CargoFlights.process(h.getLevel().getServer());
    }

    /**
     * 23.3, 20.4: Raketen liefern an ein fremdes Terminal nur, wenn die Agenturen Partner sind und es freigegeben ist;
     * Handelsposten der Partner immer. Routen dorthin lassen sich erst dann anlegen; endet die Partnerschaft, bringt
     * die Rakete die Fracht zurück.
     */
    public static void partnerDelivery(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 4.5));
        try {
            MinecraftServer server = h.getLevel().getServer();
            AgencyData agencies = AgencyData.get(server);
            Identifier dim = h.getLevel().dimension().identifier();
            Agency x = agency(server, p.getUUID());
            UUID ly = UUID.randomUUID();
            Agency y = agency(server, ly);
            CargoTerminalBlockEntity a = terminal(h, A);
            CargoTerminalBlockEntity b = terminal(h, B);
            a.setOwner(x.id());
            b.setOwner(y.id());
            OutpostManager.registerTerminal(h.getLevel(), a.getBlockPos(), x.id());
            OutpostManager.registerTerminal(h.getLevel(), b.getBlockPos(), y.id());
            TradingPostBlockEntity post = post(h, POST);
            post.setOwner(y.id());
            OutpostManager.data(h.getLevel()).addTradingPost(dim, post.getBlockPos(), y.id());
            h.assertFalse(LogisticsService.mayDeliver(server, x.id(), dim, b.getBlockPos()), "fremdes Terminal ohne Partner");
            h.assertFalse(LogisticsService.mayDeliver(server, x.id(), dim, post.getBlockPos()), "fremder Handelsposten");
            Route route = new Route(UUID.randomUUID(), x.id(), dim, a.getBlockPos(), dim, b.getBlockPos(), List.of(),
                    RouteRules.Condition.WHEN_FULL.id(), 0, "", List.of(0), 0);
            h.assertTrue(LogisticsService.edit(p, LogisticsEditPayload.SAVE, route).endsWith("bad_terminal"), "Route ohne Partner");
            partner(agencies, p.getUUID(), x, ly, y);
            h.assertFalse(LogisticsService.mayDeliver(server, x.id(), dim, b.getBlockPos()), "Terminal nicht freigegeben");
            h.assertTrue(LogisticsService.mayDeliver(server, x.id(), dim, post.getBlockPos()), "Handelsposten des Partners");
            b.setShared(true);
            h.assertTrue(LogisticsService.mayDeliver(server, x.id(), dim, b.getBlockPos()), "freigegebenes Partner-Terminal");
            h.assertTrue(LogisticsService.edit(p, LogisticsEditPayload.SAVE, route).endsWith("saved"), "Route zum Partner");
            CargoFlights.get(server).removeRoute(route.id());
            // Lieferung an den Partner
            ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
            CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
            a.setItem(a.layout().moldStart(), rocket);
            a.insertItem(new ItemStack(Items.IRON_INGOT, 10), false);
            CargoFlights.Flight back = flyNow(h, a, B);
            h.assertTrue(b.count(Items.IRON_INGOT) == 10, "Partner-Terminal hat " + b.count(Items.IRON_INGOT));
            home(h, back);
            // Handelsposten: nur Items, das Wasser fliegt zurück
            a.insertItem(new ItemStack(Items.IRON_INGOT, 5), false);
            a.cargoTanks().getFirst().setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 2000));
            back = flyNow(h, a, POST);
            h.assertTrue(post.amountOf("minecraft:iron_ingot") == 5, "Handelsposten hat " + post.amountOf("minecraft:iron_ingot"));
            h.assertTrue(back != null && back.fluids().stream().mapToLong(IhFluidStack::amount).sum() == 2000,
                    "Wasser nicht an Bord: " + back);
            home(h, back);
            // Partnerschaft beendet: Fracht kommt zurück
            agencies.endPartner(ly, x.id());
            a.insertItem(new ItemStack(Items.GOLD_INGOT, 3), false);
            back = flyNow(h, a, B);
            h.assertTrue(b.count(Items.GOLD_INGOT) == 0, "ohne Partnerschaft geliefert");
            h.assertTrue(back != null && back.items().stream().mapToInt(ItemStack::getCount).sum() == 3, "Fracht nicht an Bord: " + back);
            home(h, back);
            h.assertTrue(a.count(Items.GOLD_INGOT) == 3, "Gold nicht zurück: " + a.count(Items.GOLD_INGOT));
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    private static final BlockPos CORE = new BlockPos(4, 1, 4);
    private static final BlockPos YARD = new BlockPos(2, 1, 2);
    private static final BlockPos DOCK = new BlockPos(6, 1, 2);
    private static final BlockPos CARGO = new BlockPos(6, 1, 6);

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(InfiniteHorizon.id(id));
    }

    /**
     * 23.3 „Gemeinsame Werft“: Eine Partner-Rakete liefert an die freigegebene Werft; beim Start bekommen der Besitzer und
     * alle liefernden Agenturen den Erfolg (Mitglieder offline: beim nächsten Login).
     */
    public static void jointShipyard(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 4.5));
        MinecraftServer server = h.getLevel().getServer();
        try {
            Identifier dim = h.getLevel().dimension().identifier();
            AgencyData agencies = AgencyData.get(server);
            Agency owner = agency(server, p.getUUID());
            UUID lz = UUID.randomUUID();
            Agency z = agency(server, lz);
            StationData.get(h.getLevel()).put(h.absolutePos(CORE), null);
            h.setBlock(YARD, ModBlocks.SHIPYARD_TERMINAL.get().defaultBlockState());
            ShipyardTerminalBlockEntity yt = h.getBlockEntity(YARD, ShipyardTerminalBlockEntity.class);
            yt.setOwner(owner.id());
            ShipyardState.Yard y = yt.update(h.getLevel());
            h.setBlock(DOCK, ModBlocks.SHIPYARD_DOCK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
            ShipyardDockBlockEntity dock = h.getBlockEntity(DOCK, ShipyardDockBlockEntity.class);
            dock.setOwner(owner.id());
            dock.refreshLayout();
            ShipyardState data = ShipyardState.get(server);
            y = data.setShared(y, true);
            OutpostManager.data(h.getLevel()).addDock(dim, dock.getBlockPos(), owner.id(), true);
            partner(agencies, p.getUUID(), owner, lz, z);
            CargoTerminalBlockEntity t = terminal(h, CARGO);
            t.setOwner(z.id());
            OutpostManager.registerTerminal(h.getLevel(), t.getBlockPos(), z.id());
            ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
            CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
            t.setItem(t.layout().moldStart(), rocket);
            t.insertItem(new ItemStack(item("lightweight_plate"), 4), false);
            flyNow(h, t, DOCK);
            y = data.yard(dim, h.absolutePos(YARD));
            h.assertTrue(y.contributors().contains(z.id()), "liefernde Agentur nicht vermerkt: " + y.contributors());
            data.forceBuilt(y, ShipyardState.sections().size());
            h.assertTrue(Finale.start(p, h.absolutePos(YARD)) == Finale.Start.OK, "Start abgelehnt");
            Identifier adv = Identifier.parse(Finale.rules().get("advancement").getAsString());
            h.assertTrue(z.progress().pendingAdvancements().getOrDefault(lz, List.of()).contains(adv),
                    "Erfolg für die Partner-Agentur fehlt");
            h.getLevel().getEntitiesOfClass(de.infinitehorizon.vehicle.StarshipEntity.class,
                    new AABB(h.absolutePos(YARD)).inflate(16)).forEach(s -> s.discard());
        } finally {
            IhTestPlayers.remove(p);
            StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        }
        h.succeed();
    }
}
