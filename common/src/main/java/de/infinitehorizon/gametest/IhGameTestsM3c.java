package de.infinitehorizon.gametest;

import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.world.GotoCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M3c (27.5): in der Dimension {@code infinite_horizon:moon} und der Befehl {@code goto}. */
public final class IhGameTestsM3c {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");

    private IhGameTestsM3c() {
    }

    private static void assertMoon(GameTestHelper h) {
        h.assertTrue(h.getLevel().dimension().identifier().equals(MOON),
                "Test läuft nicht auf dem Mond, sondern in " + h.getLevel().dimension().identifier());
    }

    /** {@code moon_aluminum_ore} droppt mit Steinspitzhacke genau 1 {@code bauxite} (13.4). */
    public static void moonAluminumOreDrops(GameTestHelper h) {
        assertMoon(h);
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockState ore = ModBlocks.simple("moon_aluminum_ore").get().defaultBlockState();
        h.setBlock(rel, ore);
        List<ItemStack> drops = Block.getDrops(ore, h.getLevel(), h.absolutePos(rel), null, null,
                new ItemStack(Items.STONE_PICKAXE));
        int bauxite = drops.stream().filter(s -> s.is(ModItems.simple("bauxite").get())).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(drops.size() == 1 && bauxite == 1, "Drops " + drops);
        h.succeed();
    }

    /** Ein Spieler auf dem Mond hat den Schwerkraft-Modifikator ohne Override (12.9). */
    public static void moonGravityWithoutOverride(GameTestHelper h) {
        assertMoon(h);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        h.assertTrue(p.level() == h.getLevel(), "Testspieler nicht auf dem Mond: " + p.level().dimension().identifier());
        GravityService.apply(p);
        double gravity = p.getAttributeValue(Attributes.GRAVITY);
        IhTestPlayers.remove(p);
        h.assertTrue(Math.abs(gravity - 0.0136) < 1e-6, "Schwerkraft " + gravity);
        h.succeed();
    }

    /** {@code goto moon} landet auf festem Boden mit Luft darüber (12.10, 12.5). */
    public static void gotoLandsOnSolidGround(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        ServerLevel moon = GotoCommand.resolve(h.getLevel().getServer(), Identifier.withDefaultNamespace("moon"));
        h.assertTrue(moon != null, "Dimension infinite_horizon:moon fehlt");
        BlockPos feet = GotoCommand.teleport(p, moon, 0, 0);
        ServerPlayer moved = moon.getServer().getPlayerList().getPlayer(p.getUUID());
        h.assertTrue(moved != null && moved.level() == moon, "Spieler nicht auf dem Mond");
        BlockState ground = moon.getBlockState(feet.below());
        h.assertTrue(!ground.isAir() && ground.getFluidState().isEmpty(), "kein fester Boden: " + ground);
        h.assertTrue(moon.getBlockState(feet).isAir() && moon.getBlockState(feet.above()).isAir(), "kein Platz über dem Boden");
        int surface = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, feet.getX(), feet.getZ());
        h.assertTrue(feet.getY() == surface, "Landung bei y " + feet.getY() + " statt auf der Oberfläche (y " + surface + ")");
        IhTestPlayers.remove(moved);
        h.succeed();
    }
}
