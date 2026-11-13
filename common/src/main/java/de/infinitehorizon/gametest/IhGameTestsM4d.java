package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.FpLedger;
import de.infinitehorizon.space.FuelCalculator;
import de.infinitehorizon.space.FuelPumpBlockEntity;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.ProbeService;
import de.infinitehorizon.space.RocketAssembler;
import de.infinitehorizon.space.RocketTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameTests für M4d (27.7): Treibstoffkosten aus den geladenen Daten (Abnahme 2), Checkliste mit 3 bzw. 4 Flossen
 * (Abnahme 3), Treibstoffpumpe, Sonde zum Mond.
 */
public final class IhGameTestsM4d {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier KEROSENE = Identifier.fromNamespaceAndPath("infinite_horizon", "kerosene");
    private static final Identifier OXYGEN = Identifier.fromNamespaceAndPath("infinite_horizon", "oxygen");
    /** Mitte der 5×5-Plattform im Testbereich. */
    private static final BlockPos CENTER = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROL = new BlockPos(7, 1, 6);
    private static final BlockPos PUMP = new BlockPos(6, 1, 2);
    private static final int TANKS = 3;
    /** Toleranz der Pumpenzeit (27.7: ± 10 %). */
    private static final double TOLERANCE = 0.1;

    private IhGameTestsM4d() {
    }

    /** Abnahme 2: Treibstoffkosten Erde → Mond aus den geladenen PlanetRegistry und BalanceManager = 35.360 mB. */
    public static void fuelCostFromRegistries(GameTestHelper h) {
        PlanetRegistry planets = PlanetRegistry.get();
        long cost = FuelCalculator.cost(FuelCalculator.Body.of(planets.planet(PlanetResolver.EARTH).orElseThrow()),
                FuelCalculator.Body.of(planets.planet(MOON).orElseThrow()), FuelCalculator.Constants.from(BalanceManager.get()));
        h.assertTrue(cost == 35_360, "Erde → Mond: " + cost + " mB");
        h.succeed();
    }

