package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** GameTests für M8d (20.3, 20.5): Frachtraketen, Hangar, Frachtflüge, Antriebe. */
public final class IhGameTestsM8d {

    private static final BlockPos A = new BlockPos(1, 1, 2);
    private static final BlockPos B = new BlockPos(6, 1, 2);
    private static final Identifier METHANE = InfiniteHorizon.id("methane");

    private IhGameTestsM8d() {
    }

    private static <T extends MachineBlockEntity> T place(GameTestHelper h, BlockPos pos, Block block, Class<T> type) {
        h.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(pos, type);
        be.refreshLayout();
        return be;
    }

    private static CargoTerminalBlockEntity terminal(GameTestHelper h, BlockPos pos) {
        return place(h, pos, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(InfiniteHorizon.id(id));
    }

    private static void putRocket(CargoTerminalBlockEntity t, ItemStack rocket) {
        t.setItem(t.layout().moldStart(), rocket);
    }

    private static CargoFlights.Launch launch(GameTestHelper h, CargoTerminalBlockEntity from) {
        return CargoFlights.launch(h.getLevel(), from, 0, h.getLevel().dimension().identifier(), h.absolutePos(B),
                s -> true, f -> true);
    }

    private static CargoFlights.Flight flight(GameTestHelper h, CargoTerminalBlockEntity from) {
        return CargoFlights.get(h.getLevel().getServer()).flightOf(h.getLevel().dimension().identifier(), from.getBlockPos(), 0)
                .orElse(null);
    }

    /** Ankunft vorziehen und Flüge bearbeiten. */
    private static void arrive(GameTestHelper h, CargoFlights.Flight f) {
        CargoFlights.get(h.getLevel().getServer()).arriveNow(f.id(), h.getLevel().getServer().overworld().getGameTime());
        CargoFlights.process(h.getLevel().getServer());
    }

    /** 20.3: Treibstoff 2.000 + 1.500 × |ΔDP| (Pluto 29.000 mB), Flugzeit 1.000 + 3.950 × |ΔDP|, Antriebe ÷ 2 und ÷ 10. */
    public static void flightMath(GameTestHelper h) {
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        double pluto = CargoFlights.dp(InfiniteHorizon.id("pluto"));
        double earth = CargoFlights.dp(Identifier.withDefaultNamespace("overworld"));
        double moon = CargoFlights.dp(InfiniteHorizon.id("moon"));
        h.assertTrue(CargoRockets.fuelPerLeg(rocket, earth, pluto) == 29000, "Pluto " + CargoRockets.fuelPerLeg(rocket, earth, pluto));
        h.assertTrue(2 * CargoRockets.fuelPerLeg(rocket, earth, pluto) <= CargoRockets.fuelTank(rocket).get("capacity").getAsLong(),
                "Hin- und Rückflug Pluto passt nicht in den Tank");
        h.assertTrue(CargoRockets.flightTicks(rocket, earth, moon) == 1000, "Mond " + CargoRockets.flightTicks(rocket, earth, moon));
        long plain = CargoRockets.flightTicks(rocket, earth, pluto);
        h.assertTrue(plain == 72100, "Pluto " + plain);
        rocket.set(ModDataComponents.DRIVE.get(), InfiniteHorizon.id("atomic_drive"));
        h.assertTrue(CargoRockets.flightTicks(rocket, earth, pluto) == plain / 2, "Atomantrieb");
        rocket.set(ModDataComponents.DRIVE.get(), InfiniteHorizon.id("antimatter_drive"));
        h.assertTrue(CargoRockets.flightTicks(rocket, earth, pluto) == plain / 10, "Antimaterie-Antrieb");
        ItemStack freighter = new ItemStack(ModItems.HEAVY_FREIGHTER.get());
        h.assertTrue(InfiniteHorizon.id("atomic_drive").equals(CargoRockets.bestDrive(freighter)), "Frachter ohne Atomantrieb");
        h.succeed();
    }

    /** 20.3: Rakete + Antrieb im Montagetisch; ein schwächerer Antrieb ersetzt keinen besseren. */
    public static void retrofit(GameTestHelper h) {
        ProcessingMachineBlockEntity t = place(h, new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY_TABLE.get(),
                ProcessingMachineBlockEntity.class);
        h.onEachTick(() -> t.energyStorage().setAmountUnchecked(t.energyStorage().getCapacity()));
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        rocket.set(ModDataComponents.DRIVE.get(), InfiniteHorizon.id("antimatter_drive"));
        rocket.set(ModDataComponents.FLUID.get(), IhFluidStack.of(METHANE, 5000));
        t.setItem(0, rocket);
        t.setItem(1, new ItemStack(item("atomic_drive")));
        h.succeedWhen(() -> {
            MachineLayout l = t.layout();
            ItemStack out = ItemStack.EMPTY;
            for (int i = l.outputStart(); i < l.upgradeStart(); i++) {
                if (t.getItem(i).is(ModItems.CARGO_ROCKET.get())) {
                    out = t.getItem(i);
                }
            }
            h.assertFalse(out.isEmpty(), "noch keine Rakete");
            h.assertTrue(InfiniteHorizon.id("antimatter_drive").equals(out.get(ModDataComponents.DRIVE.get())),
                    "Antrieb " + out.get(ModDataComponents.DRIVE.get()));
            h.assertTrue(CargoRockets.fuel(out) == 5000, "Treibstoff nicht übernommen: " + CargoRockets.fuel(out));
        });
    }

    /** 20.3: Hin- und Rückflug: Tanken am Heimatterminal, Fracht ins Ziel, Rakete im Hangar gesperrt. */
    public static void roundTrip(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        CargoTerminalBlockEntity b = terminal(h, B);
        putRocket(a, new ItemStack(ModItems.CARGO_ROCKET.get()));
        h.assertFalse(a.canPlaceItem(a.layout().moldStart() + 1, new ItemStack(Items.IRON_INGOT)), "Hangar nimmt Eisen");
        a.insertFluid(IhFluidStack.of(METHANE, 10000), false);
        a.cargoTanks().getFirst().setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 5000));
        a.insertItem(new ItemStack(Items.IRON_INGOT, 100), false);
        CargoFlights.Launch r = launch(h, a);
        h.assertTrue(r == CargoFlights.Launch.OK, "Start " + r);
        h.assertTrue(a.fuelTank().stack().isEmpty(), "Treibstofftank nicht geleert: " + a.fuelTank().stack());
        h.assertTrue(a.count(Items.IRON_INGOT) == 0, "Fracht nicht geladen");
        h.assertTrue(a.hangarLocked(0), "Hangar-Slot nicht gesperrt");
        h.assertFalse(a.canTakeItemThroughFace(a.layout().moldStart(), a.hangar(0), Direction.DOWN), "Trichter darf Rakete nehmen");
        CargoFlights.Flight f = flight(h, a);
        h.assertTrue(f != null && f.arrival() - f.start() == CargoRockets.flightTicks(a.hangar(0), 0, 0), "Flugzeit " + f);
        long legFuel = CargoRockets.fuelPerLeg(a.hangar(0), 0, 0);
        arrive(h, f);
        h.assertTrue(b.count(Items.IRON_INGOT) == 100, "Ziel hat " + b.count(Items.IRON_INGOT) + " Eisen");
        h.assertTrue(b.cargoTanks().getFirst().stack().amount() == 5000, "Wasser am Ziel " + b.cargoTanks().getFirst().stack());
        CargoFlights.Flight back = flight(h, a);
        h.assertTrue(back != null && back.returning() && back.items().isEmpty(), "Rückflug " + back);
        arrive(h, back);
        h.assertTrue(flight(h, a) == null, "Flug nicht beendet");
        h.assertFalse(a.hangarLocked(0), "Hangar-Slot noch gesperrt");
        h.assertTrue(CargoRockets.fuel(a.hangar(0)) == 10000 - 2 * legFuel, "Treibstoff " + CargoRockets.fuel(a.hangar(0)));
        h.succeed();
    }

    /** 20.3: Ohne Methan im Terminal und in der Rakete: „ohne Treibstoff“. */
    public static void outOfFuel(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        terminal(h, B);
        putRocket(a, new ItemStack(ModItems.CARGO_ROCKET.get()));
        h.assertTrue(launch(h, a) == CargoFlights.Launch.OUT_OF_FUEL, "ohne Treibstoff gestartet");
        h.assertTrue(flight(h, a) == null, "Flug angelegt");
        h.succeed();
    }

    /** 20.3: Der Frachter nimmt bei flights_left = 0 einen Brennstab aus dem Lager; ein Flug = Hin und Rück. */
    public static void freighterFuelRod(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        terminal(h, B);
        putRocket(a, new ItemStack(ModItems.HEAVY_FREIGHTER.get()));
        h.assertTrue(launch(h, a) == CargoFlights.Launch.OUT_OF_FUEL, "ohne Brennstab gestartet");
        a.insertItem(new ItemStack(item("fuel_rod"), 1), false);
        h.assertTrue(launch(h, a) == CargoFlights.Launch.OK, "mit Brennstab nicht gestartet");
        h.assertTrue(a.count(item("fuel_rod")) == 0, "Brennstab nicht verbraucht");
        int max = CargoRockets.fuelItem(a.hangar(0)).get("flights").getAsInt();
        h.assertTrue(CargoRockets.flightsLeft(a.hangar(0)) == max - 1, "flights_left " + CargoRockets.flightsLeft(a.hangar(0)));
        h.succeed();
    }

    /** 20.3: Methan als Fracht: die Rakete behält, was der Rückflug braucht; der Rest füllt den Treibstofftank am Ziel. */
    public static void keepsMethane(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        CargoTerminalBlockEntity b = terminal(h, B);
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        long leg = CargoRockets.fuelPerLeg(rocket, 0, 0);
        CargoRockets.setFuel(rocket, leg);
        putRocket(a, rocket);
        a.cargoTanks().getFirst().setStackUnchecked(IhFluidStack.of(METHANE, 10000));
        h.assertTrue(launch(h, a) == CargoFlights.Launch.OK, "Start");
        arrive(h, flight(h, a));
        h.assertTrue(b.fuelTank().stack().amount() == 10000 - leg, "Treibstofftank am Ziel " + b.fuelTank().stack());
        CargoFlights.Flight back = flight(h, a);
        h.assertTrue(back != null && back.returning(), "kein Rückflug: " + back);
        h.succeed();
    }

    /** 20.3: Ohne Treibstoff am Ziel wartet die Rakete und versucht es erneut. */
    public static void stranded(GameTestHelper h) {
        CargoTerminalBlockEntity a = terminal(h, A);
        CargoTerminalBlockEntity b = terminal(h, B);
        ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
        long leg = CargoRockets.fuelPerLeg(rocket, 0, 0);
        CargoRockets.setFuel(rocket, leg);
        putRocket(a, rocket);
        h.assertTrue(launch(h, a) == CargoFlights.Launch.OK, "Start");
        arrive(h, flight(h, a));
        CargoFlights.Flight f = flight(h, a);
        h.assertTrue(f != null && !f.returning() && CargoFlights.OUT_OF_FUEL.equals(f.status()), "nicht gestrandet: " + f);
        b.insertFluid(IhFluidStack.of(METHANE, leg), false);
        arrive(h, f);
        CargoFlights.Flight back = flight(h, a);
        h.assertTrue(back != null && back.returning() && CargoFlights.IN_FLIGHT.equals(back.status()), "kein Rückflug: " + back);
        h.assertTrue(b.fuelTank().stack().isEmpty(), "nicht am Ziel getankt");
        h.succeed();
    }
}
