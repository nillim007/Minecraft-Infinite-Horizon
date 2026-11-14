package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.food.GreenhouseBlockEntity;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.SolarPanelBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.RocketEntity;
import de.infinitehorizon.station.StationCoreBlockEntity;
import de.infinitehorizon.station.StationModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** GameTests für M6b (14.2): Stationsmodule. */
public final class IhGameTestsM6b {

    private static final Identifier EARTH_ORBIT = InfiniteHorizon.id("earth_orbit");
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private IhGameTestsM6b() {
    }

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, BlockPos rel, Block block, Class<T> type) {
        h.setBlock(rel, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(rel, type);
        be.refreshLayout();
        return be;
    }

    /** Stationskern mit vollem O₂-Tank: seine Kuppel macht den Bereich atembar (14.1). */
    private static StationCoreBlockEntity core(GameTestHelper h) {
        StationCoreBlockEntity be = machine(h, CORE, ModBlocks.STATION_CORE.get(), StationCoreBlockEntity.class);
        be.tank().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("oxygen"), be.tank().capacity()));
        return be;
    }

    private static boolean domeActive(GameTestHelper h) {
        return DomeData.get(h.getLevel()).at(h.absolutePos(CORE)) != null;
    }

    /** Tankdepot: 256.000 mB (storage.json), auch außerhalb des Orbits (6.3). */
    public static void tankDepotCapacity(GameTestHelper h) {
        FluidTankBlockEntity be = machine(h, new BlockPos(2, 1, 2), ModBlocks.TANK_DEPOT.get(), FluidTankBlockEntity.class);
        long want = BalanceManager.get().entry("storage", InfiniteHorizon.id("tank_depot").toString()).orElseThrow()
                .get("capacity").getAsLong();
        h.assertTrue(be.fluidTanks().getFirst().capacity() == want, "Kapazität " + be.fluidTanks().getFirst().capacity());
        h.succeed();
    }

    /** Solarflügel im Orbit: {@code output_per_tick.orbit} × Solarfaktor; auf der Erde nichts. */
    public static void solarWingOrbit(GameTestHelper h) {
        SolarPanelBlockEntity be = machine(h, new BlockPos(2, 2, 2), ModBlocks.SOLAR_WING.get(), SolarPanelBlockEntity.class);
        double want = BalanceManager.get().entry("generators", InfiniteHorizon.id("solar_wing").toString()).orElseThrow()
                .getAsJsonObject("output_per_tick").get("orbit").getAsDouble()
                * de.infinitehorizon.data.PlanetRegistry.get().planet(EARTH_ORBIT).orElseThrow().solarFactor();
        h.assertTrue(Math.abs(be.output(h.getLevel()) - want) < 1e-9, "Leistung im Orbit " + be.output(h.getLevel()));
        h.succeed();
    }

    public static void solarWingSurface(GameTestHelper h) {
        SolarPanelBlockEntity be = machine(h, new BlockPos(2, 2, 2), ModBlocks.SOLAR_WING.get(), SolarPanelBlockEntity.class);
        h.assertTrue(be.output(h.getLevel()) == 0, "Leistung auf der Erde " + be.output(h.getLevel()));
        h.succeed();
    }

    /** Wohnmodul (14.4): Respawnpunkt nur in aktiver Kuppel; dann über dem Modul. */
    public static void habitationSpawnInDome(GameTestHelper h) {
        BlockPos module = new BlockPos(2, 2, 4);
        h.setBlock(module, ModBlocks.HABITATION_MODULE.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 3, 2.5));
        h.assertFalse(StationModules.setSpawn(p, h.absolutePos(module)), "Spawn ohne Kuppel gesetzt");
        core(h);
        h.succeedWhen(() -> {
            h.assertTrue(domeActive(h), "Kuppel nicht aktiv");
            h.assertTrue(StationModules.setSpawn(p, h.absolutePos(module)), "Spawn in der Kuppel nicht gesetzt");
            var cfg = p.getRespawnConfig();
            h.assertTrue(cfg != null && cfg.respawnData().pos().equals(h.absolutePos(module).above())
                    && cfg.respawnData().dimension().equals(h.getLevel().dimension()), "Respawn " + cfg);
            IhTestPlayers.remove(p);
            IhGameTestsM5i.removeDome(h, CORE);
        });
    }

    /** Forschungslabor (14.2): angrenzender Analysator × {@code analyzer_fp_multiplier}, sonst 1. */
    public static void researchLabBonus(GameTestHelper h) {
        BlockPos lab = new BlockPos(3, 2, 3);
        h.setBlock(lab, ModBlocks.RESEARCH_LAB.get());
        double want = BalanceManager.get().entry("research", "research_lab").orElseThrow().get("analyzer_fp_multiplier").getAsDouble();
        h.assertTrue(StationModules.labMultiplier(h.getLevel(), h.absolutePos(lab.east())) == want, "Bonus neben dem Labor");
        h.assertTrue(StationModules.labMultiplier(h.getLevel(), h.absolutePos(lab.east(2))) == 1.0, "Bonus ohne Labor");
        h.succeed();
    }

    /** Gewächshaus (14.2, 17) im Orbit unter der Kuppel: erntet mehrere Pflanzen, verbraucht Trockeneis. */
    public static void greenhouseOrbit(GameTestHelper h) {
        GreenhouseBlockEntity be = machine(h, new BlockPos(2, 2, 2), ModBlocks.GREENHOUSE.get(), GreenhouseBlockEntity.class);
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        be.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"),
                be.fluidTanks().getFirst().capacity()));
        be.setItem(0, new ItemStack(Items.WHEAT_SEEDS));
        be.setItem(1, new ItemStack(Items.BIRCH_SAPLING));
        be.setItem(be.plantSlots(), new ItemStack(ModItems.simple("dry_ice").get(), 2));
        be.setStage(0, GreenhouseBlockEntity.plant(be.getItem(0)).stages());
        be.setStage(1, GreenhouseBlockEntity.plant(be.getItem(1)).stages());
        core(h);
        int logs = BalanceManager.get().entry("agriculture", "saplings").orElseThrow().getAsJsonArray("yield").get(0)
                .getAsJsonObject().get("count").getAsInt();
        h.succeedWhen(() -> {
            h.assertTrue(be.countOutput(Items.WHEAT) > 0, "kein Weizen");
            h.assertTrue(be.countOutput(Items.BIRCH_LOG) == logs, "Birkenstämme " + be.countOutput(Items.BIRCH_LOG));
            h.assertTrue(domeActive(h), "Kuppel nicht aktiv");
            h.assertTrue(be.getItem(be.plantSlots()).getCount() == 1, "Trockeneis " + be.getItem(be.plantSlots()).getCount()
                    + ", Status " + be.status() + "/" + be.statusHint());
            IhGameTestsM5i.removeDome(h, CORE);
        });
    }

    /** Gewächshaus außerhalb des Orbits: Status {@code error}, Hinweis Ort. */
    public static void greenhouseOnlyInOrbit(GameTestHelper h) {
        GreenhouseBlockEntity be = machine(h, new BlockPos(2, 1, 2), ModBlocks.GREENHOUSE.get(), GreenhouseBlockEntity.class);
        be.setItem(0, new ItemStack(Items.WHEAT_SEEDS));
        h.runAfterDelay(10, () -> {
            h.assertTrue(be.status() == de.infinitehorizon.block.MachineStatus.ERROR
                    && be.statusHint() == MachineBlockEntity.HINT_LOCATION, "Status " + be.status() + "/" + be.statusHint());
            h.succeed();
        });
    }

    /**
     * Andockbucht (14.2, 12.2): Rakete T2 fliegt in den Erdorbit mit der Bucht als Ziel, steht darauf und tankt aus dem
     * angrenzenden Tank.
     */
    public static void dockingBayLandingRefuel(GameTestHelper h) {
        IhGameTestsM5c.build(h, 7);
        ServerPlayer p = IhGameTestsM5c.pilot(h);
        ServerLevel orbit = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, EARTH_ORBIT));
        h.assertTrue(orbit != null, "kein Erdorbit");
        JsonObject arrival = BalanceManager.get().entry("spaceflight", "orbit_arrival").orElseThrow();
        BlockPos base = h.absolutePos(new BlockPos(5, 0, 5));
        BlockPos bay = new BlockPos(base.getX(), arrival.get("y").getAsInt(), base.getZ());
        orbit.setBlockAndUpdate(bay, ModBlocks.DOCKING_BAY.get().defaultBlockState());
        orbit.setBlockAndUpdate(bay.east(), ModBlocks.FLUID_TANK.get().defaultBlockState());
        LaunchControlBlockEntity control = h.getBlockEntity(IhGameTestsM5c.CONTROL, LaunchControlBlockEntity.class);
        String error = FlightService.launchFromPad(h.getLevel(), control, p, EARTH_ORBIT, FlightService.Landing.beacon(bay));
        h.assertTrue(error == null, "Start abgelehnt: " + error);
        RocketEntity r = FlightService.complete((RocketEntity) p.getVehicle());
        h.assertTrue(r.level() == orbit && r.blockPosition().equals(bay.above()), "Landung bei " + r.blockPosition()
                + " in " + r.level().dimension());
        long landed = r.fuel(0);
        FluidTankBlockEntity tank = (FluidTankBlockEntity) orbit.getBlockEntity(bay.east());
        tank.refreshLayout();
        tank.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(r.fuelIds().get(0), 1000));
        h.succeedWhen(() -> {
            h.assertTrue(r.fuel(0) > landed, "nicht getankt: " + r.fuel(0) + " (gelandet mit " + landed + ")");
            FlightService.disassemble(orbit, r);
            orbit.setBlockAndUpdate(bay, Blocks.AIR.defaultBlockState());
            orbit.setBlockAndUpdate(bay.east(), Blocks.AIR.defaultBlockState());
            IhTestPlayers.remove(p);
        });
    }
}