    /** Rampe (5×5, Turm 5 hoch, Pumpe) mit Rakete T1: Triebwerk, 3 Tanks, Kapsel; Flossen nach Wunsch. */
    private static void buildPad(GameTestHelper h, int fins, boolean fullTanks) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                h.setBlock(CENTER.offset(dx, 0, dz), ModBlocks.LAUNCH_PAD.get());
            }
        }
        BlockPos towerBase = CENTER.offset(3, 0, 0);
        for (int y = 0; y < TANKS + 2; y++) {
            h.setBlock(towerBase.above(y), ModBlocks.LAUNCH_TOWER.get());
        }
        h.setBlock(PUMP, ModBlocks.FUEL_PUMP.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        h.setBlock(CONTROL, ModBlocks.LAUNCH_CONTROL.get().defaultBlockState());
        BlockPos engine = CENTER.above();
        h.setBlock(engine, ModBlocks.ROCKET_ENGINE_T1.get());
        for (int i = 1; i <= TANKS; i++) {
            h.setBlock(engine.above(i), ModBlocks.ROCKET_TANK_T1.get());
            if (fullTanks) {
                RocketTankBlockEntity t = h.getBlockEntity(engine.above(i), RocketTankBlockEntity.class);
                t.setFuel(0, t.capacity());
                t.setFuel(1, t.capacity());
            }
        }
        h.setBlock(engine.above(TANKS + 1), ModBlocks.ROCKET_CAPSULE_T1.get());
        Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < fins; i++) {
            h.setBlock(engine.relative(sides[i]), ModBlocks.ROCKET_FIN_T1.get());
        }
    }

    private static LaunchChecklist.State check(GameTestHelper h) {
        return h.getBlockEntity(CONTROL, LaunchControlBlockEntity.class).evaluate(h.getLevel());
    }

    /**
     * Abnahme 3: Vollständige Rampe mit Rakete T1 (3 volle Tanks) und nur 3 Flossen, dazu ein sitzender Spieler mit
     * Anzug: „Struktur gültig“ fehlt („3 von 4 Flossen“), der Start wird abgelehnt, alle Blöcke bleiben; mit der 4.
     * Flosse sind alle Punkte erfüllt.
     */
    public static void checklistNeedsFourFins(GameTestHelper h) {
        buildPad(h, 3, true);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 6.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
        BlockPos capsule = h.absolutePos(CENTER.above(TANKS + 2));
        h.getLevel().getBlockState(capsule).useWithoutItem(h.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(capsule), Direction.NORTH, capsule, false));
        h.assertTrue(p.isPassenger(), "Spieler sitzt nicht in der Kapsel");

        LaunchChecklist.State s = check(h);
        h.assertFalse(s.ok(LaunchChecklist.STRUCTURE), "Struktur gilt trotz 3 Flossen");
        LaunchChecklist.Line structure = s.lines().getFirst();
        h.assertTrue(new RocketAssembler.Problem("fins", 3, 4).equals(structure.problem()),
                "erster Fehler: " + structure.problem());
        h.assertFalse(s.ready(), "Start mit 3 Flossen möglich");
        for (String key : List.of(LaunchChecklist.THRUST, LaunchChecklist.FUEL, LaunchChecklist.TARGET, LaunchChecklist.CREW)) {
            h.assertTrue(s.ok(key), "Punkt " + key + " offen: " + s.lines());
        }
        h.assertBlockPresent(ModBlocks.ROCKET_CAPSULE_T1.get(), CENTER.above(TANKS + 2));
        h.assertBlockPresent(ModBlocks.ROCKET_ENGINE_T1.get(), CENTER.above());

        h.setBlock(CENTER.above().relative(Direction.WEST), ModBlocks.ROCKET_FIN_T1.get());
        LaunchChecklist.State full = check(h);
        h.assertTrue(full.ready(), "mit 4 Flossen offen: " + full.lines());
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /**
     * 27.7 M4d: Die Pumpe füllt 3 leere {@code rocket_tank_t1} aus einem {@code fluid_tank} (Kerosin) und einer
     * {@code pressure_bottle} (Sauerstoff) gleichmäßig; nach 48.000 / {@code transfer_per_tick} t (± 10 %) sind alle voll.
     */
    public static void fuelPumpFillsEvenly(GameTestHelper h) {
        buildPad(h, 4, false);
        BlockPos keroPos = PUMP.east();
        BlockPos oxyPos = PUMP.north();
        h.setBlock(keroPos, ModBlocks.FLUID_TANK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        h.setBlock(oxyPos, ModBlocks.PRESSURE_BOTTLE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        FluidTankBlockEntity kero = h.getBlockEntity(keroPos, FluidTankBlockEntity.class);
        FluidTankBlockEntity oxy = h.getBlockEntity(oxyPos, FluidTankBlockEntity.class);
        kero.refreshLayout();
        oxy.refreshLayout();
        long perTick = BalanceManager.get().entry("machines", "infinite_horizon:fuel_pump")
                .map(o -> o.get("transfer_per_tick").getAsLong()).orElseThrow();
        List<RocketTankBlockEntity> tanks = new java.util.ArrayList<>();
        for (int i = 1; i <= TANKS; i++) {
            tanks.add(h.getBlockEntity(CENTER.above().above(i), RocketTankBlockEntity.class));
        }
        long total = tanks.getFirst().capacity() * TANKS;
        long expected = total / perTick;
        AtomicInteger fullAt = new AtomicInteger(-1);
        h.onEachTick(() -> {
            // Quellen voll halten (fluid_tank fasst weniger als die 3 Raketentanks)
            kero.tank().setStackUnchecked(IhFluidStack.of(KEROSENE, kero.tank().capacity()));
            oxy.tank().setStackUnchecked(IhFluidStack.of(OXYGEN, oxy.tank().capacity()));
            // gleichmäßig je Komponente auf alle Raketentanks
            for (int c = 0; c < RocketTankBlockEntity.COMPONENTS; c++) {
                long min = Long.MAX_VALUE;
                long max = 0;
                for (RocketTankBlockEntity t : tanks) {
                    min = Math.min(min, t.fuel(c));
                    max = Math.max(max, t.fuel(c));
                }
                h.assertTrue(max - min <= perTick, "Komponente " + c + " ungleich verteilt: " + min + " … " + max);
            }
            if (fullAt.get() < 0 && tanks.stream().allMatch(RocketTankBlockEntity::isFull)) {
                fullAt.set((int) h.getTick());
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(fullAt.get() >= 0, "noch nicht voll");
            int t = fullAt.get();
            h.assertTrue(Math.abs(t - expected) <= expected * TOLERANCE, "voll nach " + t + " t, erwartet " + expected);
            h.assertTrue(h.getBlockEntity(PUMP, FuelPumpBlockEntity.class).targets().size() == TANKS, "Pumpe kennt die Tanks nicht");
        });
    }

    /** 27.7 M4d: Eine Sonde zum Mond ist nach höchstens 1.320 t angekommen ({@code probe_reached}, +10 FP). */
    public static void probeReachesMoon(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        AgencyData data = AgencyData.get(server);
        Agency agency = data.ensureFor(UUID.randomUUID(), 0L);
        agency.progress().addProbeTarget(MOON);
        h.assertTrue(ProbeService.canTarget(agency, MOON), "Mond ist kein Sondenziel");
        int before = agency.progress().fp();
        ProbeService.startFlight(server, agency, MOON);
        int fp = BalanceManager.get().entry("research", "probe_new_target").map(o -> o.get("fp").getAsInt()).orElseThrow();
        h.succeedWhen(() -> {
            h.assertTrue(agency.progress().probeReached().contains(MOON), "Sonde noch unterwegs");
            h.assertTrue(agency.progress().fp() == before + FpLedger.value(fp, 0, FpLedger.rules()), "FP " + agency.progress().fp());
            h.assertTrue(agency.progress().probeFlights().isEmpty(), "Flug nicht abgeschlossen");
        });
    }
}
