package de.infinitehorizon.gametest;

import de.infinitehorizon.creature.CloudRay;
import de.infinitehorizon.creature.CreatureRules;
import de.infinitehorizon.creature.DuneBurrower;
import de.infinitehorizon.creature.DustWraith;
import de.infinitehorizon.creature.FieldScannerItem;
import de.infinitehorizon.creature.RubbleCrawler;
import de.infinitehorizon.creature.SulfurSnail;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

/** GameTests für M5f (18): Kreaturen Ring 1 und Feldscanner. */
public final class IhGameTestsM5f {

    private IhGameTestsM5f() {
    }

    /** Steinboden unter dem Testbereich (y 0), damit Kreaturen stehen und laufen. */
    private static void floor(GameTestHelper h, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
    }

    private static <T extends Entity> T spawn(GameTestHelper h, EntityType<T> type, BlockPos rel) {
        floor(h, 16);
        T e = type.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(rel));
        e.setPos(at.x, at.y, at.z);
        h.getLevel().addFreshEntity(e);
        return e;
    }

    /** Krabbler: getarnt ohne Spieler; ein Spieler näher als {@code reveal_distance} enttarnt ihn. */
    public static void crawlerReveals(GameTestHelper h) {
        RubbleCrawler c = spawn(h, ModEntities.RUBBLE_CRAWLER.get(), new BlockPos(3, 1, 3));
        h.runAfterDelay(5, () -> {
            h.assertTrue(c.disguised(), "ohne Spieler nicht getarnt");
            ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 1, 3.5));
            h.runAfterDelay(5, () -> {
                h.assertFalse(c.disguised(), "Spieler daneben, trotzdem getarnt");
                IhTestPlayers.remove(p);
                c.discard();
                h.succeed();
            });
        });
    }

    /** Krabbler frisst ein Kupferkabel außerhalb einer Kuppel. */
    public static void crawlerEatsCable(GameTestHelper h) {
        BlockPos cable = new BlockPos(6, 1, 3);
        h.setBlock(cable, ModBlocks.COPPER_CABLE.get());
        RubbleCrawler c = spawn(h, ModEntities.RUBBLE_CRAWLER.get(), new BlockPos(2, 1, 3));
        h.succeedWhen(() -> {
            h.assertBlockNotPresent(ModBlocks.COPPER_CABLE.get(), cable);
            c.discard();
        });
    }

    /** Grabender Jäger: unsichtbar im Boden, kommt einem laufenden Zerkleinerer näher. */
    public static void burrowerSeeksVibration(GameTestHelper h) {
        BlockPos crusher = new BlockPos(12, 1, 3);
        h.setBlock(crusher, ModBlocks.CRUSHER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity be = h.getBlockEntity(crusher, MachineBlockEntity.class);
        be.refreshLayout();
        be.setItem(0, new ItemStack(Items.RAW_IRON, 64));
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        DuneBurrower b = spawn(h, ModEntities.DUNE_BURROWER.get(), new BlockPos(2, 1, 3));
        double start = b.distanceToSqr(Vec3.atCenterOf(h.absolutePos(crusher)));
        h.succeedWhen(() -> {
            h.assertTrue(b.isInvisible(), "nicht eingegraben");
            h.assertTrue(b.distanceToSqr(Vec3.atCenterOf(h.absolutePos(crusher))) < start / 4, "kommt nicht näher");
            b.discard();
        });
    }

    /** Schwefelschnecke legt eine Schwefelspur, die nach {@code lifetime_ticks} verschwinden soll (geplanter Tick). */
    public static void snailTrail(GameTestHelper h) {
        SulfurSnail s = spawn(h, ModEntities.SULFUR_SNAIL.get(), new BlockPos(3, 1, 3));
        h.assertTrue(s.placeTrail(h.getLevel()), "keine Spur gelegt");
        BlockPos at = s.blockPosition();
        h.assertTrue(h.getLevel().getBlockState(at).is(ModBlocks.SULFUR_TRAIL.get()), "Spur fehlt");
        h.assertTrue(h.getLevel().getBlockTicks().hasScheduledTick(at, ModBlocks.SULFUR_TRAIL.get()), "Spur verschwindet nie");
        s.discard();
        h.succeed();
    }

    /** Wolkenrochen: zähmen, satteln, aufsteigen. */
    public static void cloudRayMount(GameTestHelper h) {
        CloudRay r = spawn(h, ModEntities.CLOUD_RAY.get(), new BlockPos(3, 2, 3));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 1, 3.5));
        h.assertFalse(r.tryTame(p, 1.0), "Zähmen ohne Glück gelungen");
        h.assertTrue(r.tryTame(p, 0.0), "Zähmen mit Glück misslungen");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        r.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(r.saddled(), "kein Sattel");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        r.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(p.getVehicle() == r && r.getControllingPassenger() == p, "Spieler reitet nicht");
        p.stopRiding();
        r.discard();
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Feldscanner: einmal je Kreaturentyp {@code creature_analysis.fp} FP, danach nichts mehr. */
    public static void fieldScannerOnce(GameTestHelper h) {
        RubbleCrawler c = spawn(h, ModEntities.RUBBLE_CRAWLER.get(), new BlockPos(3, 1, 3));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 1, 3.5));
        int fp = (int) Math.round(CreatureRules.num(de.infinitehorizon.data.BalanceManager.get().entry("research", "creature_analysis")
                .orElseThrow(), "fp") * 100);
        h.assertTrue(FieldScannerItem.analyze(p, c) == fp, "erste Analyse ohne " + fp);
        h.assertTrue(FieldScannerItem.analyze(p, c) == 0, "zweite Analyse gibt FP");
        c.discard();
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Staubgeist ist immun gegen Projektile. */
    public static void wraithProjectileImmune(GameTestHelper h) {
        DustWraith w = spawn(h, ModEntities.DUST_WRAITH.get(), new BlockPos(3, 2, 3));
        float before = w.getHealth();
        w.hurtServer(h.getLevel(), h.getLevel().damageSources().thrown(null, null), 5);
        h.assertTrue(w.getHealth() == before, "Projektil schadet: " + w.getHealth());
        w.discard();
        h.succeed();
    }

    /** Spawnregel (18): feindliche Kreaturen nur bei Licht ≤ Grenze; unter freiem Himmel am Tag nicht. */
    public static void monsterNeedsDarkness(GameTestHelper h) {
        floor(h, 8);
        BlockPos pos = h.absolutePos(new BlockPos(3, 1, 3));
        AtomicReference<Boolean> ok = new AtomicReference<>(CreatureRules.canSpawn(ModEntities.DUNE_BURROWER.get(), h.getLevel(), pos));
        h.assertFalse(ok.get(), "Feind spawnt im Hellen");
        h.assertTrue(CreatureRules.canSpawn(ModEntities.SULFUR_SNAIL.get(), h.getLevel(), pos), "friedliche Kreatur spawnt nicht");
        h.succeed();
    }
}
