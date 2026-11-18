package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.logistics.LogisticsService;
import de.infinitehorizon.logistics.Route;
import de.infinitehorizon.logistics.RouteRules;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.network.LogisticsEditPayload;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** GameTests für M8e (20.4, 27): Routen, Startbedingungen, Logistik-Computer. */
public final class IhGameTestsM8e {

    private static final BlockPos A = new BlockPos(1, 1, 2);
    private static final BlockPos B = new BlockPos(6, 1, 2);
    private static final Identifier METHANE = InfiniteHorizon.id("methane");

    private IhGameTestsM8e() {
    }

    private static CargoTerminalBlockEntity terminal(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.CARGO_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CargoTerminalBlockEntity t = h.getBlockEntity(pos, CargoTerminalBlockEntity.class);
        t.refreshLayout();
        return t;
    }

    /** Start-Terminal mit getankter Rakete im Hangar-Slot 0 und Eisen im Lager. */
    private static CargoTerminalBlockEntity home(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
        a.setItem(a.layout().moldStart(), rocket);
        a.insertItem(new ItemStack(Items.IRON_INGOT, 64), false);
        return a;
    }

    private static Route route(GameTestHelper h, RouteRules.Condition c, long amount, String item) {
        Identifier dim = h.getLevel().dimension().identifier();
        return new Route(UUID.randomUUID(), new UUID(0, 0), dim, h.absolutePos(A), dim, h.absolutePos(B), List.of(), c.id(),
                amount, item, List.of(0), 0);
    }

    private static boolean flying(GameTestHelper h) {
        return CargoFlights.get(h.getLevel().getServer())
                .flightOf(h.getLevel().dimension().identifier(), h.absolutePos(A), 0).isPresent();
    }

    /** Route anlegen, Flüge und Routen bearbeiten, Route wieder entfernen. */
    private static boolean runOnce(GameTestHelper h, Route r) {
        CargoFlights data = CargoFlights.get(h.getLevel().getServer());
        data.putRoute(r);
        try {
            CargoFlights.process(h.getLevel().getServer());
            return flying(h);
        } finally {
            data.removeRoute(r.id());
        }
    }

    /** Abnahme M8 (27): Eine Route „bis der Bedarf gedeckt ist“ stoppt bei 0 Bedarf. */
    public static void demandStops(GameTestHelper h) {
        RouteRules.Context zero = new RouteRules.Context(100, 0, 0, 5, 108, 0, 4, 0, 0);
        RouteRules.Context open = new RouteRules.Context(100, 0, 0, 5, 108, 0, 4, 0, 64);
        h.assertFalse(RouteRules.shouldStart(RouteRules.Condition.UNTIL_DEMAND_MET, zero), "startet bei 0 Bedarf");
        h.assertTrue(RouteRules.shouldStart(RouteRules.Condition.UNTIL_DEMAND_MET, open), "startet nicht bei offenem Bedarf");
        // im Spiel: ein Frachtterminal meldet keinen Bedarf, die Route startet nicht
        home(h);
        terminal(h, B);
        h.assertFalse(runOnce(h, route(h, RouteRules.Condition.UNTIL_DEMAND_MET, 0, "")), "Route trotz 0 Bedarf gestartet");
        h.succeed();
    }

    /** 20.4: „alle N Ticks“ startet die Rakete, merkt den Start und zeigt den Status „geladen“. */
    public static void everyNTicks(GameTestHelper h) {
        CargoTerminalBlockEntity a = home(h);
        terminal(h, B);
        Route r = route(h, RouteRules.Condition.EVERY_N_TICKS, 100, "");
        CargoFlights data = CargoFlights.get(h.getLevel().getServer());
        data.putRoute(r);
        CargoFlights.process(h.getLevel().getServer());
        h.assertTrue(flying(h), "Rakete nicht gestartet");
        h.assertTrue(a.count(Items.IRON_INGOT) == 0, "Fracht nicht geladen");
        Route after = data.route(r.id()).orElseThrow();
        h.assertTrue(after.lastStart() > 0, "letzter Start nicht gemerkt");
        CargoFlights.RocketStatus s = CargoFlights.status(h.getLevel().getServer(), after, 0);
        h.assertTrue("loaded".equals(s.status()) && s.remaining() > 0, "Status " + s);
        data.removeRoute(r.id());
        h.succeed();
    }

    /** 20.4: „wenn Ziel weniger als N von X hat“. */
    public static void targetBelow(GameTestHelper h) {
        home(h);
        CargoTerminalBlockEntity b = terminal(h, B);
        b.insertItem(new ItemStack(Items.IRON_INGOT, 10), false);
        h.assertFalse(runOnce(h, route(h, RouteRules.Condition.WHEN_TARGET_BELOW, 5, "minecraft:iron_ingot")),
                "gestartet, obwohl das Ziel genug hat");
        h.assertTrue(runOnce(h, route(h, RouteRules.Condition.WHEN_TARGET_BELOW, 20, "minecraft:iron_ingot")),
                "nicht gestartet, obwohl das Ziel zu wenig hat");
        h.succeed();
    }

    /** 20.4: „wenn voll“: erst wenn die Fracht alle Fluid-Laderäume (oder alle Item-Slots) der Rakete füllt. */
    public static void whenFull(GameTestHelper h) {
        CargoTerminalBlockEntity a = home(h);
        terminal(h, B);
        h.assertFalse(runOnce(h, route(h, RouteRules.Condition.WHEN_FULL, 0, "")), "gestartet mit fast leerem Lager");
        int holds = CargoRockets.fluidHolds(a.hangar(0));
        for (int i = 0; i < holds; i++) {
            a.cargoTanks().get(i).setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 1000));
        }
        h.assertTrue(runOnce(h, route(h, RouteRules.Condition.WHEN_FULL, 0, "")), "nicht gestartet mit vollen Fluid-Laderäumen");
        h.succeed();
    }

    /** 20.4, 3A.5: Routen über den Logistik-Computer anlegen, doppelte Raketen ablehnen, löschen. */
    public static void editRoutes(GameTestHelper h) {
        home(h);
        terminal(h, B);
        h.setBlock(new BlockPos(3, 1, 4), ModBlocks.LOGISTICS_COMPUTER.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(3.5, 2, 3.5));
        h.runAfterDelay(2, () -> {
            try {
                CargoFlights data = CargoFlights.get(h.getLevel().getServer());
                UUID agency = LogisticsService.agencyOf(p);
                Route r = route(h, RouteRules.Condition.EVERY_N_TICKS, 600, "");
                String msg = LogisticsService.edit(p, LogisticsEditPayload.SAVE, r);
                h.assertTrue("gui.infinite_horizon.logistics.saved".equals(msg), "Speichern: " + msg);
                List<Route> mine = data.routesOf(agency);
                h.assertTrue(mine.size() == 1 && mine.getFirst().agency().equals(agency), "Routen " + mine);
                String twice = LogisticsService.edit(p, LogisticsEditPayload.SAVE, route(h, RouteRules.Condition.WHEN_FULL, 0, ""));
                h.assertTrue("gui.infinite_horizon.logistics.rocket_taken".equals(twice), "doppelte Rakete: " + twice);
                h.assertTrue(LogisticsService.state(p, h.absolutePos(new BlockPos(3, 1, 4)), "").routes().size() == 1, "Zustand");
                LogisticsService.edit(p, LogisticsEditPayload.DELETE, mine.getFirst());
                h.assertTrue(data.routesOf(agency).isEmpty(), "nicht gelöscht");
            } finally {
                IhTestPlayers.remove(p);
            }
            h.succeed();
        });
    }
}
