package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.station.StationCoreBlockEntity;
import de.infinitehorizon.station.StationData;
import de.infinitehorizon.station.StationHullBlock;
import de.infinitehorizon.station.ZeroGMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** GameTests für M6a (14.1, 14.3, 12.5): Orbit-Dimension, Schwerelosigkeit, Magnetstiefel, Stationskern. */
public final class IhGameTestsM6a {

    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private IhGameTestsM6a() {
    }

    private static double gravityPerG() {
        return BalanceManager.get().entry("misc", "gravity").orElseThrow().get("gravity_per_g").getAsDouble();
    }

    /** Im Erdorbit g = 0,005; Magnetstiefel auf festem Grund g = 1, ohne Block darunter wieder 0,005 (12.9, 14.3). */
    public static void orbitGravityMagnetBoots(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        try {
            double orbitG = BalanceManager.get().entry("misc", "gravity").orElseThrow().get("orbit_g").getAsDouble();
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - gravityPerG() * orbitG) < 1e-9,
                    "Schwerkraft im Orbit " + p.getAttributeValue(Attributes.GRAVITY));
            ItemStack boots = new ItemStack(ModItems.HEAT_SUIT_BOOTS.get());
            boots.set(ModDataComponents.SUIT_MODULES.get(), List.of(ZeroGMovement.MAGNET_BOOTS));
            p.setItemSlot(EquipmentSlot.FEET, boots);
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - gravityPerG()) < 1e-9,
                    "Magnetstiefel auf Grund " + p.getAttributeValue(Attributes.GRAVITY));
            h.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - gravityPerG() * orbitG) < 1e-9,
                    "Magnetstiefel frei schwebend " + p.getAttributeValue(Attributes.GRAVITY));
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Fall unter y 0 im Erdorbit (14.3): Teleport auf die Erde an gleicher X/Z mit Langsamem Fall. */
    public static void orbitFallToSurface(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        try {
            int x = p.getBlockX();
            int z = p.getBlockZ();
            p.teleportTo(p.getX(), h.getLevel().getMinY() - 1, p.getZ());
            ZeroGMovement.tick(h.getLevel().getServer());
            h.assertTrue(p.level().dimension().equals(Level.OVERWORLD), "nicht auf der Erde: " + p.level().dimension());
            h.assertTrue(p.getBlockX() == x && p.getBlockZ() == z, "andere X/Z: " + p.blockPosition());
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "kein Langsamer Fall");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Stationskern (14.1): 5×5-Plattform aus generierter Hülle ohne Drop, Eintrag in StationData, Kuppel per Solarzellen. */
    public static void stationCorePlatformDome(GameTestHelper h) {
        h.setBlock(CORE.east(), Blocks.STONE); // Plattform nur an Luftpositionen
        h.setBlock(CORE, ModBlocks.STATION_CORE.get().defaultBlockState()
                .setValue(de.infinitehorizon.machine.MachineBlock.FACING, Direction.NORTH));
        StationCoreBlockEntity be = h.getBlockEntity(CORE, StationCoreBlockEntity.class);
        be.refreshLayout();
        be.tank().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("oxygen"), be.tank().capacity()));
        JsonObject platform = StationCoreBlockEntity.config().getAsJsonObject("platform");
        int r = platform.get("size").getAsInt() / 2;
        int hull = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                var s = h.getBlockState(CORE.offset(dx, 0, dz));
                if (s.is(ModBlocks.STATION_HULL.get()) && s.getValue(StationHullBlock.GENERATED)) {
                    hull++;
                }
            }
        }
        int size = platform.get("size").getAsInt();
        h.assertTrue(hull == size * size - 2, "Hüllblöcke " + hull);
        h.assertTrue(h.getBlockState(CORE.east()).is(Blocks.STONE), "Stein überschrieben");
        BlockPos abs = h.absolutePos(CORE);
        h.assertTrue(StationData.get(h.getLevel()).nearest(abs, 1) != null, "Kern nicht in StationData");
        h.getLevel().destroyBlock(h.absolutePos(CORE.west()), true);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CORE.west())).inflate(2)).isEmpty(),
                "generierte Hülle droppt");
        h.succeedWhen(() -> {
            DomeData.Entry e = DomeData.get(h.getLevel()).at(abs);
            h.assertTrue(e != null && e.radius() == StationCoreBlockEntity.config().getAsJsonObject("builtin_dome")
                    .get("radius").getAsInt(), "eingebaute Kuppel nicht aktiv: " + e + ", Strom " + be.energyStorage().getAmount());
            IhGameTestsM5i.removeDome(h, CORE);
        });
    }

    /** Ohne Zielblock: Kern 3 Blöcke vor dem Spieler (Orbit). Außerhalb des Orbits lässt er sich nicht setzen. */
    public static void stationCoreAirPlacement(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 4.5));
        try {
            p.setYRot(-90); // Blick nach Osten (+X)
            p.setXRot(0);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STATION_CORE.get()));
            InteractionResult res = p.getMainHandItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            BlockPos target = de.infinitehorizon.station.StationCoreItem.airTarget(p);
            h.assertTrue(res.consumesAction(), "Rechtsklick in die Luft: " + res);
            h.assertTrue(h.getLevel().getBlockState(target).is(ModBlocks.STATION_CORE.get()), "kein Kern bei " + target);
            h.assertTrue(p.getMainHandItem().isEmpty(), "Item nicht verbraucht");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Overworld: Stationskern lässt sich nicht setzen (14.1). */
    public static void stationCoreOnlyInOrbit(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STATION_CORE.get()));
            BlockPos abs = h.absolutePos(new BlockPos(2, 1, 2));
            InteractionResult res = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            h.assertFalse(res.consumesAction() && res != InteractionResult.FAIL, "auf der Erde gesetzt: " + res);
            h.assertFalse(h.getLevel().getBlockState(abs.above()).is(ModBlocks.STATION_CORE.get()), "Kern auf der Erde");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Ankunft im Orbit (12.5, 7.): schwebt auf y aus orbit_arrival; ein fremder Kern in der Nähe verschiebt den Punkt. */
    public static void orbitArrivalHover(GameTestHelper h) {
        JsonObject o = BalanceManager.get().entry("spaceflight", "orbit_arrival").orElseThrow();
        BlockPos base = h.absolutePos(new BlockPos(2, 1, 2));
        int min = o.get("min_distance_foreign_station_core").getAsInt();
        BlockPos p1 = FlightService.landingPoint(h.getLevel(), FlightService.Landing.at(base.getX(), base.getZ()), null);
        // andere Tests im selben Orbit können Kerne setzen; geprüft wird die Regel, nicht die genaue Stelle
        h.assertTrue(p1.getY() == o.get("y").getAsInt() && !StationData.get(h.getLevel()).foreignWithin(p1, min, null),
                "Schwebepunkt " + p1);
        BlockPos foreign = new BlockPos(base.getX(), o.get("y").getAsInt(), base.getZ());
        StationData data = StationData.get(h.getLevel());
        data.put(foreign, UUID.randomUUID());
        try {
            BlockPos p2 = FlightService.landingPoint(h.getLevel(), FlightService.Landing.at(base.getX(), base.getZ()), UUID.randomUUID());
            h.assertTrue(p2.distSqr(foreign) >= (double) min * min, "zu nah am fremden Kern: " + p2);
        } finally {
            data.remove(foreign);
        }
        h.succeed();
    }
}
