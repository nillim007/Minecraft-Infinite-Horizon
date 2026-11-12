package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.multiblock.GhostPreview;
import de.infinitehorizon.multiblock.MultiblockControllerBlockEntity;
import de.infinitehorizon.multiblock.MultiblockPattern;
import de.infinitehorizon.multiblock.PortBlock;
import de.infinitehorizon.multiblock.PortBlockEntity;
import de.infinitehorizon.recipe.ProcessingRecipe;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * GameTests für M2d (27.3): Raffinerie-Multiblock mit Ports, Montagetisch mit exaktem Abgleich, Elektrolyseur.
 * Registrierung über {@link IhTestCatalog} (erzeugt aus {@code common/gametests.json}).
 */
public final class IhGameTestsM2d {

    private IhGameTestsM2d() {
    }

    private static final Identifier WATER = Identifier.withDefaultNamespace("water");
    private static final Identifier OIL = InfiniteHorizon.id("crude_oil");
    private static final Identifier KEROSENE = InfiniteHorizon.id("kerosene");
    private static final Identifier HYDROGEN = InfiniteHorizon.id("hydrogen");
    private static final Identifier OXYGEN = InfiniteHorizon.id("oxygen");
    /** Controller der Test-Raffinerie (relativ); Vorderseite Norden, Aufbau nach Osten/Westen, Süden und oben. */
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 1);

    // ------------------------------------------------------------------ Hilfen

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, BlockPos pos, Block block, Class<T> type) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(MachineBlock.FACING)) {
            s = s.setValue(MachineBlock.FACING, Direction.NORTH);
        }
        h.setBlock(pos, s);
        T be = h.getBlockEntity(pos, type);
        be.refreshLayout();
        return be;
    }

    private static void fullEnergy(MachineBlockEntity be) {
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
    }

    private static Item simple(String path) {
        return ModItems.simple(path).get();
    }

    private static MultiblockPattern refineryPattern() {
        return MultiblockPattern.forMachine(BalanceManager.get(), "infinite_horizon:refinery_controller")
                .orElseThrow(() -> new IllegalStateException("refinery_multiblock fehlt in machines.json"));
    }

    /** Relative Positionen aller 44 Gehäuseplätze der Test-Raffinerie. */
    private static List<BlockPos> casingPositions() {
        return refineryPattern().partPositions(CONTROLLER, Direction.NORTH);
    }

    /** Baut die Raffinerie; {@code skip} bleibt leer (null = vollständig). */
    private static MultiblockControllerBlockEntity buildRefinery(GameTestHelper h, BlockPos skip) {
        for (BlockPos p : casingPositions()) {
            if (!p.equals(skip)) {
                h.setBlock(p, ModBlocks.REFINERY_CASING.get());
            }
        }
        return machine(h, CONTROLLER, ModBlocks.REFINERY_CONTROLLER.get(), MultiblockControllerBlockEntity.class);
    }

    private static long amount(IhFluidTank tank, Identifier fluid) {
        return tank.stack().is(fluid) ? tank.stack().amount() : 0;
    }

    // ------------------------------------------------------------------ Raffinerie (27.2, 27.3 M2d)

    /** Fertige Raffinerie: 1.000 mB Erdöl → 600 mB Kerosin + 1 Kunststoff in höchstens 220 t. */
    public static void refineryCrudeOilToKerosene(GameTestHelper h) {
        MultiblockControllerBlockEntity c = buildRefinery(h, null);
        fullEnergy(c);
        FluidTarget in = c.fluidInput(null);
        h.assertTrue(in != null, "Raffinerie hat keinen Eingang");
        long accepted = in.insert(IhFluidStack.of(OIL, 1000), false);
        h.assertTrue(accepted == 1000, "Eingangstank nahm " + accepted + " mB Erdöl");
        Item plastic = simple("plastic");
        h.succeedWhen(() -> {
            IhFluidTank out = c.fluidTanks().get(1);
            h.assertTrue(amount(out, KEROSENE) == 600, "Kerosin " + out.stack());
            h.assertTrue(c.countOutput(plastic) == 1, "Kunststoff " + c.countOutput(plastic));
            h.assertTrue(c.fluidTanks().getFirst().stack().isEmpty(), "Erdöl übrig: " + c.fluidTanks().getFirst().stack());
        });
    }

    /** Ohne einen Gehäuseblock: error mit Koordinate; nach dem Einsetzen idle (27.3 M2d, 4.3). */
    public static void refineryMissingCasingThenIdle(GameTestHelper h) {
        BlockPos gap = casingPositions().getLast();
        MultiblockControllerBlockEntity c = buildRefinery(h, gap);
        fullEnergy(c);
        h.runAfterDelay(3, () -> {
            h.assertTrue(c.status() == MachineStatus.ERROR, "Status " + c.status() + ", erwartet error");
            h.assertTrue(!c.structureComplete(), "Aufbau gilt als vollständig");
            h.assertTrue(h.absolutePos(gap).equals(c.firstMissing()), "fehlend " + c.firstMissing() + ", erwartet " + h.absolutePos(gap));
            h.setBlock(gap, ModBlocks.REFINERY_CASING.get());
            h.runAfterDelay(2, () -> {
                h.assertTrue(c.structureComplete(), "Aufbau nach dem Einsetzen unvollständig");
                h.assertTrue(c.status() == MachineStatus.IDLE || c.status() == MachineStatus.RUNNING,
                        "Status " + c.status() + ", erwartet idle oder running");
                h.succeed();
            });
        });
    }

    /** Fremder Block im Muster zählt als fehlend; die Geisterprojektion zeigt alle falschen Plätze (4.1, 4.3). */
    public static void refineryWrongBlockAndGhostPreview(GameTestHelper h) {
        BlockPos wrong = casingPositions().get(5);
        MultiblockControllerBlockEntity c = buildRefinery(h, null);
        h.setBlock(wrong, net.minecraft.world.level.block.Blocks.IRON_BLOCK);
        h.runAfterDelay(3, () -> {
            h.assertTrue(c.status() == MachineStatus.ERROR, "Status " + c.status());
            BlockPos abs = h.absolutePos(CONTROLLER);
            List<MultiblockPattern.Missing> missing = GhostPreview.missing(h.getLevel(), abs, h.getLevel().getBlockState(abs));
            h.assertTrue(missing.size() == 1 && missing.getFirst().pos().equals(h.absolutePos(wrong)),
                    "Geisterprojektion: " + missing);
            h.assertTrue(casingPositions().size() == 44, "Muster hat " + casingPositions().size() + " Plätze, erwartet 44");
            h.succeed();
        });
    }

    /** Ports: Eingang Fluid und Energie, Ausgang Fluid in einen Tank, Ausgang Items; ohne Controller tun sie nichts. */
    public static void refineryPortsForwardToController(GameTestHelper h) {
        List<BlockPos> casings = casingPositions();
        // Plätze in der untersten Schicht neben dem Controller (links/rechts) und dahinter
        BlockPos fluidIn = CONTROLLER.east();
        BlockPos energyIn = CONTROLLER.west();
        BlockPos fluidOut = CONTROLLER.east().south();
        BlockPos itemOut = CONTROLLER.west().south();
        h.assertTrue(casings.containsAll(List.of(fluidIn, energyIn, fluidOut, itemOut)), "Port-Plätze liegen nicht im Muster");
        MultiblockControllerBlockEntity c = buildRefinery(h, null);
        h.setBlock(fluidIn, ModBlocks.FLUID_PORT.get());
        h.setBlock(energyIn, ModBlocks.ENERGY_PORT.get());
        h.setBlock(fluidOut, ModBlocks.FLUID_PORT.get().defaultBlockState().setValue(PortBlock.MODE, PortBlock.Mode.OUTPUT));
        h.setBlock(itemOut, ModBlocks.ITEM_PORT.get().defaultBlockState().setValue(PortBlock.MODE, PortBlock.Mode.OUTPUT));
        // Tank außerhalb neben dem Ausgabe-Port (östlich), Ausgangsseite weg vom Port
        BlockPos tankPos = fluidOut.east();
        h.setBlock(tankPos, ModBlocks.FLUID_TANK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        FluidTankBlockEntity tank = h.getBlockEntity(tankPos, FluidTankBlockEntity.class);
        tank.refreshLayout();
        // freistehender Port ohne Controller
        BlockPos lonely = new BlockPos(6, 1, 6);
        h.setBlock(lonely, ModBlocks.FLUID_PORT.get());
        h.runAfterDelay(3, () -> {
            h.assertTrue(c.structureComplete(), "Aufbau mit Ports unvollständig: " + c.firstMissing());
            PortBlockEntity in = h.getBlockEntity(fluidIn, PortBlockEntity.class);
            FluidTarget target = in.fluidInput(Direction.EAST);
            h.assertTrue(target != null && target.insert(IhFluidStack.of(OIL, 500), false) == 500, "Fluid-Port nimmt kein Erdöl an");
            h.assertTrue(amount(c.fluidTanks().getFirst(), OIL) == 500, "Erdöl kam nicht im Controller an");
            h.assertTrue(target.insert(IhFluidStack.of(WATER, 100), false) == 0, "Raffinerie nahm Wasser an");
            PortBlockEntity energy = h.getBlockEntity(energyIn, PortBlockEntity.class);
            long before = c.energyStorage().getAmount();
            h.assertTrue(energy.acceptsEnergyFrom(Direction.WEST) && energy.receiveEnergy(1000, false) == 1000,
                    "Energie-Port nimmt nicht an");
            h.assertTrue(c.energyStorage().getAmount() == before + 1000, "Energie kam nicht im Controller an");
            PortBlockEntity lone = h.getBlockEntity(lonely, PortBlockEntity.class);
            FluidTarget loneTarget = lone.fluidInput(Direction.UP);
            h.assertTrue(loneTarget == null || loneTarget.insert(IhFluidStack.of(OIL, 100), false) == 0,
                    "Port ohne Controller nahm Fluid an");
            // Ausgabe: Kerosin im Ausgangstank, Kunststoff im Ausgabeslot
            c.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(KEROSENE, 300));
            c.setItem(c.layout().outputStart(), new ItemStack(simple("plastic"), 2));
            h.runAfterDelay(3, () -> {
                h.assertTrue(amount(tank.tank(), KEROSENE) == 300, "Tank am Ausgabe-Port: " + tank.tank().stack());
                PortBlockEntity items = h.getBlockEntity(itemOut, PortBlockEntity.class);
                int slot = c.layout().outputStart();
                boolean offered = false;
                for (int s : items.getSlotsForFace(Direction.WEST)) {
                    offered |= s == slot;
                }
                h.assertTrue(offered && items.canTakeItemThroughFace(slot, items.getItem(slot), Direction.WEST),
                        "Item-Port bietet den Ausgabeslot nicht an");
                h.assertTrue(items.removeItem(slot, 2).getCount() == 2, "Kunststoff nicht entnehmbar");
                h.assertTrue(!items.canPlaceItemThroughFace(slot, new ItemStack(Items.STONE), Direction.WEST),
                        "Ausgabe-Port nimmt Items an");
                h.succeed();
            });
        });
    }

    /** Rechtsklick mit leerer Hand schaltet den Port-Modus um; mit Item in der Hand nicht (4.3). */
    public static void portModeToggle(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.FLUID_PORT.get());
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = h.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        BlockState s = h.getLevel().getBlockState(abs);
        s.useWithoutItem(h.getLevel(), player, hit);
        h.assertTrue(h.getLevel().getBlockState(abs).getValue(PortBlock.MODE) == PortBlock.Mode.OUTPUT, "Modus nicht umgeschaltet");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), player, hit);
        h.assertTrue(h.getLevel().getBlockState(abs).getValue(PortBlock.MODE) == PortBlock.Mode.OUTPUT,
                "mit Item in der Hand umgeschaltet");
        PortBlockEntity port = h.getBlockEntity(pos, PortBlockEntity.class);
        h.assertTrue(port.fluidInput(Direction.UP) == null && port.providesFluidTo(Direction.UP), "Ausgabe-Port verhält sich wie Eingang");
        h.succeed();
    }

    // ------------------------------------------------------------------ Montagetisch (22.3, 27.2)

    private static Optional<ProcessingRecipe> assemblyRecipe(GameTestHelper h, ProcessingMachineBlockEntity table) {
        return h.getLevel().recipeAccess().getRecipeFor(ModRecipeTypes.ASSEMBLY.type(), table.recipeInput(), h.getLevel())
                .map(RecipeHolder::value);
    }

    private static String resultOf(Optional<ProcessingRecipe> r) {
        return r.map(x -> BuiltInRegistries.ITEM.getKey(x.results().getFirst().item()).getPath()).orElse("-");
    }

    /** Exakter Abgleich: 4 Platten + 1 Rohr = fluid_port, 2 + 1 = refinery_casing, Überschuss = nichts. */
    public static void assemblyExactMatch(GameTestHelper h) {
        ProcessingMachineBlockEntity t = machine(h, new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY_TABLE.get(),
                ProcessingMachineBlockEntity.class);
        Item plate = simple("iron_plate");
        ItemStack pipe = new ItemStack(ModItems.COPPER_PIPE.get());
        t.setItem(0, new ItemStack(plate, 4));
        t.setItem(4, pipe.copy());
        String r1 = resultOf(assemblyRecipe(h, t));
        h.assertTrue("fluid_port".equals(r1), "4 Platten + 1 Rohr ergeben " + r1 + ", erwartet fluid_port");
        // Zutat über mehrere Slots verteilt (3 + 1)
        t.setItem(0, new ItemStack(plate, 3));
        t.setItem(8, new ItemStack(plate, 1));
        String r2 = resultOf(assemblyRecipe(h, t));
        h.assertTrue("fluid_port".equals(r2), "3 + 1 Platten + Rohr ergeben " + r2);
        t.setItem(8, ItemStack.EMPTY);
        t.setItem(0, new ItemStack(plate, 2));
        String r3 = resultOf(assemblyRecipe(h, t));
        h.assertTrue("refinery_casing".equals(r3), "2 Platten + 1 Rohr ergeben " + r3 + ", erwartet refinery_casing");
        t.setItem(0, new ItemStack(plate, 5));
        String r4 = resultOf(assemblyRecipe(h, t));
        h.assertTrue("-".equals(r4), "5 Platten + 1 Rohr ergeben " + r4 + ", erwartet nichts (Überschuss)");
        t.setItem(0, new ItemStack(plate, 4));
        t.setItem(2, new ItemStack(Items.STONE));
        String r5 = resultOf(assemblyRecipe(h, t));
        h.assertTrue("-".equals(r5), "mit fremdem Item: " + r5 + ", erwartet nichts");
        h.succeed();
    }

    /** 2 Silizium + 4 Kupferdraht + 1 Kunststoff → 1 Schaltkreis in höchstens 220 t; die Slots sind danach leer. */
    public static void assemblyCircuit(GameTestHelper h) {
        ProcessingMachineBlockEntity t = machine(h, new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY_TABLE.get(),
                ProcessingMachineBlockEntity.class);
        fullEnergy(t);
        t.setItem(0, new ItemStack(simple("silicon"), 2));
        t.setItem(1, new ItemStack(simple("copper_wire"), 3));
        t.setItem(5, new ItemStack(simple("copper_wire"), 1));
        t.setItem(7, new ItemStack(simple("plastic"), 1));
        Item circuit = simple("circuit");
        h.succeedWhen(() -> {
            h.assertTrue(t.countOutput(circuit) == 1, "Schaltkreise " + t.countOutput(circuit));
            MachineLayout l = t.layout();
            for (int i = 0; i < l.inputs(); i++) {
                h.assertTrue(t.getItem(i).isEmpty(), "Eingabeslot " + i + " nicht leer: " + t.getItem(i));
            }
        });
    }

    // ------------------------------------------------------------------ Elektrolyseur (27.2, 5.3)

    /** 200 mB Wasser → 200 mB Wasserstoff + 100 mB Sauerstoff in höchstens 44 t; Erdöl wird nicht angenommen. */
    public static void electrolyzerWater(GameTestHelper h) {
        ProcessingMachineBlockEntity e = machine(h, new BlockPos(1, 1, 1), ModBlocks.ELECTROLYZER.get(),
                ProcessingMachineBlockEntity.class);
        fullEnergy(e);
        FluidTarget in = e.fluidInput(Direction.EAST);
        h.assertTrue(in != null, "Elektrolyseur hat keinen Eingang");
        h.assertTrue(in.insert(IhFluidStack.of(OIL, 100), false) == 0, "Elektrolyseur nahm Erdöl an");
        h.assertTrue(in.insert(IhFluidStack.of(WATER, 200), false) == 200, "Elektrolyseur nahm kein Wasser an");
        h.succeedWhen(() -> {
            List<IhFluidTank> tanks = e.fluidTanks();
            h.assertTrue(tanks.size() == 3, "Tanks " + tanks.size());
            h.assertTrue(amount(tanks.get(1), HYDROGEN) == 200, "Wasserstoff " + tanks.get(1).stack());
            h.assertTrue(amount(tanks.get(2), OXYGEN) == 100, "Sauerstoff " + tanks.get(2).stack());
        });
    }

    /** Voller Gastank → Statuslampe error (5.3: Gase haben in M2 keinen Speicher). */
    public static void electrolyzerFullError(GameTestHelper h) {
        ProcessingMachineBlockEntity e = machine(h, new BlockPos(1, 1, 1), ModBlocks.ELECTROLYZER.get(),
                ProcessingMachineBlockEntity.class);
        fullEnergy(e);
        IhFluidTank h2 = e.fluidTanks().get(1);
        h2.setStackUnchecked(IhFluidStack.of(HYDROGEN, h2.capacity()));
        e.fluidTanks().get(2).setStackUnchecked(IhFluidStack.of(OXYGEN, 100));
        e.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(WATER, 1000));
        h.runAfterDelay(3, () -> {
            h.assertTrue(e.status() == MachineStatus.ERROR, "Status " + e.status() + ", erwartet error");
            h.succeed();
        });
    }

    /** Der Tank nimmt nur Flüssigkeiten (6.3), keine Gase. */
    public static void tankRejectsGas(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.FLUID_TANK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        FluidTankBlockEntity t = h.getBlockEntity(pos, FluidTankBlockEntity.class);
        t.refreshLayout();
        FluidTarget in = t.fluidInput(Direction.SOUTH);
        h.assertTrue(in != null && in.insert(IhFluidStack.of(HYDROGEN, 100), false) == 0, "Tank nahm Wasserstoff an");
        h.assertTrue(in.insert(IhFluidStack.of(KEROSENE, 100), false) == 100, "Tank nahm kein Kerosin an");
        h.succeed();
    }

    /** Knopf „Tank“: Auswahl unter den Ausgangstanks, gespeichert und begrenzt (4.1). */
    public static void electrolyzerTankSelection(GameTestHelper h) {
        ProcessingMachineBlockEntity e = machine(h, new BlockPos(1, 1, 1), ModBlocks.ELECTROLYZER.get(),
                ProcessingMachineBlockEntity.class);
        long packed = e.dataAccess().get(MachineBlockEntity.SYNC_TANK * 4) & 0xFFFF;
        h.assertTrue((packed & 0xFF) == 0 && ((packed >> 8) & 0xFF) == 2, "Tankwahl am Anfang: " + Long.toHexString(packed));
        e.setSelectedTank(1);
        h.assertTrue(e.selectedTank() == 1, "Tank 2 nicht gewählt");
        e.setSelectedTank(7);
        h.assertTrue(e.selectedTank() == 1, "Auswahl nicht begrenzt: " + e.selectedTank());
        long after = e.dataAccess().get(MachineBlockEntity.SYNC_TANK * 4) & 0xFFFF;
        h.assertTrue((after & 0xFF) == 1, "Sync-Wert der Tankwahl " + Long.toHexString(after));
        h.succeed();
    }
}
