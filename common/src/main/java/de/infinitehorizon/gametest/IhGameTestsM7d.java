package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.creature.GeyserMoth;
import de.infinitehorizon.creature.MethaneSkimmer;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.dome.DomeGeneratorBlockEntity;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.event.EventService;
import de.infinitehorizon.machine.AntimatterCellItem;
import de.infinitehorizon.machine.GasCollectorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MagneticTrapBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.ResearchService;
import de.infinitehorizon.space.LanderRoutes;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.station.ZeroGMovement;
import de.infinitehorizon.world.AmmoniaGeyserBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** GameTests für M7d (13.3, 13.4, 6.1, 10.1, 11.4, 18, 19): Saturn-Orbit, Titan, Enceladus, Magnetfalle, Kuppel T3. */
public final class IhGameTestsM7d {

    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final Identifier SATURN_ORBIT = InfiniteHorizon.id("saturn_orbit");
    private static final Identifier TITAN = InfiniteHorizon.id("titan");
    private static final Identifier ENCELADUS = InfiniteHorizon.id("enceladus");

    private IhGameTestsM7d() {
    }

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, Block block, Class<T> type) {
        h.setBlock(MACHINE, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(MACHINE, type);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** Gas-Sammler im Saturn-Orbit (13.3): Helium. */
    public static void saturnHelium(GameTestHelper h) {
        GasCollectorBlockEntity be = machine(h, ModBlocks.GAS_COLLECTOR.get(), GasCollectorBlockEntity.class);
        JsonObject out = GasCollectorBlockEntity.output(h.getLevel(), h.absolutePos(MACHINE));
        h.assertTrue(out != null && out.get("fluid").getAsString().equals(InfiniteHorizon.id("helium").toString()), "Gas " + out);
        h.succeedWhen(() -> h.assertTrue(be.fluidTanks().getFirst().stack().is(InfiniteHorizon.id("helium")), "kein Helium"));
    }

    /** Stufe 5 (11.4): öffnet sich, sobald ein Mitglied den Saturn-Orbit betritt. */
    public static void tierFiveSaturn(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            Agency agency = AgencyManager.ensureFor(p);
            for (int t = 1; t <= 4; t++) {
                agency.progress().openTier(t);
            }
            h.assertFalse(agency.progress().openTiers().contains(5), "Stufe 5 schon offen");
            AgencyManager.onDimensionChange(p, ResourceKey.create(Registries.DIMENSION, SATURN_ORBIT));
            ResearchService.checkGates(h.getLevel().getServer(), agency, Set.of());
            h.assertTrue(agency.progress().openTiers().contains(5), "Stufe 5 nicht offen");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Ringtrümmer (19): Einschlag wie ein Meteorit, aber ohne Erz. */
    public static void ringDebris(GameTestHelper h) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        JsonObject e = BalanceManager.get().entry("events", EventService.RING_DEBRIS.toString()).orElseThrow();
        JsonObject fx = e.getAsJsonObject("effects").deepCopy();
        fx.addProperty("impact_radius", 0);
        BlockPos land = EventService.impact(h.getLevel(), h.absolutePos(new BlockPos(4, 1, 4)), fx, EventService.RING_DEBRIS);
        h.assertFalse(h.getLevel().getBlockState(land).is(ModBlocks.simple("meteorite_ore").get()), "Erz nach Ringtrümmern");
        h.succeed();
    }

    /** Titan (13.4): Methan ist Fluid der Seen; der Methanläufer steht darauf und flieht vor Spielern. */
    public static void methaneSkimmer(GameTestHelper h) {
        Block methane = ModFluids.get("methane").block().get();
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                h.setBlock(new BlockPos(x, 0, z), methane);
            }
        }
        MethaneSkimmer s = h.spawn(ModEntities.METHANE_SKIMMER.get(), new Vec3(4.5, 1.2, 4.5));
        BlockPos under = h.absolutePos(new BlockPos(4, 0, 4));
        var fs = h.getLevel().getFluidState(under);
        h.assertTrue(s.canStandOnFluid(fs), "steht nicht auf " + fs + " / " + h.getLevel().getBlockState(under));
        h.assertFalse(h.getLevel().getBlockState(under).getCollisionShape(h.getLevel(), under,
                net.minecraft.world.phys.shapes.CollisionContext.of(s)).isEmpty(), "keine Kollision über Methan");
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 3, 2.5));
        p.setNoGravity(true);
        double start = s.distanceTo(p);
        h.runAfterDelay(30, () -> {
            try {
                // steht auf der Fluidoberfläche (halbe Blockhöhe), nicht auf dem Boden darunter
                h.assertTrue(s.getY() > h.absoluteVec(new Vec3(0, 0.3, 0)).y, "Läufer versinkt: " + h.relativeVec(s.position()));
                h.assertTrue(s.distanceTo(p) > start, "flieht nicht: " + start + " → " + s.distanceTo(p));
            } finally {
                IhTestPlayers.remove(p);
                s.discard();
            }
            h.succeed();
        });
    }

    /** Titan (13.4): Beim Gleiten mit Elytren gilt die Schwerkraft × {@code elytra_sink_rate_multiplier}. */
    public static void titanElytra(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 3, 1.5));
        try {
            var planet = PlanetRegistry.get().planet(TITAN).orElseThrow();
            double g = planet.gravity();
            h.assertTrue(Math.abs(ZeroGMovement.gravity(p, planet, g) - g) < 1e-9, "ohne Gleiten verändert");
            p.startFallFlying();
            double mult = planet.raw().getAsJsonObject("special").get("elytra_sink_rate_multiplier").getAsDouble();
            h.assertTrue(Math.abs(ZeroGMovement.gravity(p, planet, g) - g * mult) < 1e-9, "Gleiten: " + ZeroGMovement.gravity(p, planet, g));
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Ammoniak-Geysir (13.4): Die Fontäne hebt Entities mit {@code entity_velocity_up} an. */
    public static void geyserLaunch(GameTestHelper h) {
        BlockPos geyser = new BlockPos(2, 1, 2);
        h.setBlock(geyser, ModBlocks.AMMONIA_GEYSER.get());
        Entity pig = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(2.5, 2, 2.5));
        List<Entity> hit = AmmoniaGeyserBlock.erupt(h.getLevel(), h.absolutePos(geyser), true);
        double v = AmmoniaGeyserBlock.config().get("entity_velocity_up").getAsDouble();
        h.assertTrue(hit.contains(pig) && pig.getDeltaMovement().y >= v - 1e-9, "nicht angehoben: " + pig.getDeltaMovement());
        h.assertTrue(AmmoniaGeyserBlock.fountainTicks() > 0, "keine Fontäne");
        h.assertTrue(h.getBlockState(geyser).getDestroySpeed(h.getLevel(), h.absolutePos(geyser)) < 0, "Geysir zerstörbar");
        pig.discard();
        h.succeed();
    }

    /** Geysirmotte (18): findet den Geysir und kreist im Umkreis {@code circle_radius} darum. */
    public static void geyserMoth(GameTestHelper h) {
        BlockPos geyser = new BlockPos(5, 1, 5);
        h.setBlock(geyser, ModBlocks.AMMONIA_GEYSER.get());
        GeyserMoth m = h.spawn(ModEntities.GEYSER_MOTH.get(), new Vec3(1.5, 4, 1.5));
        h.assertTrue(h.absolutePos(geyser).equals(m.findGeyser(h.getLevel())), "Geysir nicht gefunden");
        double r = de.infinitehorizon.creature.CreatureRules.num(de.infinitehorizon.creature.CreatureRules.params(m.getType()),
                "circle_radius");
        h.runAfterDelay(80, () -> {
            Vec3 c = Vec3.atCenterOf(h.absolutePos(geyser));
            double d = Math.hypot(m.getX() - c.x, m.getZ() - c.z);
            m.discard();
            h.assertTrue(Math.abs(d - r) < 2, "Abstand zum Geysir " + d + ", soll " + r);
            h.succeed();
        });
    }

    /** Magnetfalle (6.1): nur im Jupiter-Orbit; füllt eine leere Zelle, eine volle nicht. */
    public static void magneticTrap(GameTestHelper h) {
        MagneticTrapBlockEntity be = machine(h, ModBlocks.MAGNETIC_TRAP.get(), MagneticTrapBlockEntity.class);
        ItemStack empty = new ItemStack(ModItems.ANTIMATTER_CELL.get());
        ItemStack full = new ItemStack(ModItems.ANTIMATTER_CELL.get());
        full.set(ModDataComponents.CHARGE.get(), 1);
        h.assertTrue(AntimatterCellItem.charged(full) && !AntimatterCellItem.charged(empty), "Komponente charge");
        int ticks = BalanceManager.get().entry("machines", InfiniteHorizon.id("magnetic_trap").toString()).orElseThrow()
                .get("ticks").getAsInt();
        h.assertTrue(be.durationFor(h.getLevel(), empty) == ticks, "Dauer " + be.durationFor(h.getLevel(), empty));
        h.assertTrue(be.durationFor(h.getLevel(), full) == -1, "volle Zelle wird gefüllt");
        h.succeed();
    }

    /** Magnetfalle außerhalb des Jupiter-Orbits: {@code error}. */
    public static void magneticTrapWrongPlace(GameTestHelper h) {
        MagneticTrapBlockEntity be = machine(h, ModBlocks.MAGNETIC_TRAP.get(), MagneticTrapBlockEntity.class);
        be.setItem(0, new ItemStack(ModItems.ANTIMATTER_CELL.get()));
        h.runAfterDelay(10, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + "/" + be.statusHint());
            h.succeed();
        });
    }

    /** Kuppelgenerator III (10.1): Radius und Stufe aus domes.json. */
    public static void domeTierThree(GameTestHelper h) {
        h.setBlock(MACHINE, ModBlocks.DOME_GENERATOR_T3.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        DomeGeneratorBlockEntity be = h.getBlockEntity(MACHINE, DomeGeneratorBlockEntity.class);
        JsonObject t3 = BalanceManager.get().entry("domes", InfiniteHorizon.id("dome_generator_t3").toString()).orElseThrow();
        h.assertTrue(be.tier() == t3.get("tier").getAsInt() && be.radius() == t3.get("radius").getAsInt(),
                "Stufe " + be.tier() + ", Radius " + be.radius());
        h.succeed();
    }

    /** Landefähre (12.7): Titan und Enceladus hängen am Saturn-Orbit. */
    public static void landerSaturn(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            Agency agency = AgencyManager.ensureFor(p);
            agency.progress().addProbeReached(TITAN);
            agency.progress().addProbeReached(ENCELADUS);
            List<LaunchChecklist.Destination> d = LanderRoutes.destinations(h.getLevel(), Long.MAX_VALUE,
                    PlanetRegistry.get().planet(SATURN_ORBIT).orElseThrow(), agency);
            h.assertTrue(d.stream().anyMatch(x -> x.dimension().equals(TITAN)) && d.stream().anyMatch(x -> x.dimension().equals(ENCELADUS)),
                    "Ziele vom Saturn-Orbit: " + d);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Enceladus (13.4): Feature und Geysir-Biom sind registriert; Titan hat Methan als Fluid. */
    public static void moonsWorldgen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getValue(InfiniteHorizon.id("enceladus_geyser")) != null, "Geysir-Feature fehlt");
        h.assertTrue(PlanetResolver.planetFor(TITAN, PlanetRegistry.get()) != null, "Titan fehlt");
        h.succeed();
    }
}
