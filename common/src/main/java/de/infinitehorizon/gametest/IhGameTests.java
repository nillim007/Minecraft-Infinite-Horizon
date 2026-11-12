package de.infinitehorizon.gametest;

import com.google.gson.JsonPrimitive;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.CombustionGeneratorBlockEntity;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.machine.FluidPumpBlockEntity;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * GameTests aus Kapitel 27, Meilenstein 1. Die Logik steht hier; die Loader registrieren nur
 * (Fabric: Annotation, NeoForge: RegisterGameTestsEvent). Namen nach 3A.8.
 */
public final class IhGameTests {

    /** Struktur unter data/infinite_horizon/structure/gametest/empty.nbt (leer, 8×8×8). */
    public static final String STRUCTURE = "infinite_horizon:gametest/empty";

    public static final String CRUSHER_CHAIN = "crusher_rawiron_twodustwithin220ticks";
    public static final int CRUSHER_CHAIN_TICKS = 220;
    public static final String BALANCE_RELOAD = "crusher_balancechange_changesduration";
    public static final int BALANCE_RELOAD_TICKS = 20;
    public static final String PLATFORM_CONSUMER = "cable_platformconsumer_receivesenergy";
    public static final int PLATFORM_CONSUMER_TICKS = 100;
    public static final String METAL_CHAIN = "machines_rawtitanium_platewithin440ticks";
    /** 27.2/27.3: Zerkleinerer 220 t + Elektroofen 110 t + Presse 110 t. */
    public static final int METAL_CHAIN_TICKS = 440;

    private static final BlockPos GENERATOR = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE_A = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE_B = new BlockPos(3, 1, 1);
    private static final BlockPos END = new BlockPos(4, 1, 1);

    private IhGameTests() {
    }

