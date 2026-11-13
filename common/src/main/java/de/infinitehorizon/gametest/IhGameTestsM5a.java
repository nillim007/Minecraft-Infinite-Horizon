package de.infinitehorizon.gametest;

import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.LandingService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** GameTests für M5a (13.4): Mars- und Venus-Blöcke mit ihren Drops, Landeplatz auf der Venus. */
public final class IhGameTestsM5a {

    private IhGameTestsM5a() {
    }

    private static int count(List<ItemStack> drops, String item) {
        return drops.stream().filter(s -> s.is(ModItems.simple(item).get())).mapToInt(ItemStack::getCount).sum();
    }

    private static List<ItemStack> drops(GameTestHelper h, String block) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockState state = ModBlocks.simple(block).get().defaultBlockState();
        h.setBlock(rel, state);
        return Block.getDrops(state, h.getLevel(), h.absolutePos(rel), null, null, new ItemStack(Items.IRON_PICKAXE));
    }

    /** 13.4: Magnesiumerz → 1 {@code raw_magnesium}; Meteorit → 1–2 {@code raw_nickel}; Schwefelerz → 2–4 {@code sulfur}. */
    public static void ringOneOreDrops(GameTestHelper h) {
        List<ItemStack> mg = drops(h, "mars_magnesium_ore");
        h.assertTrue(count(mg, "raw_magnesium") == 1, "Magnesiumerz: " + mg);
        List<ItemStack> met = drops(h, "iron_nickel_meteorite");
        int ni = count(met, "raw_nickel");
        h.assertTrue(ni >= 1 && ni <= 2, "Meteorit: " + met);
        List<ItemStack> s = drops(h, "venus_sulfur_ore");
        int sulfur = count(s, "sulfur");
        h.assertTrue(sulfur >= 2 && sulfur <= 4, "Schwefelerz: " + s);
        List<ItemStack> g = drops(h, "gypsum_block");
        h.assertTrue(count(g, "gypsum") == 4, "Gipsblock: " + g);
        h.succeed();
    }

    /** 12.5, 13.4: Auf der Venus (Lavameer bis y 40) liegt der Landeplatz auf festem Boden ohne Lava. */
    public static void venusLandingOnSolidGround(GameTestHelper h) {
        ServerLevel venus = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("infinite_horizon", "venus")));
        h.assertTrue(venus != null, "Dimension Venus fehlt");
        BlockPos at = LandingService.find(venus, 0, 0);
        BlockState below = venus.getBlockState(at.below());
        h.assertTrue(below.isSolid() && venus.getFluidState(at.below()).isEmpty() && venus.getFluidState(at).isEmpty(),
                "Landeplatz " + at + " über " + below);
        h.succeed();
    }
}
