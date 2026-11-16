package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.creature.SulfurGlob;
import de.infinitehorizon.creature.SulfurSalamander;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.event.VolcanicEruption;
import de.infinitehorizon.machine.GasCollectorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.space.LanderRoutes;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.station.StationData;
import de.infinitehorizon.station.ZeroGMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M7c (13.3, 13.4, 14.3, 18, 19): Jupiter-Orbit, Gas-Sammler, Io. */
public final class IhGameTestsM7c {

    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final Identifier IO = InfiniteHorizon.id("io");
    private static final Identifier JUPITER_ORBIT = InfiniteHorizon.id("jupiter_orbit");

    private IhGameTestsM7c() {
    }

    private static GasCollectorBlockEntity collector(GameTestHelper h) {
        h.setBlock(MACHINE, ModBlocks.GAS_COLLECTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        GasCollectorBlockEntity be = h.getBlockEntity(MACHINE, GasCollectorBlockEntity.class);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** Gas-Sammler im Jupiter-Orbit (13.3): Wasserstoff, Menge und Dauer aus {@code gas_collector_output}. */
    public static void gasCollectorHydrogen(GameTestHelper h) {
        GasCollectorBlockEntity be = collector(h);
        JsonObject out = GasCollectorBlockEntity.output(h.getLevel(), h.absolutePos(MACHINE));
        h.assertTrue(out != null && out.get("fluid").getAsString().equals(InfiniteHorizon.id("hydrogen").toString()),
                "Gas im Jupiter-Orbit: " + out);
        long amount = out.get("amount").getAsLong();
        h.succeedWhen(() -> {
            var tank = be.fluidTanks().getFirst().stack();
            h.assertTrue(tank.is(InfiniteHorizon.id("hydrogen")) && tank.amount() >= amount, "Tank " + tank);
            h.assertTrue(be.status() == MachineStatus.RUNNING, "Status " + be.status());
        });
    }

    /** Gas-Sammler außerhalb der Gasriesen-Orbits (6.1): {@code error} mit Hinweis auf den Ort. */
    public static void gasCollectorWrongPlace(GameTestHelper h) {
        GasCollectorBlockEntity be = collector(h);
        h.runAfterDelay(10, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + "/" + be.statusHint());
            h.assertTrue(be.fluidTanks().getFirst().stack().isEmpty(), "auf der Erde gesammelt");
            h.succeed();
        });
    }

    /**
     * Fall unter y 0 im Jupiter-Orbit (14.3): ohne Stationskern auf die Ersatzplattform, mit Kern auf den Kern; jeweils
     * mit Schaden.
     */
    public static void gasGiantFall(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        JsonObject giants = BalanceManager.get().entry("stations", "orbit_fall_rules").orElseThrow()
                .getAsJsonObject("gas_giant_orbits");
        var fb = giants.getAsJsonArray("fallback_position");
        BlockPos fallback = new BlockPos(fb.get(0).getAsInt(), fb.get(1).getAsInt(), fb.get(2).getAsInt());
        float damage = giants.get("damage").getAsFloat();
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        BlockPos core = h.absolutePos(new BlockPos(1, 1, 1));
        // ohne Client gilt der Spieler als nicht geladen und damit als unverwundbar
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        try {
            float before = p.getHealth();
            p.teleportTo(p.getX(), -5, p.getZ());
            ZeroGMovement.tick(level.getServer());
            h.assertTrue(p.blockPosition().equals(fallback), "nicht auf der Ersatzplattform: " + p.blockPosition());
            h.assertTrue(level.getBlockState(fallback.below()).is(ModBlocks.STATION_HULL.get()), "keine Plattform");
            h.assertTrue(p.getHealth() <= before - damage + 1e-3, "kein Schaden: " + p.getHealth());
            StationData.get(level).put(core, null);
            p.teleportTo(p.getX(), -5, p.getZ());
            ZeroGMovement.tick(level.getServer());
            h.assertTrue(p.blockPosition().equals(core.above()), "nicht am Stationskern: " + p.blockPosition());
        } finally {
            StationData.get(level).remove(core);
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Schwefelsalamander (18): feuerfest; sein Schwefelklumpen trifft mit dem Angriffsschaden und setzt kein Feuer. */
    public static void salamanderGlob(GameTestHelper h) {
        SulfurSalamander s = h.spawnWithNoFreeWill(ModEntities.SULFUR_SALAMANDER.get(), new Vec3(1.5, 2, 1.5));
        Pig pig = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(1.5, 0, 5.5));
        h.assertTrue(s.fireImmune(), "Salamander nicht feuerfest");
        // ohne Streuung direkt auf das Schwein (shoot() streut wie die Lohe)
        Vec3 from = h.absoluteVec(new Vec3(1.5, 0.4, 3.5));
        SulfurGlob glob = new SulfurGlob(h.getLevel(), s, pig.position().add(0, 0.4, 0).subtract(from).normalize());
        glob.setPos(from);
        h.getLevel().addFreshEntity(glob);
        h.succeedWhen(() -> {
            h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "Schwein nicht getroffen: Klumpen " + h.relativeVec(glob.position())
                    + (glob.isRemoved() ? " entfernt" : "") + ", Schwein " + h.relativeVec(pig.position()) + " " + pig.getHealth());
            h.assertFalse(pig.isOnFire(), "Schwein brennt");
            for (BlockPos b : BlockPos.betweenClosed(h.absolutePos(new BlockPos(0, 1, 3)), h.absolutePos(new BlockPos(3, 4, 8)))) {
                h.assertFalse(h.getLevel().getBlockState(b).is(Blocks.FIRE), "Feuer bei " + b);
            }
        });
    }

