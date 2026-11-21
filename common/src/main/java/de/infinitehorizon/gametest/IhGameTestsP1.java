package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.ConduitBlock;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.machine.FluidPumpBlockEntity;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Spieltest Phase 1 (Ä31, docs/dev/spieltest-phase1.md): Pumpe gibt überall außer vorn ab, Speicher nehmen und geben
 * überall, aber nie an Speicher, der Elektrolyseur gibt jedes Gas über eine eigene Seite ab, Rohre verbinden sich nach
 * dem Umstellen neu, der Montagetisch nimmt Überschuss, Maschinen starten erst mit Strom für den ganzen Vorgang, ein
 * abgebauter Tank behält seinen Inhalt.
 */
public final class IhGameTestsP1 {

    private static final Identifier WATER = Identifier.withDefaultNamespace("water");
    private static final Identifier OIL = InfiniteHorizon.id("crude_oil");

    private IhGameTestsP1() {
    }

    private static <T extends MachineBlockEntity> T place(GameTestHelper h, BlockPos pos, Block block, Direction facing, Class<T> type) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(MachineBlock.FACING)) {
            s = s.setValue(MachineBlock.FACING, facing);
        }
        h.setBlock(pos, s);
        T be = h.getBlockEntity(pos, type);
        be.refreshLayout();
        return be;
    }

    private static Item simple(String id) {
        return BuiltInRegistries.ITEM.getValue(InfiniteHorizon.id(id));
    }

    /** Pumpe (Front Nord) über Erdöl: gibt an allen Seiten außer vorn ab; der Tank vorn im Norden bekommt nichts. */
    public static void pumpOutputsAllSidesExceptFront(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), ModFluids.get("crude_oil").source().get().defaultFluidState().createLegacyBlock());
        FluidPumpBlockEntity pump = place(h, new BlockPos(2, 3, 2), ModBlocks.FLUID_PUMP.get(), Direction.NORTH,
                FluidPumpBlockEntity.class);
        pump.energyStorage().setAmountUnchecked(pump.energyStorage().getCapacity());
        FluidTankBlockEntity front = place(h, new BlockPos(2, 3, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        FluidTankBlockEntity east = place(h, new BlockPos(3, 3, 2), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        FluidTankBlockEntity up = place(h, new BlockPos(2, 4, 2), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        for (Direction d : Direction.values()) {
            h.assertTrue(pump.providesFluidTo(d) == (d != Direction.NORTH), "Pumpe gibt über " + d + " ab: " + pump.providesFluidTo(d));
        }
        h.succeedWhen(() -> {
            h.assertTrue(east.tank().stack().is(OIL) || up.tank().stack().is(OIL), "Osten " + east.tank().stack() + ", oben " + up.tank().stack());
            h.assertTrue(front.tank().stack().isEmpty(), "vorn bekam " + front.tank().stack());
        });
    }

    /**
     * Ein Tank gibt an jeder Seite ab, aber nie an einen anderen Speicher: Über ein Rohr bekommt der Elektrolyseur das
     * Wasser, der zweite Tank am selben Rohr nichts.
     */
    public static void storageFeedsMachinesNotStorage(GameTestHelper h) {
        FluidTankBlockEntity source = place(h, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        source.tank().setStackUnchecked(IhFluidStack.of(WATER, 4000));
        // Rohr an der Westseite des Tanks – früher keine Ausgangsseite
        for (int z = 1; z <= 3; z++) {
            h.setBlock(new BlockPos(0, 1, z), ModBlocks.COPPER_PIPE.get());
        }
        ProcessingMachineBlockEntity electrolyzer = place(h, new BlockPos(0, 1, 4), ModBlocks.ELECTROLYZER.get(), Direction.SOUTH,
                ProcessingMachineBlockEntity.class);
        FluidTankBlockEntity other = place(h, new BlockPos(1, 1, 3), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        h.runAfterDelay(40, () -> {
            h.assertTrue(electrolyzer.fluidTanks().getFirst().stack().is(WATER), "Elektrolyseur: " + electrolyzer.fluidTanks().getFirst().stack());
            h.assertTrue(other.tank().stack().isEmpty(), "zweiter Tank bekam " + other.tank().stack());
            h.succeed();
        });
    }

    /** Ein Tank nimmt an jeder Seite an, auch an der früheren Ausgangsseite vorn. */
    public static void storageAcceptsEverySide(GameTestHelper h) {
        FluidTankBlockEntity tank = place(h, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        for (Direction d : Direction.values()) {
            h.assertTrue(tank.fluidInput(d) != null, "nimmt nicht an über " + d);
            h.assertTrue(tank.providesFluidTo(d), "gibt nicht ab über " + d);
        }
        h.succeed();
    }

    /** Der Elektrolyseur gibt Wasserstoff über die erste, Sauerstoff über die zweite Ausgangsseite in Druckflaschen. */
    public static void electrolyzerGasesTwoSides(GameTestHelper h) {
        ProcessingMachineBlockEntity e = place(h, new BlockPos(2, 1, 2), ModBlocks.ELECTROLYZER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        e.energyStorage().setAmountUnchecked(e.energyStorage().getCapacity());
        e.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(WATER, 2000));
        Direction h2Side = e.outputSideOf(0);
        Direction o2Side = e.outputSideOf(1);
        h.assertTrue(h2Side != o2Side, "beide Gase auf derselben Seite " + h2Side);
        FluidTankBlockEntity h2 = place(h, new BlockPos(2, 1, 2).relative(h2Side), ModBlocks.PRESSURE_BOTTLE.get(), Direction.NORTH,
                FluidTankBlockEntity.class);
        FluidTankBlockEntity o2 = place(h, new BlockPos(2, 1, 2).relative(o2Side), ModBlocks.PRESSURE_BOTTLE.get(), Direction.NORTH,
                FluidTankBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(h2.tank().stack().is(InfiniteHorizon.id("hydrogen")), "Wasserstoff-Flasche: " + h2.tank().stack());
            h.assertTrue(o2.tank().stack().is(InfiniteHorizon.id("oxygen")), "Sauerstoff-Flasche: " + o2.tank().stack());
        });
    }

    /** Wird die Ausgangsseite umgestellt, verbindet sich das angrenzende Rohr sofort neu (Arm erscheint). */
    public static void pipeReconnectsAfterSideChange(GameTestHelper h) {
        ProcessingMachineBlockEntity crusher = place(h, new BlockPos(1, 1, 1), ModBlocks.ELECTROLYZER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        BlockPos pipe = new BlockPos(1, 2, 1);
        h.setBlock(pipe, ModBlocks.COPPER_PIPE.get());
        crusher.setSelectedTank(0);
        crusher.setOutputSide(Direction.UP);
        h.assertTrue(h.getBlockState(pipe).getValue(ConduitBlock.CONNECTIONS.get(Direction.DOWN)),
                "Rohr nicht mit der neuen Ausgangsseite verbunden");
        h.succeed();
    }

    /** Montagetisch: mehr Material als nötig ist erlaubt; der Rest bleibt liegen. */
    public static void assemblyAcceptsSurplus(GameTestHelper h) {
        ProcessingMachineBlockEntity t = place(h, new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY_TABLE.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        t.energyStorage().setAmountUnchecked(t.energyStorage().getCapacity());
        t.setItem(0, new ItemStack(simple("silicon"), 5));
        t.setItem(1, new ItemStack(simple("copper_wire"), 9));
        t.setItem(2, new ItemStack(simple("plastic"), 3));
        Item circuit = simple("circuit");
        h.succeedWhen(() -> {
            h.assertTrue(t.countOutput(circuit) >= 1, "kein Schaltkreis bei Überschuss");
            h.assertTrue(!t.getItem(0).isEmpty(), "Überschuss nicht liegen geblieben");
        });
    }

    /** Eine Maschine startet erst, wenn Strom für den ganzen Vorgang im Speicher ist. */
    public static void machineWaitsForEnergy(GameTestHelper h) {
        ProcessingMachineBlockEntity c = place(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        c.energyStorage().setAmountUnchecked(1);
        c.setItem(0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
        h.runAfterDelay(10, () -> {
            h.assertTrue(c.progress() == 0, "Vorgang mit 1 J gestartet: " + c.progress());
            c.energyStorage().setAmountUnchecked(c.energyStorage().getCapacity());
            h.runAfterDelay(10, () -> {
                h.assertTrue(c.progress() > 0, "Vorgang mit vollem Speicher nicht gestartet");
                h.succeed();
            });
        });
    }

    /** Ein abgebauter Tank behält seinen Inhalt im Item (Komponente infinite_horizon:fluid). */
    public static void tankKeepsFluidWhenBroken(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        FluidTankBlockEntity tank = place(h, pos, ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        tank.tank().setStackUnchecked(IhFluidStack.of(OIL, 5000));
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        h.succeedWhen(() -> {
            var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(2));
            IhFluidStack kept = items.stream().map(ItemEntity::getItem).filter(s -> s.is(ModBlocks.FLUID_TANK.get().asItem()))
                    .map(s -> s.get(ModDataComponents.FLUID.get())).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            h.assertTrue(kept != null && kept.is(OIL) && kept.amount() == 5000, "Item-Inhalt: " + kept);
        });
    }

    /** Die Probenzange baut den Block ab und gibt die Probe. */
    public static void sampleRemovesBlock(GameTestHelper h) {
        BlockPos ore = new BlockPos(1, 1, 1);
        h.setBlock(ore, ModBlocks.simple("bauxite_ore").get());
        var p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var r = de.infinitehorizon.research.SampleTool.sample(h.getLevel(), h.absolutePos(ore), p);
        h.assertTrue(r == de.infinitehorizon.research.SampleTool.Result.SAMPLED, "Ergebnis " + r);
        h.assertBlockPresent(Blocks.AIR, ore);
        boolean found = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            found |= p.getInventory().getItem(i).is(ModItems.SAMPLE.get());
        }
        h.assertTrue(found, "keine Probe im Inventar");
        h.succeed();
    }
}
