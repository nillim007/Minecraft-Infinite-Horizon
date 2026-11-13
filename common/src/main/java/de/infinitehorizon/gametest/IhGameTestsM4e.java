package de.infinitehorizon.gametest;

import com.mojang.authlib.GameProfile;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.LandingService;
import de.infinitehorizon.space.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * GameTests für M4e (27.7): Flug zum Mond und zurück mit übersprungener Sequenz, Zerlegen, Landefeuer als Landeziel,
 * Abmelden im Flug.
 */
public final class IhGameTestsM4e {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier OVERWORLD = Level.OVERWORLD.identifier();
    /** 27.7 M4e: Rest je Komponente nach Erde → Mond bzw. zurück (3 volle Tanks T1). */
    private static final long AFTER_MOON = 12_640;
    private static final long AFTER_RETURN = 5_200;
    /** Teile der Testrakete: Triebwerk, 3 Tanks, Kapsel, 4 Flossen. */
    private static final int PART_ITEMS = 9;
    private static final int BEACON_X = 100;
    private static final int BEACON_Z = 100;

    private IhGameTestsM4e() {
    }

    private static ServerLevel moon(GameTestHelper h) {
        ServerLevel moon = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, MOON));
        h.assertTrue(moon != null, "Dimension Mond fehlt");
        return moon;
    }

    /** Spieler im vollständigen Anzug sitzt in der Kapsel der Testrampe; seine Agentur kennt den Mond (Sonde). */
    private static ServerPlayer seatedPilot(GameTestHelper h) {
        IhGameTestsM4d.buildPad(h, 4, true);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 6.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
        AgencyManager.ensureFor(p).progress().addProbeReached(MOON);
        BlockPos capsule = h.absolutePos(IhGameTestsM4d.CENTER.above(IhGameTestsM4d.TANKS + 2));
        h.getLevel().getBlockState(capsule).useWithoutItem(h.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(capsule), Direction.NORTH, capsule, false));
        h.assertTrue(p.isPassenger(), "Spieler sitzt nicht in der Kapsel");
        return p;
    }

    private static RocketEntity launch(GameTestHelper h, ServerPlayer p) {
        LaunchControlBlockEntity control = h.getBlockEntity(IhGameTestsM4d.CONTROL, LaunchControlBlockEntity.class);
        String error = FlightService.launchFromPad(h.getLevel(), control, p, MOON, FlightService.Landing.DEFAULT);
        h.assertTrue(error == null, "Start abgelehnt: " + error);
        h.assertTrue(p.getVehicle() instanceof RocketEntity, "Spieler sitzt nicht in der Rakete");
        return (RocketEntity) p.getVehicle();
    }

    private static void assertOnGround(GameTestHelper h, RocketEntity r, String where) {
        BlockPos below = r.blockPosition().below();
        h.assertTrue(r.level().getBlockState(below).isSolid(), where + ": kein fester Boden unter der Rakete (" + below + ")");
        h.assertFalse(r.inFlight(), where + ": Rakete noch im Flug");
    }

    /** Rakete zerlegen und die Items wieder einsammeln (nichts bleibt liegen). */
    private static int disassemble(RocketEntity r) {
        ServerLevel level = (ServerLevel) r.level();
        AABB box = r.getBoundingBox().inflate(2);
        List<ItemStack> drops = FlightService.disassemble(level, r);
        level.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
        return drops.stream().mapToInt(ItemStack::getCount).sum();
    }

    /**
     * 27.7 M4e: Rakete T1 mit 3 vollen Tanks startet zum Mond (Sequenz übersprungen): Rampenblöcke weg, Rakete im Mond
     * auf festem Boden nahe 0/0, Spieler sitzt, je Komponente 12.640 mB; Rückflug zur Overworld mit 5.200 mB; Zerlegen
     * gibt 9 Teil-Items.
     */
    public static void flightToMoonAndBack(GameTestHelper h) {
        ServerPlayer p = seatedPilot(h);
        RocketEntity rocket = launch(h, p);
        BlockPos engine = IhGameTestsM4d.CENTER.above();
        h.assertBlockPresent(Blocks.AIR, engine);
        h.assertBlockPresent(Blocks.AIR, engine.above(IhGameTestsM4d.TANKS + 1));
        h.assertBlockPresent(Blocks.AIR, engine.relative(Direction.NORTH));
        h.assertBlockPresent(ModBlocks.LAUNCH_PAD.get(), IhGameTestsM4d.CENTER);

        RocketEntity onMoon = FlightService.complete(rocket);
        h.assertTrue(onMoon.level().dimension().identifier().equals(MOON), "Rakete nicht im Mond: " + onMoon.level().dimension());
        assertOnGround(h, onMoon, "Mond");
        int radius = LandingService.Search.from(BalanceManager.get()).radius();
        h.assertTrue(Math.abs(onMoon.getBlockX()) <= radius && Math.abs(onMoon.getBlockZ()) <= radius,
                "Landung zu weit von 0/0: " + onMoon.blockPosition());
        h.assertTrue(p.getVehicle() == onMoon, "Spieler sitzt nicht in der gelandeten Rakete");
        for (int c = 0; c < 2; c++) {
            h.assertTrue(onMoon.fuel(c) == AFTER_MOON, "Komponente " + c + " nach dem Hinflug: " + onMoon.fuel(c) + " mB");
        }

        // Rückflug auf die eigene Rampe (stört keine anderen Tests am Weltspawn)
        BlockPos home = h.absolutePos(IhGameTestsM4d.CENTER);
        String error = FlightService.launchFromRocket((ServerLevel) onMoon.level(), onMoon, p, OVERWORLD,
                FlightService.Landing.at(home.getX(), home.getZ()));
        h.assertTrue(error == null, "Rückflug abgelehnt: " + error);
        RocketEntity back = FlightService.complete(onMoon);
        h.assertTrue(back.level().dimension().equals(Level.OVERWORLD), "Rakete nicht zurück in der Overworld");
        assertOnGround(h, back, "Overworld");
        for (int c = 0; c < 2; c++) {
            h.assertTrue(back.fuel(c) == AFTER_RETURN, "Komponente " + c + " nach dem Rückflug: " + back.fuel(c) + " mB");
        }

        int items = disassemble(back);
        h.assertTrue(items == PART_ITEMS, "Zerlegen gibt " + items + " Teil-Items");
        h.assertFalse(p.isPassenger(), "Spieler sitzt nach dem Zerlegen noch");
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** 27.7 M4e: Mit einem Landefeuer bei X/Z 100/100 auf dem Mond liegt der Landepunkt höchstens 3 Blöcke davon. */
    public static void beaconLandingWithinRadius(GameTestHelper h) {
        ServerLevel moon = moon(h);
        BlockPos beacon = moon.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(BEACON_X, 0, BEACON_Z));
        moon.setBlockAndUpdate(beacon, ModBlocks.LANDING_BEACON.get().defaultBlockState());
        try {
            BlockPos point = FlightService.landingPoint(moon, FlightService.Landing.beacon(beacon));
            int max = BalanceManager.get().entry("spaceflight", "rocket_flight")
                    .map(o -> o.get("beacon_landing_radius").getAsInt()).orElseThrow();
            int dist = Math.max(Math.abs(point.getX() - beacon.getX()), Math.abs(point.getZ() - beacon.getZ()));
            h.assertTrue(dist <= max, "Landepunkt " + point + " ist " + dist + " Blöcke vom Landefeuer " + beacon);
            h.assertFalse(point.getX() == beacon.getX() && point.getZ() == beacon.getZ(), "Landung auf dem Landefeuer");
        } finally {
            moon.setBlockAndUpdate(beacon, Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    /** 27.7 M4e: Wer sich im Flug abmeldet, sitzt beim nächsten Login in der (gelandeten) Rakete. */
    public static void logoutInFlightRelogSeated(GameTestHelper h) {
        ServerPlayer p = seatedPilot(h);
        RocketEntity rocket = launch(h, p);
        h.assertTrue(rocket.inFlight(), "Rakete nicht im Flug");
        GameProfile profile = p.getGameProfile();
        IhTestPlayers.remove(p);

        ServerPlayer again = IhTestPlayers.login(h.getLevel(), profile);
        String loaded = String.valueOf(Services.ATTACHMENTS.pendingFlight(again));
        // Der Join-Hook setzt den Spieler in den nächsten Ticks in die Rakete (FlightService#queueLogin)
        h.succeedWhen(() -> {
            if (!(again.getVehicle() instanceof RocketEntity)) {
                StringBuilder rockets = new StringBuilder();
                for (RocketEntity r : moon(h).getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(RocketEntity.class), r -> true)) {
                    rockets.append(r.getUUID()).append('@').append(r.blockPosition()).append(" flight=").append(r.inFlight())
                            .append(" pass=").append(r.getPassengers().size()).append(" seats=").append(r.seats()).append("; ");
                }
                h.fail("nach dem Login nicht in der Rakete: " + again.getVehicle() + " in " + again.level().dimension().identifier()
                        + " bei " + again.blockPosition() + ", geladen " + loaded + ", Raketen: " + rockets);
            }
            RocketEntity landed = (RocketEntity) again.getVehicle();
            h.assertTrue(landed.level().dimension().identifier().equals(MOON), "Rakete nicht im Mond");
            h.assertFalse(landed.inFlight(), "Rakete noch im Flug");
            de.infinitehorizon.space.PendingFlight pending = Services.ATTACHMENTS.pendingFlight(again);
            h.assertTrue(pending == null || pending.isNone(), "pending_flight nicht geleert");
            disassemble(landed);
            IhTestPlayers.remove(again);
        });
    }
}
