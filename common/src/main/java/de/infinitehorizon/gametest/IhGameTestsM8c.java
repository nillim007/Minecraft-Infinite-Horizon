package de.infinitehorizon.gametest;

import com.google.gson.JsonPrimitive;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.logistics.OfflineProduction;
import de.infinitehorizon.logistics.OutpostData;
import de.infinitehorizon.logistics.OutpostManager;
import de.infinitehorizon.machine.DrillingRigBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MagneticTrapBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** GameTests für M8c (20.1, 27): Frachtterminal, Außenposten, Offline-Produktion. */
public final class IhGameTestsM8c {

    private static final BlockPos RIG = new BlockPos(2, 3, 2);
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 2);

    private IhGameTestsM8c() {
    }

    private static <T extends MachineBlockEntity> T place(GameTestHelper h, BlockPos pos, Block block, Class<T> type) {
        h.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(pos, type);
        be.refreshLayout();
        return be;
    }

    /** Bohrturm über 10 Eisenerz-Blöcken (Override „nicht Erde“), immer mit vollem Puffer. */
    private static DrillingRigBlockEntity rig(GameTestHelper h) {
        BlockPos abs = h.absolutePos(RIG);
        for (int k = 1; k <= 10; k++) {
            h.getLevel().setBlockAndUpdate(abs.below(k), Blocks.IRON_ORE.defaultBlockState());
        }
        DrillingRigBlockEntity be = place(h, RIG, ModBlocks.DRILLING_RIG.get(), DrillingRigBlockEntity.class);
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** Abnahme M8 (27), reine Funktion: running, Rate 1 Item / 100 t, Faktor 0,5, 24.000 t = 120 Items. */
    public static void offlineCompute(GameTestHelper h) {
        h.assertTrue(OfflineProduction.compute(true, 1, 100, 0.5, 24000) == 120,
                "compute = " + OfflineProduction.compute(true, 1, 100, 0.5, 24000));
        h.assertTrue(OfflineProduction.compute(false, 1, 100, 0.5, 24000) == 0, "ohne running");
        h.succeed();
    }

    /** 20.1: 216 Lagerslots, 8 Frachttanks und ein Treibstofftank; Methan füllt zuerst den Treibstofftank. */
    public static void terminalTanks(GameTestHelper h) {
        CargoTerminalBlockEntity t = place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.assertTrue(t.storageSlots() == 216, "Lagerslots " + t.storageSlots());
        h.assertTrue(t.cargoTanks().size() == 8, "Frachttanks " + t.cargoTanks().size());
        Identifier methane = InfiniteHorizon.id("methane");
        long fuelCap = t.fuelTank().capacity();
        long extra = t.cargoTanks().getFirst().capacity() / 4;
        long took = t.insertFluid(IhFluidStack.of(methane, fuelCap + extra), false);
        h.assertTrue(took == fuelCap + extra, "angenommen " + took);
        h.assertTrue(t.fuelTank().stack().amount() == fuelCap, "Treibstofftank " + t.fuelTank().stack());
        h.assertTrue(t.cargoTanks().getFirst().stack().amount() == extra, "Überschuss " + t.cargoTanks().getFirst().stack());
        // andere Fluide nie in den Treibstofftank, sondern in den nächsten freien Frachttank
        Identifier water = Identifier.withDefaultNamespace("water");
        t.insertFluid(IhFluidStack.of(water, 1000), false);
        h.assertTrue(t.cargoTanks().get(1).stack().is(water), "Wasser in Tank 2: " + t.cargoTanks().get(1).stack());
        // volles Lager: der Rest kommt zurück
        ItemStack rest = t.insertItem(new ItemStack(Items.COBBLESTONE, 64 * 216 + 5), false);
        h.assertTrue(rest.getCount() == 5, "Rest " + rest.getCount());
        h.succeed();
    }

    /** 20.1: Ein Produzent im Umkreis gibt direkt ins Terminal und steht im Außenposten-Register. */
    public static void producerDelivers(GameTestHelper h) {
        EnvironmentOverrides.Entry o = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 300);
        DrillingRigBlockEntity rig = rig(h);
        CargoTerminalBlockEntity t = place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(t.count(Items.IRON_ORE) > 0, "noch kein Erz im Terminal");
            OutpostData.Producer p = OutpostManager.data(h.getLevel()).producer(h.getLevel().dimension().identifier(),
                    rig.getBlockPos());
            h.assertTrue(p != null && p.running(), "Schnappschuss " + p);
            EnvironmentOverrides.remove(o);
        });
    }

    /** 20.1: Ein Produzent gehört immer zum nächstgelegenen Terminal. */
    public static void nearestTerminal(GameTestHelper h) {
        BlockPos far = new BlockPos(8, 3, 2);
        place(h, far, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.runAfterDelay(3, () -> {
            BlockPos got = OutpostManager.assignedTerminal(h.getLevel(), h.absolutePos(RIG));
            h.assertTrue(h.absolutePos(TERMINAL).equals(got), "zugeordnet " + got);
            BlockPos outside = h.absolutePos(TERMINAL).above(OutpostManager.radius() + 1);
            h.assertTrue(OutpostManager.assignedTerminal(h.getLevel(), outside) == null, "außerhalb des Umkreises");
            h.succeed();
        });
    }

    /** 20.1: Nach 24.000 t ohne geladenen Chunk liefert der Bohrturm vergangene_Ticks × Rate × Faktor ins Terminal. */
    public static void offlineCatchUp(GameTestHelper h) {
        EnvironmentOverrides.Entry o = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 300);
        DrillingRigBlockEntity rig = rig(h);
        CargoTerminalBlockEntity t = place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.runAfterDelay(25, () -> {
            OfflineProduction.Snapshot s = rig.offlineSnapshot(h.getLevel());
            h.assertTrue(s.cycleTicks() > 0, "kein Vorgang: " + s);
            long elapsed = 24000;
            OutpostData.Producer snap = new OutpostData.Producer(h.getLevel().dimension().identifier(), rig.getBlockPos(),
                    rig.machineId(), true, s.outputPerCycle(), s.cycleTicks(), OutpostManager.now(h.getLevel()) - elapsed,
                    rig.outpostToken());
            OutpostManager.data(h.getLevel()).putProducer(snap);
            long expected = OfflineProduction.cycles(snap.snapshot(), OutpostManager.factor(), elapsed);
            int before = t.count(Items.IRON_ORE);
            long done = OutpostManager.catchUp(h.getLevel(), rig);
            h.assertTrue(expected > 0 && done == expected, "Vorgänge " + done + ", erwartet " + expected);
            h.assertTrue(t.count(Items.IRON_ORE) - before == done * s.outputPerCycle(),
                    "Erz " + before + " → " + t.count(Items.IRON_ORE));
            EnvironmentOverrides.remove(o);
            h.succeed();
        });
    }

    /** 20.1: Nur wer beim Entladen {@code running} war, zählt; {@code outposts_offline = false} schaltet ab. */
    public static void offlineRules(GameTestHelper h) {
        EnvironmentOverrides.Entry o = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 300);
        DrillingRigBlockEntity rig = rig(h);
        place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.runAfterDelay(25, () -> {
            OfflineProduction.Snapshot s = rig.offlineSnapshot(h.getLevel());
            Identifier dim = h.getLevel().dimension().identifier();
            long past = OutpostManager.now(h.getLevel()) - 24000;
            OutpostData d = OutpostManager.data(h.getLevel());
            d.putProducer(new OutpostData.Producer(dim, rig.getBlockPos(), rig.machineId(), false, s.outputPerCycle(), s.cycleTicks(), past, rig.outpostToken()));
            h.assertTrue(OutpostManager.catchUp(h.getLevel(), rig) == 0, "nicht running gezählt");
            String key = "outposts_offline";
            var old = ServerConfig.overrideForTest(key, new JsonPrimitive(false));
            try {
                d.putProducer(new OutpostData.Producer(dim, rig.getBlockPos(), rig.machineId(), true, s.outputPerCycle(), s.cycleTicks(), past, rig.outpostToken()));
                h.assertTrue(OutpostManager.catchUp(h.getLevel(), rig) == 0, "trotz outposts_offline = false");
            } finally {
                ServerConfig.overrideForTest(key, old == null ? new JsonPrimitive(true) : old);
            }
            EnvironmentOverrides.remove(o);
            h.succeed();
        });
    }

    /** 20.1: Offline begrenzen die vorhandenen Eingaben (leere antimatter_cell der Magnetfalle). */
    public static void offlineInputs(GameTestHelper h) {
        CargoTerminalBlockEntity t = place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        MagneticTrapBlockEntity trap = place(h, RIG, ModBlocks.MAGNETIC_TRAP.get(), MagneticTrapBlockEntity.class);
        ItemStack empty = new ItemStack(ModItems.ANTIMATTER_CELL.get(), 3);
        empty.set(ModDataComponents.CHARGE.get(), 0);
        trap.setItem(0, empty);
        long done = trap.produceOffline(h.getLevel(), 100, t);
        h.assertTrue(done == 3, "Vorgänge " + done);
        h.assertTrue(trap.getItem(0).isEmpty(), "Zellen übrig: " + trap.getItem(0));
        h.assertTrue(t.count(ModItems.ANTIMATTER_CELL.get()) == 3, "geladene Zellen im Terminal: " + t.count(ModItems.ANTIMATTER_CELL.get()));
        h.succeed();
    }

    /** Abgebautes Terminal verschwindet aus dem Register. */
    public static void terminalRemoved(GameTestHelper h) {
        place(h, TERMINAL, ModBlocks.CARGO_TERMINAL.get(), CargoTerminalBlockEntity.class);
        h.runAfterDelay(3, () -> {
            Identifier dim = h.getLevel().dimension().identifier();
            h.assertTrue(OutpostManager.data(h.getLevel()).hasTerminal(dim, h.absolutePos(TERMINAL)), "nicht registriert");
            h.getLevel().destroyBlock(h.absolutePos(TERMINAL), false);
            h.assertFalse(OutpostManager.data(h.getLevel()).hasTerminal(dim, h.absolutePos(TERMINAL)), "noch registriert");
            h.succeed();
        });
    }
}