    private static CombustionGeneratorBlockEntity generatorWithCoal(GameTestHelper helper) {
        helper.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(CABLE_A, ModBlocks.COPPER_CABLE.get());
        helper.setBlock(CABLE_B, ModBlocks.COPPER_CABLE.get());
        CombustionGeneratorBlockEntity gen = helper.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class);
        gen.setItem(0, new ItemStack(Items.COAL));
        return gen;
    }

    /** Generator mit Kohle → Kabel → Zerkleinerer: 1 raw_iron → 2 iron_dust in höchstens 220 t. */
    public static void crusherRawIronTwoDust(GameTestHelper helper) {
        generatorWithCoal(helper);
        helper.setBlock(END, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity crusher = helper.getBlockEntity(END, ProcessingMachineBlockEntity.class);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        helper.succeedWhen(() -> helper.assertTrue(crusher.countOutput(ModItems.simple("iron_dust").get()) == 2,
                "Erwartet 2 iron_dust, vorhanden: " + crusher.countOutput(ModItems.simple("iron_dust").get())));
    }

    /**
     * Geänderter Wert in machines.json ändert die Vorgangsdauer. Der Test ersetzt die geladenen Werte genau so,
     * wie es der Reload-Listener nach /reload tut (BalanceManager.set), und stellt sie im selben Tick wieder her.
     */
    public static void crusherBalanceChangesDuration(GameTestHelper helper) {
        helper.setBlock(END, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity crusher = helper.getBlockEntity(END, ProcessingMachineBlockEntity.class);
        ItemStack input = new ItemStack(Items.RAW_IRON);
        BalanceManager original = BalanceManager.get();
        int before = crusher.durationFor(helper.getLevel(), input);
        int wanted = before * 2;
        int after;
        try {
            BalanceManager.set(original.withValue("machines", crusher.machineId(), "ticks", new JsonPrimitive(wanted)));
            after = crusher.durationFor(helper.getLevel(), input);
        } finally {
            BalanceManager.set(original);
        }
        helper.assertTrue(before > 0, "Kein crushing-Rezept für raw_iron gefunden");
        helper.assertTrue(after == wanted, "Dauer vorher " + before + ", nachher " + after + ", erwartet " + wanted);
        helper.succeed();
    }

    private static ProcessingMachineBlockEntity poweredMachine(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block) {
        helper.setBlock(pos, block);
        ProcessingMachineBlockEntity be = helper.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
        be.refreshLayout();
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
        return be;
    }

    private static void insertInput(ProcessingMachineBlockEntity be, ItemStack stack) {
        ItemStack current = be.getItem(0);
        if (current.isEmpty()) {
            be.setItem(0, stack);
        } else if (ItemStack.isSameItemSameComponents(current, stack)) {
            current.grow(stack.getCount());
            be.setChanged();
        }
    }

    /** M2a: raw_titanium → titanium_dust → titanium_ingot → titanium_plate in Zerkleinerer, Elektroofen und Presse. */
    public static void machinesRawTitaniumToPlate(GameTestHelper helper) {
        ProcessingMachineBlockEntity crusher = poweredMachine(helper, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity furnace = poweredMachine(helper, new BlockPos(3, 1, 1), ModBlocks.ELECTRIC_FURNACE.get());
        ProcessingMachineBlockEntity press = poweredMachine(helper, new BlockPos(5, 1, 1), ModBlocks.PRESS.get());
        crusher.setItem(0, new ItemStack(ModItems.simple("raw_titanium").get()));
        press.setItem(press.layout().moldStart(), new ItemStack(ModItems.simple("press_mold_plate").get()));
        var dust = ModItems.simple("titanium_dust").get();
        var ingot = ModItems.simple("titanium_ingot").get();
        var plate = ModItems.simple("titanium_plate").get();
        helper.succeedWhen(() -> {
            ItemStack d = crusher.takeOutput(dust, 64);
            if (!d.isEmpty()) {
                insertInput(furnace, d);
            }
            ItemStack i = furnace.takeOutput(ingot, 64);
            if (!i.isEmpty()) {
                insertInput(press, i);
            }
            helper.assertTrue(press.countOutput(plate) >= 1, "Noch keine titanium_plate (Zerkleinerer "
                    + crusher.status() + ", Ofen " + furnace.status() + ", Presse " + press.status() + ")");
        });
    }

    public static final String ORE_TOOLS = "ores_tooltier_dropsonlywithcorrecttool";
    public static final int ORE_TOOLS_TICKS = 20;

    /** M2b: bauxite_ore mit Steinspitzhacke gibt 1 bauxite; titanium_ore braucht eine Eisenspitzhacke. */
    public static void oresNeedCorrectTool(GameTestHelper helper) {
        BlockState bauxite = ModBlocks.simple("bauxite_ore").get().defaultBlockState();
        BlockState titanium = ModBlocks.simple("titanium_ore").get().defaultBlockState();
        ItemStack wood = new ItemStack(Items.WOODEN_PICKAXE);
        ItemStack stone = new ItemStack(Items.STONE_PICKAXE);
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        helper.assertTrue(bauxite.requiresCorrectToolForDrops(), "bauxite_ore braucht ein Werkzeug");
        helper.assertTrue(stone.isCorrectToolForDrops(bauxite), "Steinspitzhacke muss bauxite_ore abbauen");
        helper.assertFalse(wood.isCorrectToolForDrops(bauxite), "Holzspitzhacke darf bauxite_ore nicht abbauen");
        helper.assertFalse(stone.isCorrectToolForDrops(titanium), "titanium_ore braucht eine Eisenspitzhacke");
        helper.assertTrue(iron.isCorrectToolForDrops(titanium), "Eisenspitzhacke muss titanium_ore abbauen");

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, bauxite);
        List<ItemStack> drops = Block.getDrops(bauxite, helper.getLevel(), helper.absolutePos(pos), null, null, stone);
        Item item = ModItems.simple("bauxite").get();
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                "Erwartet genau 1 bauxite, erhalten: " + drops);
        helper.succeed();
    }

    public static final String PUMP_TANK = "pump_crudeoil_tank1000within50ticks";
    /**
     * 27.2 nennt 44 t (40 t × 1,1), übersieht aber den Rohrdurchsatz: 1.000 mB bei 200 mB/t brauchen 5 t mehr.
     * SPEC-GAP (#23): (40 + 5) × 1,1 ≈ 50 t.
     */
    public static final int PUMP_TANK_TICKS = 50;
    public static final String PIPES_NO_MIX = "pipes_twofluids_nevermixed";
    public static final int PIPES_NO_MIX_TICKS = 60;
    public static final String BUCKET_FILL = "tank_fillslot_bucketbecomescrudeoilbucket";
    public static final int BUCKET_FILL_TICKS = 20;

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T placeFacing(GameTestHelper helper, BlockPos pos, Block block, Direction facing, Class<T> type) {
        helper.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
        T be = helper.getBlockEntity(pos, type);
        ((MachineBlockEntity) be).refreshLayout();
        return be;
    }

    /** M2c: Pumpe saugt einen crude_oil-Quellblock 4 Blöcke tiefer ab, Rohr → Tank hat 1.000 mB in höchstens 44 t. */
    public static void pumpCrudeOilIntoTank(GameTestHelper helper) {
        BlockPos pump = new BlockPos(2, 5, 2);
        helper.setBlock(new BlockPos(2, 1, 2), ModFluids.get("crude_oil").source().get().defaultFluidState().createLegacyBlock());
        FluidPumpBlockEntity p = placeFacing(helper, pump, ModBlocks.FLUID_PUMP.get(), Direction.EAST, FluidPumpBlockEntity.class);
        p.energyStorage().setAmountUnchecked(p.energyStorage().getCapacity());
        helper.setBlock(new BlockPos(3, 5, 2), ModBlocks.COPPER_PIPE.get());
        FluidTankBlockEntity tank = placeFacing(helper, new BlockPos(4, 5, 2), ModBlocks.FLUID_TANK.get(), Direction.NORTH,
                FluidTankBlockEntity.class);
        Identifier oil = InfiniteHorizon.id("crude_oil");
        helper.succeedWhen(() -> helper.assertTrue(tank.tank().stack().is(oil) && tank.tank().stack().amount() >= 1000,
                "Tank: " + tank.tank().stack() + ", Pumpe " + p.status()));
    }

    /** M2c: Zwei Quellen mit Wasser und Erdöl an einem Rohrnetz; beide Empfänger bekommen dasselbe Fluid. */
    public static void pipesNeverMix(GameTestHelper helper) {
        FluidTankBlockEntity water = placeFacing(helper, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.EAST,
                FluidTankBlockEntity.class);
        FluidTankBlockEntity oil = placeFacing(helper, new BlockPos(5, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.WEST,
                FluidTankBlockEntity.class);
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.COPPER_PIPE.get());
        }
        FluidTankBlockEntity a = placeFacing(helper, new BlockPos(2, 1, 2), ModBlocks.FLUID_TANK.get(), Direction.SOUTH,
                FluidTankBlockEntity.class);
        FluidTankBlockEntity b = placeFacing(helper, new BlockPos(4, 1, 2), ModBlocks.FLUID_TANK.get(), Direction.SOUTH,
                FluidTankBlockEntity.class);
        water.tank().setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 4000));
        oil.tank().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("crude_oil"), 4000));
        helper.runAfterDelay(PIPES_NO_MIX_TICKS - 10, () -> {
            IhFluidStack sa = a.tank().stack();
            IhFluidStack sb = b.tank().stack();
            helper.assertTrue(!sa.isEmpty() && !sb.isEmpty(), "Empfänger leer: " + sa + " / " + sb);
            helper.assertTrue(sa.fluid().equals(sb.fluid()), "Fluide gemischt: " + sa + " / " + sb);
            helper.assertTrue(water.tank().stack().amount() == 4000 || oil.tank().stack().amount() == 4000,
                    "Beide Quellen haben abgegeben: " + water.tank().stack() + " / " + oil.tank().stack());
            helper.succeed();
        });
    }

    /** M2c: Ein leerer Eimer im Füllslot wird zu crude_oil_bucket. */
    public static void bucketFill(GameTestHelper helper) {
        FluidTankBlockEntity tank = placeFacing(helper, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH,
                FluidTankBlockEntity.class);
        tank.tank().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("crude_oil"), 2000));
        int fill = tank.layout().fillStart();
        tank.setItem(fill, new ItemStack(Items.BUCKET));
        Item oilBucket = ModFluids.get("crude_oil").bucket().get();
        helper.succeedWhen(() -> helper.assertTrue(tank.getItem(fill).is(oilBucket) && tank.tank().stack().amount() == 1000,
                "Füllslot: " + tank.getItem(fill) + ", Tank " + tank.tank().stack()));
    }

    /** Ein Verbraucher, der nur die Loader-API nutzt, erhält Strom aus dem Kabel. */
    public static void cablePlatformConsumerReceivesEnergy(GameTestHelper helper) {
        generatorWithCoal(helper);
        helper.setBlock(END, Blocks.TARGET);
        BlockPos abs = helper.absolutePos(END);
        helper.succeedWhen(() -> {
            long amount = Services.ENERGY.testConsumerAmount(helper.getLevel(), abs);
            helper.assertTrue(amount > 0, "Test-Verbraucher hat keine Energie erhalten (" + amount + ")");
        });
    }
}
