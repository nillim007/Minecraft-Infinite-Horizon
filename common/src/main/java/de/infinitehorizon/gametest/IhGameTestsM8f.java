package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.item.StarChartItem;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.logistics.LogisticsService;
import de.infinitehorizon.logistics.Route;
import de.infinitehorizon.logistics.RouteRules;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.shipyard.ShipyardDockBlockEntity;
import de.infinitehorizon.shipyard.ShipyardMath;
import de.infinitehorizon.shipyard.ShipyardService;
import de.infinitehorizon.shipyard.ShipyardState;
import de.infinitehorizon.shipyard.ShipyardTerminalBlock;
import de.infinitehorizon.shipyard.ShipyardTerminalBlockEntity;
import de.infinitehorizon.station.StationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** GameTests für M8f (21.1, 21.2, 20.4): Werft-Terminal, Dock, Werftlager, Bau, Bedarf für den Logistik-Computer. */
public final class IhGameTestsM8f {

    private static final BlockPos CORE = new BlockPos(4, 1, 4);
    private static final BlockPos TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos DOCK = new BlockPos(6, 1, 2);
    private static final BlockPos CARGO = new BlockPos(6, 1, 6);

    private IhGameTestsM8f() {
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(InfiniteHorizon.id(id));
    }

    /** Station (Kern im Register), Werft-Terminal und Dock; Werft angemeldet. */
    private static ShipyardDockBlockEntity yard(GameTestHelper h) {
        StationData.get(h.getLevel()).put(h.absolutePos(CORE), null);
        h.setBlock(TERMINAL, ModBlocks.SHIPYARD_TERMINAL.get().defaultBlockState());
        h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).update(h.getLevel());
        h.setBlock(DOCK, ModBlocks.SHIPYARD_DOCK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ShipyardDockBlockEntity dock = h.getBlockEntity(DOCK, ShipyardDockBlockEntity.class);
        dock.refreshLayout();
        return dock;
    }

    private static ShipyardState.Yard state(GameTestHelper h) {
        return ShipyardState.get(h.getLevel().getServer()).yard(h.getLevel().dimension().identifier(), h.absolutePos(TERMINAL));
    }

    /** 21.1: Das Terminal arbeitet nur in einer Station im Erdorbit. */
    public static void terminalLocation(GameTestHelper h) {
        // hoch über dem Testbereich: weit weg von den Stationskernen paralleler Tests
        BlockPos at = h.absolutePos(TERMINAL).above(150);
        BlockPos core = at.east(3);
        h.assertFalse(ShipyardService.terminalAllowed(h.getLevel(), at), "ohne Stationskern erlaubt");
        StationData.get(h.getLevel()).put(core, null);
        try {
            h.assertTrue(ShipyardService.terminalAllowed(h.getLevel(), at), "in der Station im Erdorbit verboten");
        } finally {
            StationData.get(h.getLevel()).remove(core);
        }
        h.succeed();
    }

    /** 21.1: Das Dock nimmt nur, was die Werft braucht; Methan füllt zuerst den Treibstofftank. */
    public static void dockDemand(GameTestHelper h) {
        ShipyardDockBlockEntity dock = yard(h);
        h.assertTrue(dock.yard() != null, "Dock findet die Werft nicht");
        String plate = "infinite_horizon:lightweight_plate";
        long need = dock.demand(plate);
        h.assertTrue(need > 0, "kein Bedarf an Leichtbauplatten");
        ItemStack rest = dock.insertItem(new ItemStack(item("lightweight_plate"), 10), false);
        h.assertTrue(rest.isEmpty() && dock.demand(plate) == need - 10, "Lieferung: Rest " + rest + ", Bedarf " + dock.demand(plate));
        h.assertTrue(dock.insertItem(new ItemStack(Items.DIRT, 4), false).getCount() == 4, "Erde angenommen");
        h.assertTrue(dock.canPlaceItem(0, new ItemStack(item("lightweight_plate"))), "Trichter: Platte abgelehnt");
        h.assertFalse(dock.canPlaceItem(0, new ItemStack(Items.DIRT)), "Trichter: Erde angenommen");
        Identifier methane = InfiniteHorizon.id("methane");
        long cap = dock.fuelTank().capacity();
        long took = dock.insertFluid(IhFluidStack.of(methane, cap + 6000), false);
        h.assertTrue(took == cap + 6000, "Methan angenommen: " + took);
        h.assertTrue(dock.fuelTank().stack().amount() == cap, "Treibstofftank " + dock.fuelTank().stack());
        h.assertTrue(ShipyardState.delivered(state(h), methane.toString()) == 6000, "Methan im Werftlager: "
                + state(h).delivered() + ", Dock-Werft " + (dock.yard() == null ? null : dock.yard().pos()) + ", Terminal "
                + h.absolutePos(TERMINAL));
        StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        h.succeed();
    }

