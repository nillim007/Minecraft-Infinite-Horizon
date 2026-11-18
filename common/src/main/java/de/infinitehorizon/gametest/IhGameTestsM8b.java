package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.event.EventService;
import de.infinitehorizon.food.HydroponicsBlockEntity;
import de.infinitehorizon.machine.DrillingRigBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.PumpStationBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** GameTests für M8b (20.2, 6.1, 17, 19): Bohrturm, Pumpstation, Ponton, Ammoniak-Dünger. */
public final class IhGameTestsM8b {

    private static final BlockPos MACHINE = new BlockPos(2, 3, 2);

    private IhGameTestsM8b() {
    }

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, Block block, Class<T> type) {
        h.setBlock(MACHINE, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(MACHINE, type);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** Abnahme M8 (27): Mit Override „nicht Erde“ läuft der Bohrturm über 10 Eisenerz-Blöcken und liefert Erz. */
    public static void drillingRigRuns(GameTestHelper h) {
        EnvironmentOverrides.Entry o = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 300);
        for (int k = 0; k < 10; k++) {
            h.setBlock(new BlockPos(k % 5, 1, k / 5), Blocks.IRON_ORE);
        }
        DrillingRigBlockEntity be = machine(h, ModBlocks.DRILLING_RIG.get(), DrillingRigBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(be.counts().getOrDefault("minecraft:iron_ore", 0) >= 10, "Eisenerz gezählt: " + be.counts());
            h.assertTrue(be.status() == MachineStatus.RUNNING, "Status " + be.status() + "/" + be.statusHint());
            boolean ore = false;
            for (int i = be.layout().outputStart(); i < be.layout().upgradeStart(); i++) {
                ore |= !be.getItem(i).isEmpty();
            }
            h.assertTrue(ore, "noch kein Erz");
            EnvironmentOverrides.remove(o);
        });
    }

    /** Auf der Erde arbeitet der Bohrturm nicht (Ortsbindung, 6.1). */
    public static void drillingRigEarth(GameTestHelper h) {
        DrillingRigBlockEntity be = machine(h, ModBlocks.DRILLING_RIG.get(), DrillingRigBlockEntity.class);
        h.runAfterDelay(5, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + "/" + be.statusHint());
            h.succeed();
        });
    }

    /** Ohne Erze im Chunk darunter: {@code error} (20.2). */
    public static void drillingRigNoOres(GameTestHelper h) {
        DrillingRigBlockEntity be = machine(h, ModBlocks.DRILLING_RIG.get(), DrillingRigBlockEntity.class);
        h.runAfterDelay(5, () -> {
            h.assertTrue(be.counts().isEmpty(), "Erze im leeren Raum: " + be.counts());
            h.assertTrue(be.status() == MachineStatus.ERROR, "Status " + be.status());
            h.succeed();
        });
    }

    /** Pumpstation auf dem Ammoniak-Geysir: Ammoniak, der Geysir bleibt (6.1). */
    public static void pumpGeyser(GameTestHelper h) {
        h.setBlock(MACHINE.below(), ModBlocks.AMMONIA_GEYSER.get());
        PumpStationBlockEntity be = machine(h, ModBlocks.PUMP_STATION.get(), PumpStationBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(be.fluidTanks().getFirst().stack().is(InfiniteHorizon.id("ammonia")), "Tank " + be.fluidTanks().getFirst().stack());
            h.assertTrue(h.getBlockState(MACHINE.below()).is(ModBlocks.AMMONIA_GEYSER.get()), "Geysir verbraucht");
        });
    }

    /** Titan (19): Pumpstation auf einem Ponton über Methan; im Methanregen mehr je Vorgang. */
    public static void pumpPontoonMethaneRain(GameTestHelper h) {
        Block methane = ModFluids.get("methane").block().get();
        h.setBlock(MACHINE.below(2), methane);
        h.assertFalse(ModBlocks.PONTOON.get().defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(new BlockPos(4, 1, 4))),
                "Ponton ohne Fluid");
        h.assertTrue(ModBlocks.PONTOON.get().defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(MACHINE.below())),
                "Ponton nicht auf Methan");
        h.setBlock(MACHINE.below(), ModBlocks.PONTOON.get());
        PumpStationBlockEntity be = machine(h, ModBlocks.PUMP_STATION.get(), PumpStationBlockEntity.class);
        h.assertTrue(InfiniteHorizon.id("methane").equals(be.source(h.getLevel())), "Quelle " + be.source(h.getLevel()));
        long base = be.amount(h.getLevel());
        EventService.force(h.getLevel(), InfiniteHorizon.id("methane_rain"), 100);
        try {
            long rain = be.amount(h.getLevel());
            h.assertTrue(rain > base, "kein Bonus im Methanregen: " + base + " → " + rain);
        } finally {
            EventService.clear(h.getLevel());
        }
        h.succeed();
    }

    /** Ammoniak-Dünger (17): Rechtsklick düngt das Hydrokultur-Becken und wird verbraucht. */
    public static void fertilizer(GameTestHelper h) {
        h.setBlock(MACHINE, ModBlocks.HYDROPONICS_BASIN.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        HydroponicsBlockEntity basin = h.getBlockEntity(MACHINE, HydroponicsBlockEntity.class);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 2, 4.5));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AMMONIA_FERTILIZER.get(), 2));
            BlockPos abs = h.absolutePos(MACHINE);
            p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            h.assertTrue(basin.fertilized(h.getLevel()), "nicht gedüngt");
            h.assertTrue(p.getMainHandItem().getCount() == 1, "Dünger nicht verbraucht");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
