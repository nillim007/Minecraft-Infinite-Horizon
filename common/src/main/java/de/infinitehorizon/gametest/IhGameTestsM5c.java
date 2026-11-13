package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.FuelCalculator;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.RocketAssembler;
import de.infinitehorizon.space.RocketEntity;
import de.infinitehorizon.space.RocketTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** GameTests für M5c (12.4): Rakete T2 nur von der 7×7-Rampe, Flug zum Mars. */
public final class IhGameTestsM5c {

    private static final Identifier MARS = Identifier.fromNamespaceAndPath("infinite_horizon", "mars");
    private static final BlockPos CENTER = new BlockPos(5, 1, 5);
    private static final BlockPos CONTROL = new BlockPos(5, 1, 12);
    private static final int TANKS = 4;

    private IhGameTestsM5c() {
    }

    /** Rampe mit Kantenlänge {@code edge}, Turm, Pumpe, Pult und Rakete T2 (Triebwerk, 4 volle Tanks, Kapsel, 4 Flossen). */
    private static void build(GameTestHelper h, int edge) {
        int half = edge / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                h.setBlock(CENTER.offset(dx, 0, dz), ModBlocks.LAUNCH_PAD.get());
            }
        }
        BlockPos tower = CENTER.offset(half + 1, 0, 0);
        for (int y = 0; y < TANKS + 2; y++) {
            h.setBlock(tower.above(y), ModBlocks.LAUNCH_TOWER.get());
        }
        h.setBlock(tower.north(), ModBlocks.FUEL_PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        h.setBlock(CONTROL, ModBlocks.LAUNCH_CONTROL.get().defaultBlockState());
        BlockPos engine = CENTER.above();
        h.setBlock(engine, ModBlocks.ROCKET_ENGINE_T2.get());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            h.setBlock(engine.relative(d), ModBlocks.ROCKET_FIN_T2.get());
        }
        for (int i = 1; i <= TANKS; i++) {
            h.setBlock(engine.above(i), ModBlocks.ROCKET_TANK_T2.get());
            RocketTankBlockEntity t = h.getBlockEntity(engine.above(i), RocketTankBlockEntity.class);
            t.setFuel(0, t.capacity());
            t.setFuel(1, t.capacity());
        }
        h.setBlock(engine.above(TANKS + 1), ModBlocks.ROCKET_CAPSULE_T2.get());
    }

    private static ServerPlayer pilot(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 12.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
        AgencyManager.ensureFor(p).progress().addProbeReached(MARS);
        BlockPos capsule = h.absolutePos(CENTER.above(TANKS + 2));
        h.getLevel().getBlockState(capsule).useWithoutItem(h.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(capsule), Direction.NORTH, capsule, false));
        h.assertTrue(p.isPassenger(), "Spieler sitzt nicht in der Kapsel");
        return p;
    }

    /** 12.4: Rakete T2 auf der 5×5-Rampe: „Struktur gültig“ fehlt (Plattform 25 von 49). */
    public static void t2NeedsSevenPad(GameTestHelper h) {
        build(h, 5);
        LaunchChecklist.State s = h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class).evaluate(h.getLevel());
        h.assertFalse(s.ok(LaunchChecklist.STRUCTURE), "T2 auf 5×5 gültig");
        h.assertTrue(new RocketAssembler.Problem("platform", 25, 49).equals(s.lines().getFirst().problem()),
                "erster Fehler: " + s.lines().getFirst().problem());
        h.succeed();
    }

    /** Rakete T2 (4 volle Tanks) von der 7×7-Rampe zum Mars: Treibstoff je Komponente = voll − Kosten Erde → Mars. */
    public static void t2FlightToMars(GameTestHelper h) {
        build(h, 7);
        ServerPlayer p = pilot(h);
        LaunchControlBlockEntity control = h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class);
        LaunchChecklist.State s = control.evaluate(h.getLevel());
        h.assertTrue(s.ready(), "Checkliste: " + s.lines());
        long full = s.rocket().usableFuel();
        String error = FlightService.launchFromPad(h.getLevel(), control, p, MARS, FlightService.Landing.DEFAULT);
        h.assertTrue(error == null, "Start abgelehnt: " + error);
        RocketEntity r = FlightService.complete((RocketEntity) p.getVehicle());
        h.assertTrue(r.level().dimension().identifier().equals(MARS), "nicht auf dem Mars: " + r.level().dimension());
        PlanetRegistry planets = PlanetRegistry.get();
        long cost = FuelCalculator.cost(FuelCalculator.Body.of(planets.planet(PlanetResolver.EARTH).orElseThrow()),
                FuelCalculator.Body.of(planets.planet(MARS).orElseThrow()), FuelCalculator.Constants.from(BalanceManager.get()));
        h.assertTrue(r.fuel(0) == full - cost && r.fuel(1) == full - cost,
                "Treibstoff " + r.fuel(0) + "/" + r.fuel(1) + ", erwartet " + (full - cost));
        h.assertTrue(p.getVehicle() == r && r.seats() == 2, "Sitz: " + p.getVehicle() + ", Sitze " + r.seats());
        FlightService.disassemble((net.minecraft.server.level.ServerLevel) r.level(), r);
        IhTestPlayers.remove(p);
        h.succeed();
    }
}
