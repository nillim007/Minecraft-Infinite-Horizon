package de.sternenwerft.gametest;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.machine.ProcessingMachineBlockEntity;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/**
 * GameTests für M2f (27.3): Join-Geschenk und die Beispielrezepte aus 27.2, die noch keinen eigenen Test hatten
 * (Zerkleinerer mit Bauxit, Elektroofen mit Quarzsand, Presse). Zusammen mit den Tests aus M2a–M2e laufen damit alle
 * Beispielrezepte in einem GameTest-Durchlauf.
 */
public final class SwGameTestsM2f {

    private SwGameTestsM2f() {
    }

    private static ProcessingMachineBlockEntity machine(GameTestHelper h, BlockPos pos, Block block) {
        h.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ProcessingMachineBlockEntity be = h.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
        be.refreshLayout();
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
        return be;
    }

    private static Item simple(String path) {
        return ModItems.simple(path).get();
    }

    /** Das Handbuch kommt beim ersten Join genau einmal (24.1). */
    public static void handbookGiftOnce(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        Sternenwerft.giveHandbookOnce(p);
        Sternenwerft.giveHandbookOnce(p);
        int n = p.getInventory().countItem(ModItems.ENGINEERS_HANDBOOK.get());
        h.assertTrue(n == 1, "Handbücher im Inventar: " + n + ", erwartet 1");
        h.assertTrue(p.entityTags().contains(Sternenwerft.HANDBOOK_GIVEN_TAG), "Merker fehlt");
        h.succeed();
    }

    /** 1 Bauxit → 2 Aluminiumstaub im Zerkleinerer in höchstens 220 t (27.2). */
    public static void crusherBauxite(GameTestHelper h) {
        ProcessingMachineBlockEntity c = machine(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get());
        c.setItem(0, new ItemStack(simple("bauxite")));
        Item dust = simple("aluminum_dust");
        h.succeedWhen(() -> h.assertTrue(c.countOutput(dust) == 2, "Aluminiumstaub " + c.countOutput(dust)));
    }

    /** 1 Quarzsand → 1 Silizium im Elektroofen (electric_smelting) in höchstens 110 t (27.2). */
    public static void furnaceQuartzSand(GameTestHelper h) {
        ProcessingMachineBlockEntity f = machine(h, new BlockPos(1, 1, 1), ModBlocks.ELECTRIC_FURNACE.get());
        f.setItem(0, new ItemStack(ModBlocks.simple("quartz_sand").get()));
        Item silicon = simple("silicon");
        h.succeedWhen(() -> h.assertTrue(f.countOutput(silicon) == 1, "Silizium " + f.countOutput(silicon)));
    }

    /** 1 Titanbarren + Pressform → 1 Titanplatte in höchstens 110 t (27.2). */
    public static void pressTitaniumPlate(GameTestHelper h) {
        MachineBlockEntity p = machine(h, new BlockPos(1, 1, 1), ModBlocks.PRESS.get());
        p.setItem(0, new ItemStack(simple("titanium_ingot")));
        p.setItem(p.layout().moldStart(), new ItemStack(simple("press_mold_plate")));
        Item plate = simple("titanium_plate");
        h.succeedWhen(() -> h.assertTrue(((ProcessingMachineBlockEntity) p).countOutput(plate) == 1, "Titanplatte fehlt"));
    }
}