    /** Vulkanausbruch (19): Schlote im Umkreis werden gefunden, Brocken aus Io-Schwefelerz fallen im Radius. */
    public static void volcanicEruption(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(4, 1, 4));
        var start = IhGameTestsM4b.placeStructure(level, InfiniteHorizon.id("volcanic_vent"), at);
        h.assertTrue(start.isValid(), "Vulkanschlot nicht gesetzt");
        Structure vent = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(InfiniteHorizon.id("volcanic_vent"));
        level.getChunkAt(start.getBoundingBox().getCenter()).setStartForStructure(vent, start);
        JsonObject e = BalanceManager.get().entry("events", InfiniteHorizon.id("volcanic_eruption").toString()).orElseThrow();
        int search = e.get("chance_search_radius").getAsInt();
        List<BlockPos> found = VolcanicEruption.near(level, vent, at.offset(search / 2, 0, 0), search);
        h.assertTrue(found.size() == 1, "Schlote " + found);
        JsonObject fx = e.getAsJsonObject("effects");
        int radius = fx.get("radius").getAsInt();
        // Brocken fallen nur über geladenen Chunks
        BlockPos v = found.getFirst();
        for (int cx = (v.getX() - radius) >> 4; cx <= (v.getX() + radius) >> 4; cx++) {
            for (int cz = (v.getZ() - radius) >> 4; cz <= (v.getZ() + radius) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        FallingBlockEntity block = null;
        for (int k = 0; k < 10 && block == null; k++) {
            block = VolcanicEruption.drop(level, found.getFirst(), fx, level.getRandom());
        }
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, v.getX(), v.getZ());
        h.assertTrue(block != null, "kein Brocken: Schlot " + v + ", Chunk " + level.hasChunk(v.getX() >> 4, v.getZ() >> 4)
                + ", Höhe " + top + ", dort " + level.getBlockState(new BlockPos(v.getX(), top + 24, v.getZ())) + ", max " + level.getMaxY());
        h.assertTrue(block.getBlockState().is(ModBlocks.simple("io_sulfur_ore").get()), "Brocken " + block.getBlockState());
        double dx = block.getX() - found.getFirst().getX();
        double dz = block.getZ() - found.getFirst().getZ();
        h.assertTrue(Math.sqrt(dx * dx + dz * dz) <= radius + 1, "außerhalb des Radius");
        block.discard();
        h.succeed();
    }

    /** Io (13.2): an Lava gilt die Zone {@code near_lava} mit ihrer Temperatur. */
    public static void ioNearLava(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.LAVA);
        PlanetResolver.Resolved near = PlanetResolver.at(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 1)));
        h.assertTrue(near.planet() != null && near.planet().dimension().equals(IO), "Planet " + near.planet());
        h.assertTrue(near.zones().stream().anyMatch(z -> "near_lava".equals(z.id())), "Zone fehlt: " + near.zones());
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.succeed();
    }

    /** Landefähre (12.7): Io und Europa hängen am Jupiter-Orbit. */
    public static void landerIoRoute(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            Agency agency = AgencyManager.ensureFor(p);
            agency.progress().addProbeReached(IO);
            agency.progress().addProbeReached(InfiniteHorizon.id("europa"));
            List<LaunchChecklist.Destination> d = LanderRoutes.destinations(h.getLevel(), Long.MAX_VALUE,
                    PlanetRegistry.get().planet(JUPITER_ORBIT).orElseThrow(), agency);
            h.assertTrue(d.stream().anyMatch(x -> x.dimension().equals(IO))
                    && d.stream().anyMatch(x -> x.dimension().equals(InfiniteHorizon.id("europa"))), "Ziele vom Jupiter-Orbit: " + d);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
