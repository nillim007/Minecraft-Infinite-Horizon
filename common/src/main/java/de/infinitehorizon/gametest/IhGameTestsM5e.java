package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.vehicle.RoverData;
import de.infinitehorizon.vehicle.RoverEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M5e (16): Rover-Bausatz, Laden, Solardach, Bohrer, Druckkabine. */
public final class IhGameTestsM5e {

    private IhGameTestsM5e() {
    }

    private static RoverEntity rover(GameTestHelper h, BlockPos rel) {
        RoverEntity r = ModEntities.ROVER.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(rel));
        r.setPos(at.x, at.y, at.z);
        h.getLevel().addFreshEntity(r);
        return r;
    }

    /** Bausatz aufstellen und abbauen: Energie, Module und Fracht bleiben erhalten (16). */
    public static void kitRoundTrip(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.CREATIVE, new Vec3(1.5, 1, 1.5));
        ItemStack kit = new ItemStack(ModItems.ROVER_KIT.get());
        RoverData data = new RoverData(5000, 0, List.of(RoverEntity.SCANNER), List.of(new ItemStack(Items.IRON_INGOT, 7)));
        kit.set(ModDataComponents.ROVER_DATA.get(), data);
        p.setItemInHand(InteractionHand.MAIN_HAND, kit);
        BlockPos floor = h.absolutePos(new BlockPos(4, 0, 4));
        kit.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        List<RoverEntity> rovers = h.getLevel().getEntitiesOfClass(RoverEntity.class, new net.minecraft.world.phys.AABB(floor).inflate(4));
        h.assertTrue(rovers.size() == 1, "Rover nicht aufgestellt: " + rovers.size());
        RoverEntity r = rovers.getFirst();
        h.assertTrue(r.energy() == 5000 && r.has(RoverEntity.SCANNER), "Daten nicht übernommen: " + r.data());
        ItemStack back = r.dismantle();
        RoverData after = back.get(ModDataComponents.ROVER_DATA.get());
        h.assertTrue(after != null && after.sameAs(r.data()) && after.energy() == 5000
                && after.cargo().getFirst().is(Items.IRON_INGOT) && after.cargo().getFirst().getCount() == 7, "Bausatz: " + after);
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(floor).inflate(4)).forEach(ItemEntity::discard);
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Neben einer geladenen {@code battery} lädt der Rover mit {@code charge_rate} J/t (16). */
    public static void chargesAtBattery(GameTestHelper h) {
        BlockPos battery = new BlockPos(1, 1, 3);
        h.setBlock(battery, ModBlocks.BATTERY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        MachineBlockEntity be = h.getBlockEntity(battery, MachineBlockEntity.class);
        be.refreshLayout();
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
        RoverEntity r = rover(h, new BlockPos(3, 1, 3));
        int rate = RoverEntity.rules().get("charge_rate").getAsInt();
        int ticks = 10;
        h.runAfterDelay(ticks + 1, () -> {
            h.assertTrue(r.energy() >= (long) rate * ticks, "Energie nach " + ticks + " t: " + r.energy());
            r.discard();
            h.succeed();
        });
    }

    /** Bohrer (16): der Block vor dem Rover wird abgebaut, der Drop landet im Frachtraum. */
    public static void drillToCargo(GameTestHelper h) {
        RoverEntity r = rover(h, new BlockPos(3, 1, 3));
        r.setYRot(0);
        r.load(new RoverData(0, 0, List.of(RoverEntity.DRILL), List.of()), null);
        // Blickrichtung Yaw 0 = Süden (+Z): Wand direkt vor der Hitbox
        BlockPos wall = BlockPos.containing(Vec3.atBottomCenterOf(new BlockPos(3, 1, 3)).add(0, 0, r.getBbWidth() / 2 + 0.5));
        h.setBlock(wall, Blocks.STONE);
        h.assertTrue(r.drill(h.getLevel(), true), "Bohrer findet keinen Block");
        h.assertBlockPresent(Blocks.AIR, wall);
        int cobble = 0;
        for (int i = 0; i < r.cargo().getContainerSize(); i++) {
            if (r.cargo().getItem(i).is(Items.COBBLESTONE)) {
                cobble += r.cargo().getItem(i).getCount();
            }
        }
        h.assertTrue(cobble == 1, "Fracht: " + cobble + " Bruchstein");
        r.discard();
        h.succeed();
    }

    /** Druckkabine (16, Override Mond): Insassen atmen ohne Anzug, solange der Tank Sauerstoff hat. */
    public static void cabinBreathable(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 40);
        RoverEntity r = rover(h, new BlockPos(3, 1, 3));
        JsonObject cabin = RoverEntity.rules();
        h.assertTrue(cabin.size() > 0, "vehicles.json fehlt");
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(3.5, 1, 3.5));
        r.load(new RoverData(0, 0, List.of(RoverEntity.CABIN), List.of()), null);
        p.startRiding(r);
        h.assertFalse(HazardTracker.evaluate(p, HazardTracker.rules()).breathable(), "ohne O₂ atembar");
        r.load(new RoverData(0, 1800, List.of(RoverEntity.CABIN), List.of()), null);
        h.assertTrue(HazardTracker.evaluate(p, HazardTracker.rules()).breathable(), "Kabine mit O₂ nicht atembar");
        p.stopRiding();
        r.discard();
        IhTestPlayers.remove(p);
        EnvironmentOverrides.remove(override);
        h.succeed();
    }
}
