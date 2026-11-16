package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.creature.IceJelly;
import de.infinitehorizon.creature.ReefKeeper;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.machine.EnergyItems;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.station.CloneChamberBlockEntity;
import de.infinitehorizon.station.CloneChambers;
import de.infinitehorizon.suit.IceDrillItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M7c, Teil Europa (13.4, 9.2, 14.4, 18): Eisdecke, Quellen, Eisbohrer, Tauchmodul, Klon-Kammer. */
public final class IhGameTestsM7c2 {

    private IhGameTestsM7c2() {
    }

    /** Eisdecke (13.1): In erzeugten Chunks abseits des Spawns liegt Europa-Eis zwischen y 112 und 124. */
    public static void iceShell(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int iced = 0;
        int columns = 0;
        for (int cx = 30; cx < 33; cx++) {
            for (int cz = 30; cz < 33; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL);
                for (int x = 0; x < 16; x += 4) {
                    for (int z = 0; z < 16; z += 4) {
                        columns++;
                        if (level.getBlockState(new BlockPos(cx * 16 + x, 118, cz * 16 + z)).is(ModBlocks.simple("europa_ice").get())) {
                            iced++;
                        }
                    }
                }
            }
        }
        h.assertTrue(iced > columns / 2, "Eis in " + iced + " von " + columns + " Spalten");
        h.succeed();
    }

    /** Tiefseedruck (9.1, 9.2): unter y 80 im Ozean der Europa; das Tauchmodul an der Hose schützt. */
    public static void divingModule(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
            ItemStack legs = new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get());
            p.setItemSlot(EquipmentSlot.LEGS, legs);
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
            p.teleportTo(p.getX(), 60, p.getZ());
            h.assertTrue(HazardTracker.evaluate(p, HazardTracker.rules()).hazards().contains(Hazard.DEEP_PRESSURE),
                    "kein Tiefseedruck bei y 60");
            legs.set(ModDataComponents.SUIT_MODULES.get(), List.of(InfiniteHorizon.id("diving_module")));
            h.assertFalse(HazardTracker.evaluate(p, HazardTracker.rules()).hazards().contains(Hazard.DEEP_PRESSURE),
                    "Tauchmodul schützt nicht");
            de.infinitehorizon.suit.SuitModules.tick(h.getLevel().getServer());
            h.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY) > 0,
                    "kein schnelleres Schwimmen");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Hydrothermale Quelle (13.4): Glasflasche → Mikrobenkultur, danach ruht die Quelle. */
    public static void ventBottle(GameTestHelper h) {
        BlockPos vent = new BlockPos(2, 1, 2);
        h.setBlock(vent, ModBlocks.HYDROTHERMAL_VENT.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 4.5));
        try {
            BlockPos abs = h.absolutePos(vent);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
            h.getBlockState(vent).useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(p.getInventory().countItem(ModItems.simple("microbe_culture").get()) == 1, "keine Mikrobenkultur");
            h.assertTrue(p.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "Flasche nicht verbraucht");
            h.getBlockState(vent).useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(p.getInventory().countItem(ModItems.simple("microbe_culture").get()) == 1, "Quelle ruht nicht");
            h.assertTrue(h.getBlockState(vent).getDestroySpeed(h.getLevel(), abs) < 0, "Quelle zerstörbar");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Eisbohrer (13.4): eine Schicht 3×3 unter dem Spieler, {@code energy_per_block} J je Block. */
    public static void iceDrill(GameTestHelper h) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                h.setBlock(new BlockPos(x, 1, z), ModBlocks.simple("europa_ice").get());
            }
        }
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        try {
            JsonObject c = IceDrillItem.config();
            long per = c.get("energy_per_block").getAsLong();
            int w = c.getAsJsonObject("shaft").get("width").getAsInt();
            ItemStack drill = new ItemStack(ModItems.ICE_DRILL.get());
            drill.set(ModDataComponents.ENERGY.get(), EnergyItems.componentCapacity(drill));
            h.assertTrue(EnergyItems.componentCapacity(drill) == c.get("energy_capacity").getAsLong(), "Akku");
            int n = IceDrillItem.drillLayer(h.getLevel(), p, drill);
            h.assertTrue(n == w * w, "gebohrt " + n);
            h.assertTrue(EnergyItems.stored(drill) == EnergyItems.componentCapacity(drill) - n * per, "Energie " + EnergyItems.stored(drill));
            h.assertTrue(h.getBlockState(new BlockPos(2, 1, 2)).isAir() && !h.getBlockState(new BlockPos(0, 1, 0)).isAir(),
                    "falscher Schacht");
            drill.set(ModDataComponents.ENERGY.get(), per * 2);
            p.teleportTo(h.absoluteVec(new Vec3(0.5, 2, 0.5)).x, p.getY(), h.absoluteVec(new Vec3(0.5, 2, 0.5)).z);
            h.assertTrue(IceDrillItem.drillLayer(h.getLevel(), p, drill) == 2, "ohne Energie weitergebohrt");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /**
     * Klon-Kammer (14.4): Rechtsklick bindet; mit Strom und Kultur erscheint der Spieler beim Respawn dort und eine
     * Kultur wird abgezogen. Ohne Strom gilt der normale Respawn.
     */
    public static void cloneChamber(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.CLONE_CHAMBER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        CloneChamberBlockEntity be = h.getBlockEntity(pos, CloneChamberBlockEntity.class);
        be.refreshLayout();
        be.setItem(0, new ItemStack(ModItems.simple("microbe_culture").get(), 2));
        boolean[] power = {true};
        h.onEachTick(() -> {
            if (power[0]) {
                be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
            } else {
                be.energyStorage().setAmountUnchecked(0);
            }
        });
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 2, 4.5));
        BlockPos abs = h.absolutePos(pos);
        h.getBlockState(pos).useWithoutItem(h.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        CloneChambers data = CloneChambers.get(h.getLevel().getServer());
        h.assertTrue(GlobalPos.of(h.getLevel().dimension(), abs).equals(data.binding(p.getUUID())), "nicht gebunden");
        h.runAfterDelay(5, () -> {
            try {
                h.assertTrue(CloneChambers.respawn(p), "kein Respawn in der Kammer");
                h.assertTrue(p.blockPosition().equals(abs.above()), "Spieler bei " + p.blockPosition());
            } catch (RuntimeException e) {
                IhTestPlayers.remove(p);
                throw e;
            }
        });
        h.runAfterDelay(10, () -> {
            try {
                h.assertTrue(be.getItem(0).getCount() == 1, "Kultur nicht abgezogen: " + be.getItem(0));
                power[0] = false;
            } catch (RuntimeException e) {
                IhTestPlayers.remove(p);
                throw e;
            }
        });
        h.runAfterDelay(15, () -> {
            try {
                h.assertFalse(CloneChambers.respawn(p), "Respawn ohne Strom");
            } finally {
                IhTestPlayers.remove(p);
                data.remove(GlobalPos.of(h.getLevel().dimension(), abs));
            }
            h.succeed();
        });
    }

    /** Riffwächter (18): Abbau in der Nähe macht ihn feindlich; mit einer Weltraumration gefüttert bleibt er friedlich. */
    public static void reefKeeper(GameTestHelper h) {
        ReefKeeper k = h.spawnWithNoFreeWill(ModEntities.REEF_KEEPER.get(), new Vec3(4.5, 1, 4.5));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 1.5));
        try {
            BlockPos at = h.absolutePos(new BlockPos(1, 1, 3));
            ReefKeeper.provoke(h.getLevel(), p, at, ReefKeeper.BLOCK_BREAK);
            h.assertTrue(k.getTarget() == p, "nicht feindlich");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPACE_RATION.get()));
            k.interact(p, InteractionHand.MAIN_HAND, k.position());
            h.assertTrue(p.getMainHandItem().isEmpty(), "Ration nicht verbraucht");
            h.assertTrue(k.getTarget() == null && k.pacifiedFor(p), "nicht beruhigt");
            ReefKeeper.provoke(h.getLevel(), p, at, ReefKeeper.SAMPLE_TOOL);
            h.assertTrue(k.getTarget() == null, "trotz Ration feindlich");
        } finally {
            IhTestPlayers.remove(p);
            k.discard();
        }
        h.succeed();
    }

    /** Eisqualle (18): findet die nächste Quelle und schwimmt dorthin. */
    public static void iceJellySeeksVent(GameTestHelper h) {
        h.setBlock(new BlockPos(5, 1, 5), ModBlocks.HYDROTHERMAL_VENT.get());
        IceJelly j = h.spawnWithNoFreeWill(ModEntities.ICE_JELLY.get(), new Vec3(1.5, 2, 1.5));
        Vec3 t = j.findVent(h.getLevel());
        h.assertTrue(t != null && Math.abs(t.x - h.absoluteVec(new Vec3(5.5, 0, 5.5)).x) < 1e-6
                && Math.abs(t.z - h.absoluteVec(new Vec3(5.5, 0, 5.5)).z) < 1e-6, "Ziel " + t);
        j.discard();
        h.succeed();
    }

    /** Quellenfeld (13.4): Quellen auf dem Meeresboden der Europa und genau ein Riffwächter. */
    public static void hydrothermalField(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var start = IhGameTestsM4b.placeStructure(level, InfiniteHorizon.id("hydrothermal_field"), h.absolutePos(new BlockPos(4, 1, 4)));
        h.assertTrue(start.isValid(), "Quellenfeld nicht gesetzt");
        var box = start.getBoundingBox();
        int vents = 0;
        for (BlockPos q : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(q).is(ModBlocks.HYDROTHERMAL_VENT.get())) {
                vents++;
            }
        }
        JsonObject s = BalanceManager.get().entry("creatures", InfiniteHorizon.id("reef_keeper").toString()).orElseThrow()
                .getAsJsonObject("spawn");
        List<ReefKeeper> keepers = level.getEntitiesOfClass(ReefKeeper.class, AABB.of(box).inflate(4));
        h.assertTrue(vents >= 1, "keine Quellen in " + box);
        h.assertTrue(keepers.size() == s.get("per_structure").getAsInt(), "Riffwächter " + keepers.size());
        keepers.forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
    }
}
