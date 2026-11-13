package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.food.FoodRules;
import de.infinitehorizon.food.HydroponicsBlockEntity;
import de.infinitehorizon.food.Nutrition;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** GameTests für M5h (17): Nahrungspacker, Kompostboden, Hydrokultur-Becken. */
public final class IhGameTestsM5h {

    private static final Identifier WATER = Identifier.withDefaultNamespace("water");
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);

    private IhGameTestsM5h() {
    }

    private static <T extends ProcessingMachineBlockEntity> T machine(GameTestHelper h, Block block, Class<T> type) {
        h.setBlock(MACHINE, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(MACHINE, type);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** Becken mit Wasser und Licht darüber. */
    private static HydroponicsBlockEntity basin(GameTestHelper h, ItemStack plant) {
        HydroponicsBlockEntity be = machine(h, ModBlocks.HYDROPONICS_BASIN.get(), HydroponicsBlockEntity.class);
        h.setBlock(MACHINE.above(), Blocks.LIGHT);
        be.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(WATER, be.fluidTanks().getFirst().capacity()));
        be.setItem(0, plant);
        return be;
    }

    /** 3 Nahrungsmittel → 1 {@code space_ration}, Nahrung und Sättigung = Summe (max. 20), höchstens 110 t. */
    public static void foodPackerRation(GameTestHelper h) {
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.FOOD_PACKER.get(), ProcessingMachineBlockEntity.class);
        List<ItemStack> in = List.of(new ItemStack(Items.BREAD), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.BAKED_POTATO));
        List<FoodRules.Food> foods = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            FoodProperties f = in.get(i).get(DataComponents.FOOD);
            foods.add(new FoodRules.Food(f.nutrition(), f.saturation()));
            be.setItem(i, in.get(i).copy());
        }
        var cfg = BalanceManager.get().entry("agriculture", InfiniteHorizon.id("space_ration").toString()).orElseThrow();
        Nutrition want = FoodRules.pack(foods, cfg.get("nutrition_max").getAsInt(), cfg.get("saturation_max").getAsFloat());
        h.succeedWhen(() -> {
            ItemStack out = be.getItem(be.layout().outputStart());
            h.assertTrue(out.is(ModItems.SPACE_RATION.get()), "keine Ration: " + out);
            h.assertTrue(want.equals(out.get(ModDataComponents.NUTRITION.get())), "Nährwert " + out.get(ModDataComponents.NUTRITION.get()));
            FoodProperties f = out.get(DataComponents.FOOD);
            h.assertTrue(f != null && f.nutrition() == want.nutrition() && f.saturation() == want.saturation(), "food " + f);
            h.assertTrue(be.getItem(0).isEmpty(), "Zutaten nicht verbraucht");
        });
    }

    /** Hacke auf {@code compost_soil} → {@code compost_farmland} (trägt Feldfrüchte), Austrocknen → Kompostboden. */
    public static void compostSoilTills(GameTestHelper h) {
        BlockPos soil = new BlockPos(2, 1, 2);
        BlockPos regolith = new BlockPos(4, 1, 2);
        h.setBlock(soil, ModBlocks.simple("compost_soil").get());
        h.setBlock(regolith, ModBlocks.simple("moon_regolith").get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(3.5, 2, 0.5));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
            for (BlockPos rel : List.of(soil, regolith)) {
                BlockPos abs = h.absolutePos(rel);
                p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
            }
            BlockPos abs = h.absolutePos(soil);
            h.assertTrue(h.getLevel().getBlockState(abs).is(ModBlocks.COMPOST_FARMLAND.get()), "kein Kompost-Ackerland");
            h.assertTrue(h.getLevel().getBlockState(abs).is(BlockTags.SUPPORTS_CROPS), "trägt keine Feldfrüchte");
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(regolith)).is(ModBlocks.simple("moon_regolith").get()),
                    "Planetenboden umgegraben");
            ((FarmlandBlock) ModBlocks.COMPOST_FARMLAND.get()).turnToBaseBlock(null, h.getLevel().getBlockState(abs),
                    h.getLevel(), abs);
            h.assertTrue(h.getLevel().getBlockState(abs).is(ModBlocks.simple("compost_soil").get()), "trocknet nicht zu Kompostboden");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Becken: eine Altersstufe in {@code ticks_per_growth_stage} (± 10 %), reif → Ernte in den Ausgabeslot, Samen bleibt. */
    public static void hydroponicsStageAndHarvest(GameTestHelper h) {
        HydroponicsBlockEntity be = basin(h, new ItemStack(Items.WHEAT_SEEDS));
        int last = HydroponicsBlockEntity.plant(be.getItem(0)).stages() - 1;
        be.setStage(last);
        int ticks = BalanceManager.get().entry("agriculture", InfiniteHorizon.id("hydroponics_basin").toString()).orElseThrow()
                .get("ticks_per_growth_stage").getAsInt();
        long start = h.getTick();
        h.succeedWhen(() -> {
            h.assertTrue(be.countOutput(Items.WHEAT) > 0, "noch keine Ernte (Stufe " + be.stage() + ")");
            h.assertTrue(h.getTick() - start >= ticks * 9 / 10, "zu schnell: " + (h.getTick() - start) + " t");
            h.assertTrue(be.getItem(0).is(Items.WHEAT_SEEDS), "Samen nicht im Pflanzslot");
            h.assertTrue(be.stage() == 0, "Stufe nach der Ernte " + be.stage());
        });
    }

    /** Setzling: reif → Stämme der Holzart ({@code saplings.yield}), Setzling bleibt. */
    public static void hydroponicsSaplingLogs(GameTestHelper h) {
        HydroponicsBlockEntity be = basin(h, new ItemStack(Items.BIRCH_SAPLING));
        be.setStage(HydroponicsBlockEntity.plant(be.getItem(0)).stages());
        int logs = BalanceManager.get().entry("agriculture", "saplings").orElseThrow().getAsJsonArray("yield").get(0)
                .getAsJsonObject().get("count").getAsInt();
        h.succeedWhen(() -> {
            h.assertTrue(be.countOutput(Items.BIRCH_LOG) == logs, "Birkenstämme " + be.countOutput(Items.BIRCH_LOG));
            h.assertTrue(be.getItem(0).is(Items.BIRCH_SAPLING), "Setzling nicht im Pflanzslot");
        });
    }

    /** Override Mond ohne Kuppel: kein Wachstum, Hinweis {@code status.growth}. */
    public static void hydroponicsNeedsDome(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 40);
        HydroponicsBlockEntity be = basin(h, new ItemStack(Items.WHEAT_SEEDS));
        h.runAfterDelay(20, () -> {
            try {
                h.assertTrue(be.statusHint() == HydroponicsBlockEntity.HINT_GROWTH, "Hinweis " + be.statusHint());
                h.assertTrue(be.stage() == 0, "Stufe " + be.stage());
            } finally {
                EnvironmentOverrides.remove(override);
            }
            h.succeed();
        });
    }
}
