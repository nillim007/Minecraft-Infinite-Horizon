package de.infinitehorizon.gametest;

import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.dome.DomeGeneratorBlockEntity;
import de.infinitehorizon.environment.AtmosphereRules;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.EnvironmentResult;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.machine.ElectricLampBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.world.ExtinguishedTorchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicLong;

/** GameTests für M3e (27.5): Kuppel T1 (Abnahme 2 und 3), Welt ohne Atmosphäre (8.5), elektrische Lampe. */
public final class IhGameTestsM3e {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier OXYGEN = Identifier.fromNamespaceAndPath("infinite_horizon", "oxygen");
    private static final BlockPos DOME = new BlockPos(4, 1, 4);

    private IhGameTestsM3e() {
    }

    /** Kuppelgenerator mit vollem Energiepuffer und {@code oxygen} mB im Tank. */
    private static DomeGeneratorBlockEntity dome(GameTestHelper h, long oxygen) {
        h.setBlock(DOME, ModBlocks.DOME_GENERATOR_T1.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        DomeGeneratorBlockEntity be = h.getBlockEntity(DOME, DomeGeneratorBlockEntity.class);
        be.refreshLayout();
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
        be.tank().setStackUnchecked(IhFluidStack.of(OXYGEN, oxygen));
        return be;
    }

    private static boolean breathable(ServerPlayer p) {
        EnvironmentResult r = HazardTracker.lastResult(p);
        return r != null && r.breathable();
    }

    /** Abnahme 2: Ein Spieler in aktiver Kuppel im Override Mond hat {@code breathable}. */
    public static void domeMakesAirBreathable(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 80);
        dome(h, 2000);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(6.5, 1, 6.5));
        h.succeedWhen(() -> {
            h.assertTrue(DomeData.get(h.getLevel()).at(h.absolutePos(DOME)) != null, "Kuppel nicht aktiv");
            h.assertTrue(breathable(p), "in der Kuppel nicht atembar");
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
        });
    }

    /**
     * Abnahme 3: Eine Kuppel mit 1 mB O₂ und vollem Puffer geht nach 24 t auf {@code failing} und ist 600 t später aus;
     * danach ist der Spieler nicht mehr {@code breathable} (Toleranz 10 %).
     */
    public static void domeFailsThenTurnsOff(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 760);
        dome(h, 1);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.CREATIVE, new Vec3(6.5, 1, 6.5));
        BlockPos center = h.absolutePos(DOME);
        AtomicLong failingAt = new AtomicLong(-1);
        AtomicLong offAt = new AtomicLong(-1);
        h.onEachTick(() -> {
            DomeData.Entry e = DomeData.get(h.getLevel()).at(center);
            if (e != null && e.failing() && failingAt.get() < 0) {
                failingAt.set(h.getTick());
            }
            if (e == null && failingAt.get() >= 0 && offAt.get() < 0) {
                offAt.set(h.getTick());
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(failingAt.get() >= 0, "noch nicht failing");
            h.assertTrue(failingAt.get() >= 22 && failingAt.get() <= 27, "failing nach " + failingAt.get() + " t, erwartet 24");
            h.assertTrue(offAt.get() >= 0, "noch nicht aus");
            long failingFor = offAt.get() - failingAt.get();
            h.assertTrue(failingFor >= 540 && failingFor <= 660, "aus nach " + failingFor + " t failing, erwartet 600");
            h.assertTrue(!breathable(p), "nach dem Ausfall noch atembar");
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
        });
    }

    /** Eine Fackel lässt sich im Override Mond außerhalb der Kuppel nicht setzen, innerhalb schon (8.5). */
    public static void torchOnlyInsideDome(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 60);
        dome(h, 2000);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(12.5, 1, 12.5));
        // fester Untergrund an beiden Stellen, damit nur die Atmosphäre-Regel entscheidet
        h.setBlock(new BlockPos(20, 0, 20), Blocks.STONE.defaultBlockState());
        h.setBlock(new BlockPos(6, 0, 6), Blocks.STONE.defaultBlockState());
        h.runAtTickTime(5, () -> {
            ItemStack torches = new ItemStack(Items.TORCH, 8);
            p.setItemInHand(InteractionHand.MAIN_HAND, torches);
            h.placeAt(p, torches, new BlockPos(20, 0, 20), Direction.UP);
            h.placeAt(p, torches, new BlockPos(6, 0, 6), Direction.UP);
            h.assertBlockNotPresent(Blocks.TORCH, new BlockPos(20, 1, 20));
            h.assertBlockPresent(Blocks.TORCH, new BlockPos(6, 1, 6));
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
            h.succeed();
        });
    }

    /** Bestehende Fackeln erlöschen ohne Atmosphäre, auch an der Wand (8.5). */
    public static void torchesExtinguish(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 20);
        BlockPos standing = new BlockPos(1, 1, 1);
        BlockPos wall = new BlockPos(2, 1, 3);
        h.setBlock(new BlockPos(3, 1, 3), Blocks.STONE.defaultBlockState());
        h.setBlock(standing, Blocks.TORCH.defaultBlockState());
        h.setBlock(wall, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
        for (BlockPos rel : new BlockPos[]{standing, wall}) {
            BlockPos abs = h.absolutePos(rel);
            BlockState state = h.getLevel().getBlockState(abs);
            h.assertTrue(state.isRandomlyTicking(), "Fackel bekommt keine Random Ticks");
            h.assertTrue(AtmosphereRules.randomTick(state, h.getLevel(), abs), "Random Tick nicht behandelt");
        }
        h.assertBlockProperty(standing, ExtinguishedTorchBlock.FACING, Direction.UP);
        h.assertBlockProperty(wall, ExtinguishedTorchBlock.FACING, Direction.WEST);
        EnvironmentOverrides.remove(override);
        h.succeed();
    }

    /** Wasser gefriert ohne Atmosphäre auf dem Mond ({@code water_behavior: freeze}, 8.5). */
    public static void waterFreezes(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 20);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, Blocks.WATER.defaultBlockState());
        h.assertBlockPresent(Blocks.ICE, pos);
        EnvironmentOverrides.remove(override);
        h.succeed();
    }

    /** Die elektrische Lampe leuchtet nur mit Strom (8.5). */
    public static void lampNeedsPower(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.ELECTRIC_LAMP.get().defaultBlockState());
        ElectricLampBlockEntity be = h.getBlockEntity(pos, ElectricLampBlockEntity.class);
        be.refreshLayout();
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
        h.succeedWhen(() -> {
            h.assertBlockProperty(pos, BlockStateProperties.LIT, true);
            int light = h.getLevel().getBlockState(h.absolutePos(pos)).getLightEmission();
            h.assertTrue(light == 15, "Licht " + light);
        });
    }
}
