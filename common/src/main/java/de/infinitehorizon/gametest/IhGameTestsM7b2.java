package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.machine.EnergyItems;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.RocketEntity;
import de.infinitehorizon.space.RocketTankBlockEntity;
import de.infinitehorizon.suit.Jetpack;
import de.infinitehorizon.suit.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M7b, Teil 2 (12.4, 14.2, 9.2): Start von der Andockbucht, Fernflug-Modul, Jetpack. */
public final class IhGameTestsM7b2 {

    private static final BlockPos BAY = new BlockPos(5, 1, 5);
    private static final BlockPos CONTROL = new BlockPos(5, 1, 10);

    private IhGameTestsM7b2() {
    }

    /** Rakete auf der Bucht: Triebwerk, 4 Flossen, Tanks (voll), optional Fernflug-Modul, Kapsel. */
    private static void build(GameTestHelper h, boolean t2, boolean longRange, int tanks) {
        h.setBlock(BAY, ModBlocks.DOCKING_BAY.get());
        h.setBlock(CONTROL, ModBlocks.LAUNCH_CONTROL.get());
        BlockPos engine = BAY.above();
        h.setBlock(engine, t2 ? ModBlocks.ROCKET_ENGINE_T2.get() : ModBlocks.ROCKET_ENGINE_T1.get());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            h.setBlock(engine.relative(d), t2 ? ModBlocks.ROCKET_FIN_T2.get() : ModBlocks.ROCKET_FIN_T1.get());
        }
        int y = 1;
        for (int i = 0; i < tanks; i++, y++) {
            h.setBlock(engine.above(y), t2 ? ModBlocks.ROCKET_TANK_T2.get() : ModBlocks.ROCKET_TANK_T1.get());
            RocketTankBlockEntity t = h.getBlockEntity(engine.above(y), RocketTankBlockEntity.class);
            t.setFuel(0, t.capacity());
            t.setFuel(1, t.capacity());
        }
        if (longRange) {
            h.setBlock(engine.above(y), ModBlocks.LONG_RANGE_MODULE.get());
            y++;
        }
        h.setBlock(engine.above(y), t2 ? ModBlocks.ROCKET_CAPSULE_T2.get() : ModBlocks.ROCKET_CAPSULE_T1.get());
    }

    /** Andockbucht als Startort (12.4): T2 startet ohne Rampe, Turm und Pumpe; T1 nicht. */
    public static void dockingBayLaunch(GameTestHelper h) {
        build(h, true, false, 4);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 10.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
        AgencyManager.ensureFor(p);
        BlockPos capsule = h.absolutePos(BAY.above(6));
        h.getLevel().getBlockState(capsule).useWithoutItem(h.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(capsule), Direction.NORTH, capsule, false));
        h.assertTrue(p.isPassenger(), "Spieler sitzt nicht");
        LaunchControlBlockEntity control = h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class);
        LaunchChecklist.State s = control.evaluate(h.getLevel());
        h.assertTrue(s.ready(), "Checkliste: " + s.lines());
        String error = FlightService.launchFromPad(h.getLevel(), control, p, PlanetResolver.EARTH, FlightService.Landing.DEFAULT);
        h.assertTrue(error == null, "Start abgelehnt: " + error);
        h.assertTrue(h.getBlockState(BAY).is(ModBlocks.DOCKING_BAY.get()), "Bucht weg");
        RocketEntity r = FlightService.complete((RocketEntity) p.getVehicle());
        h.assertTrue(r.level().dimension().identifier().equals(PlanetResolver.EARTH), "nicht auf der Erde");
        FlightService.disassemble((net.minecraft.server.level.ServerLevel) r.level(), r);
        IhTestPlayers.remove(p);
        h.succeed();
    }

    public static void dockingBayRejectsT1(GameTestHelper h) {
        build(h, false, false, 3);
        LaunchChecklist.State s = h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class).evaluate(h.getLevel());
        h.assertFalse(s.ready(), "T1 auf der Bucht bereit");
        h.assertTrue(s.pad() != null && s.pad().problems().stream().anyMatch(pr -> pr.key().equals("site")),
                "kein Hinweis Startort: " + (s.pad() == null ? "keine Rampe" : s.pad().problems()));
        h.succeed();
    }

    /** Fernflug-Modul (12.4): + {@code fuel_capacity} je Komponente, Rolle LONG_RANGE in der Rakete. */
    public static void longRangeModule(GameTestHelper h) {
        build(h, true, true, 4);
        LaunchChecklist.State s = h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class).evaluate(h.getLevel());
        h.assertTrue(s.rocket() != null, "keine Rakete erkannt: " + s.lines());
        long module = RocketPartRegistry.get().part(InfiniteHorizon.id("long_range_module")).orElseThrow().fuelCapacity();
        long tank = RocketPartRegistry.get().part(InfiniteHorizon.id("rocket_tank_t2")).orElseThrow().fuelCapacity();
        h.assertTrue(s.rocket().capacity() == 4 * tank + module, "Fassungsvermögen " + s.rocket().capacity());
        h.assertTrue(s.rocket().stack().stream().anyMatch(p -> p.role() == RocketPartRegistry.Role.LONG_RANGE), "kein Fernflug-Modul");
        h.succeed();
    }

    /** Andockbucht füllt die Tanks einer Rakete im Bau aus einem angrenzenden Tank. */
    public static void dockingBayFillsTanks(GameTestHelper h) {
        build(h, true, false, 4);
        BlockPos firstTank = BAY.above(2);
        RocketTankBlockEntity t = h.getBlockEntity(firstTank, RocketTankBlockEntity.class);
        t.setFuel(0, 0);
        h.setBlock(BAY.east(), ModBlocks.FLUID_TANK.get());
        var source = h.getBlockEntity(BAY.east(), de.infinitehorizon.machine.FluidTankBlockEntity.class);
        source.refreshLayout();
        source.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("hydrogen"), 1000));
        h.succeedWhen(() -> h.assertTrue(t.fuel()[0] > 0, "Tank nicht gefüllt"));
    }

    /** Jetpack (9.2): Energie wandert beim Ein- und Ausbau mit; Ladeslot erkennt die Brustplatte; Springen verbraucht Energie. */
    public static void jetpackEnergy(GameTestHelper h) {
        ItemStack jet = new ItemStack(ModItems.simple("jetpack").get());
        long cap = EnergyItems.componentCapacity(jet);
        h.assertTrue(cap == Jetpack.config().get("energy_capacity").getAsLong(), "Kapazität Jetpack " + cap);
        jet.set(ModDataComponents.ENERGY.get(), 1000L);
        ItemStack chest = new ItemStack(ModItems.HEAT_SUIT_CHESTPLATE.get());
        var plan = SuitModules.plan(List.of(chest, jet)).orElseThrow();
        ItemStack fitted = plan.results().getFirst();
        h.assertTrue(Jetpack.energy(fitted) == 1000 && EnergyItems.componentCapacity(fitted) == cap, "Einbau: " + fitted);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 3, 1.5));
        try {
            p.setItemSlot(EquipmentSlot.CHEST, fitted);
            p.setLastClientInput(new Input(false, false, false, false, true, false, false));
            Jetpack.tick(h.getLevel().getServer());
            long per = Jetpack.config().get("energy_per_tick").getAsLong();
            h.assertTrue(Jetpack.energy(p.getItemBySlot(EquipmentSlot.CHEST)) == 1000 - per,
                    "Energie nach einem Tick " + Jetpack.energy(p.getItemBySlot(EquipmentSlot.CHEST)));
            var back = SuitModules.plan(List.of(p.getItemBySlot(EquipmentSlot.CHEST).copy())).orElseThrow().results();
            h.assertTrue(back.size() == 2 && Jetpack.energy(back.get(1)) == 1000 - per && Jetpack.energy(back.get(0)) == 0,
                    "Ausbau: " + back);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