    /** 18, 21.2: Für den Bordcomputer nur die Sternenkarte der eigenen Agentur. */
    public static void starChartOwner(GameTestHelper h) {
        ShipyardDockBlockEntity dock = yard(h);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            UUID own = LogisticsService.agencyOf(p);
            h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).claimFor(p);
            h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).update(h.getLevel());
            h.assertTrue(state(h).owner().isPresent(), "Werft ohne Besitzer");
            ItemStack foreign = new ItemStack(ModItems.STAR_CHART.get());
            foreign.set(ModDataComponents.CHART_OWNER.get(), new StarChartItem.ChartOwner(UUID.randomUUID(), UUID.randomUUID()));
            h.assertTrue(dock.insertItem(foreign, false).getCount() == 1, "fremde Sternenkarte angenommen");
            ItemStack mine = new ItemStack(ModItems.STAR_CHART.get());
            mine.set(ModDataComponents.CHART_OWNER.get(), new StarChartItem.ChartOwner(own, UUID.randomUUID()));
            h.assertTrue(dock.insertItem(mine, false).isEmpty(), "eigene Sternenkarte abgelehnt");
        } finally {
            IhTestPlayers.remove(p);
            StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        }
        h.succeed();
    }

    /** 21.1: Voll belieferter Abschnitt baut {@code build_ticks} (Overworld-Zeit), danach wächst das Gerüst. */
    public static void buildSection(GameTestHelper h) {
        ShipyardDockBlockEntity dock = yard(h);
        ShipyardMath.Section first = ShipyardState.sections().getFirst();
        for (ShipyardMath.Material m : first.materials()) {
            h.assertFalse(m.fluid(), "Abschnitt 1 mit Fluid");
            dock.insertItem(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(m.id())), (int) m.amount()), false);
        }
        ShipyardState data = ShipyardState.get(h.getLevel().getServer());
        long now = h.getLevel().getServer().overworld().getGameTime();
        ShipyardState.Yard y = state(h);
        h.assertTrue(y.buildEnd() == now + first.buildTicks(), "Bauende " + y.buildEnd() + ", jetzt " + now);
        h.assertTrue(data.advance(y, now + first.buildTicks() - 1).built() == 0, "zu früh fertig");
        h.assertTrue(data.advance(state(h), now + first.buildTicks()).built() == 1, "nicht fertig nach der Bauzeit");
        h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).update(h.getLevel());
        h.assertTrue(h.getBlockState(TERMINAL).getValue(ShipyardTerminalBlock.STAGE) == 1, "Gerüst nicht gewachsen");
        StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        h.succeed();
    }

    /** 20.4 mit echter Werft: „bis der Bedarf gedeckt ist“ lädt nur Benötigtes und stoppt bei 0 Bedarf. */
    public static void demandRoute(GameTestHelper h) {
        ShipyardDockBlockEntity dock = yard(h);
        h.setBlock(CARGO, ModBlocks.CARGO_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CargoTerminalBlockEntity t = h.getBlockEntity(CARGO, CargoTerminalBlockEntity.class);
        t.refreshLayout();
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
        t.setItem(t.layout().moldStart(), rocket);
        t.insertItem(new ItemStack(item("lightweight_plate"), 20), false);
        t.insertItem(new ItemStack(Items.DIRT, 10), false);
        Identifier dim = h.getLevel().dimension().identifier();
        String plate = "infinite_horizon:lightweight_plate";
        Route r = new Route(UUID.randomUUID(), new UUID(0, 0), dim, h.absolutePos(CARGO), dim, h.absolutePos(DOCK), List.of(),
                RouteRules.Condition.UNTIL_DEMAND_MET.id(), 0, "", List.of(0), 0);
        CargoFlights data = CargoFlights.get(h.getLevel().getServer());
        data.putRoute(r);
        try {
            CargoFlights.process(h.getLevel().getServer());
            CargoFlights.Flight f = data.flightOf(dim, h.absolutePos(CARGO), 0).orElse(null);
            h.assertTrue(f != null, "Route mit offenem Bedarf nicht gestartet");
            h.assertTrue(t.count(Items.DIRT) == 10 && t.count(item("lightweight_plate")) == 0, "geladen: Erde "
                    + t.count(Items.DIRT) + ", Platten " + t.count(item("lightweight_plate")));
            data.arriveNow(f.id(), h.getLevel().getServer().overworld().getGameTime());
            CargoFlights.process(h.getLevel().getServer());
            h.assertTrue(ShipyardState.delivered(state(h), plate) == 20, "im Werftlager: " + ShipyardState.delivered(state(h), plate));
            // Bedarf an Platten decken: eine Route nur für Platten startet nicht mehr
            dock.insertItem(new ItemStack(item("lightweight_plate"), (int) dock.demand(plate)), false);
            h.assertTrue(dock.demand(plate) == 0, "Bedarf " + dock.demand(plate));
            data.removeRoute(r.id());
            Route plates = new Route(UUID.randomUUID(), new UUID(0, 0), dim, h.absolutePos(CARGO), dim, h.absolutePos(DOCK),
                    List.of(plate), RouteRules.Condition.UNTIL_DEMAND_MET.id(), 0, "", List.of(1), 0);
            t.setItem(t.layout().moldStart() + 1, rocket.copy());
            t.insertItem(new ItemStack(item("lightweight_plate"), 20), false);
            data.putRoute(plates);
            CargoFlights.process(h.getLevel().getServer());
            h.assertTrue(data.flightOf(dim, h.absolutePos(CARGO), 1).isEmpty(), "Route trotz 0 Bedarf gestartet");
            data.removeRoute(plates.id());
        } finally {
            data.removeRoute(r.id());
            StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        }
        h.succeed();
    }
}
