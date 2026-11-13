package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.environment.VenusHeat;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.CloudElevatorBlockEntity;
import de.infinitehorizon.space.CloudLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** GameTests für M5d (9.1, 12.8): Hitzezeit der Venus (Abnahme M5), Wolkenschicht, Aufzug, Gelände. */
public final class IhGameTestsM5d {

    private static final Identifier VENUS = InfiniteHorizon.id("venus");

    private IhGameTestsM5d() {
    }

    private static ServerLevel venus(GameTestHelper h) {
        ServerLevel l = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, VENUS));
        h.assertTrue(l != null, "Dimension Venus fehlt");
        return l;
    }

    /**
     * Abnahme M5 (27): Hitzezeit Venus 3.600 t. Spieler im Hitzeanzug (Override Venus-Oberfläche): vorher keine
     * Hitze-Belastung, nach Ablauf baut sie sich auf.
     */
    public static void venusHeatTime(GameTestHelper h) {
        VenusHeat heat = VenusHeat.rules();
        h.assertTrue(heat != null, "venus_heat_timer fehlt");
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, VENUS, null, null, heat.duration() + 200);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HEAT_SUIT_HELMET.get()));
        ItemStack chest = new ItemStack(ModItems.HEAT_SUIT_CHESTPLATE.get());
        Suits.setOxygen(chest, Suits.supply().capacity());
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HEAT_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.HEAT_SUIT_BOOTS.get()));
        // Toleranz zwei Prüftakte (27)
        h.runAtTickTime(heat.duration() - 20, () -> {
            HazardState s = Services.ATTACHMENTS.hazardState(p);
            h.assertTrue(s.buildup(Hazard.HEAT) == 0 && !heat.expired(s.venusHeat()),
                    "vor Ablauf: Hitze " + s.buildup(Hazard.HEAT) + ", Zeit " + s.venusHeat());
        });
        h.runAtTickTime(heat.duration() + 40, () -> {
            HazardState s = Services.ATTACHMENTS.hazardState(p);
            h.assertTrue(heat.expired(s.venusHeat()) && s.buildup(Hazard.HEAT) > 0,
                    "nach Ablauf: Hitze " + s.buildup(Hazard.HEAT) + ", Zeit " + s.venusHeat());
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
            h.succeed();
        });
    }

    /** 12.8: Ohne festen Block in der Wolkenschicht entsteht eine 5×5-Plattform aus {@code aerostat_platform} auf y 200. */
    public static void aerostatLandingPlatform(GameTestHelper h) {
        ServerLevel venus = venus(h);
        BlockPos at = CloudLayer.landingPoint(venus, 700, 700);
        try {
            h.assertTrue(venus.getBlockState(at.below()).is(ModBlocks.simple("aerostat_platform").get()),
                    "Landepunkt " + at + " über " + venus.getBlockState(at.below()));
            h.assertTrue(at.getY() > 180 && at.getY() < 240, "Landepunkt nicht in der Wolkenschicht: " + at);
            h.assertTrue(CloudLayer.landingPoint(venus, 700, 700).equals(at), "zweite Landung baut neu");
        } finally {
            for (BlockPos p : BlockPos.betweenClosed(at.below().offset(-2, 0, -2), at.below().offset(2, 0, 2))) {
                venus.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        h.succeed();
    }

    /** 12.8: Aufzug oben → Oberfläche unter y 180 mit verknüpfter unterer Station; unten → zurück nach oben. */
    public static void cloudElevatorRide(GameTestHelper h) {
        ServerLevel venus = venus(h);
        BlockPos platform = new BlockPos(720, 200, 720);
        venus.setBlockAndUpdate(platform, ModBlocks.simple("aerostat_platform").get().defaultBlockState());
        BlockPos upper = platform.above();
        venus.setBlockAndUpdate(upper, ModBlocks.CLOUD_ELEVATOR.get().defaultBlockState());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.CREATIVE, new Vec3(1.5, 1, 1.5));
        p.teleport(new TeleportTransition(venus, Vec3.atBottomCenterOf(upper.above()), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        BlockPos lower = null;
        try {
            h.assertTrue(CloudLayer.ride(venus, upper, p), "Fahrt nach unten fehlgeschlagen");
            h.assertTrue(p.getBlockY() < 180, "nicht unten: " + p.blockPosition());
            CloudElevatorBlockEntity up = (CloudElevatorBlockEntity) venus.getBlockEntity(upper);
            lower = up.link();
            h.assertTrue(lower != null && venus.getBlockEntity(lower) instanceof CloudElevatorBlockEntity be
                    && be.lower() && upper.equals(be.link()), "untere Station fehlt oder nicht verknüpft: " + lower);
            h.assertTrue(CloudLayer.ride(venus, lower, p), "Fahrt nach oben fehlgeschlagen");
            h.assertTrue(p.blockPosition().equals(upper.above()), "nicht oben: " + p.blockPosition());
        } finally {
            venus.setBlockAndUpdate(upper, Blocks.AIR.defaultBlockState());
            venus.setBlockAndUpdate(platform, Blocks.AIR.defaultBlockState());
            if (lower != null) {
                venus.setBlockAndUpdate(lower, Blocks.AIR.defaultBlockState());
            }
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** 12.8: Über der Oberfläche bleibt die Wolkenschicht frei (kein Gelände ab y 180). */
    public static void venusTerrainBelowClouds(GameTestHelper h) {
        ServerLevel venus = venus(h);
        for (int x = -256; x <= 256; x += 64) {
            for (int z = -256; z <= 256; z += 64) {
                int top = venus.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                h.assertTrue(top < 180, "Gelände bei " + x + "/" + z + " bis y " + top);
            }
        }
        h.succeed();
    }
}
