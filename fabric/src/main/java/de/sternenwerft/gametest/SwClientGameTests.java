package de.sternenwerft.gametest;

import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.machine.MachineLayout;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * Client-GameTest mit Screenshots (nur bei {@code -Dfabric.client.gametest}): jedes Maschinen-GUI einmal öffnen und
 * fotografieren, dazu die Blöcke in der Welt. Die CI legt die Bilder im Branch {@code ci-screenshots} ab.
 */
public class SwClientGameTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder()
                .adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            sp.getConnection().waitForChunksRender();

            machine(context, sp, ModBlocks.CRUSHER.get(), "gui_crusher", be -> {
                be.setItem(0, new ItemStack(Items.RAW_IRON, 8));
                fillEnergy(be, 2);
            });
            machine(context, sp, ModBlocks.ELECTRIC_FURNACE.get(), "gui_electric_furnace", be -> {
                be.setItem(0, new ItemStack(ModItems.simple("titanium_dust").get(), 4));
                be.setItem(1, new ItemStack(ModItems.simple("iron_dust").get(), 4));
                MachineLayout l = be.layout();
                be.setItem(l.upgradeStart(), new ItemStack(ModItems.simple("speed_upgrade").get(), 2));
                fillEnergy(be, 1);
            });
            machine(context, sp, ModBlocks.PRESS.get(), "gui_press", be -> {
                MachineLayout l = be.layout();
                be.setItem(0, new ItemStack(ModItems.simple("titanium_ingot").get(), 16));
                be.setItem(l.moldStart(), new ItemStack(ModItems.simple("press_mold_plate").get()));
                be.setItem(l.upgradeStart() + 1, new ItemStack(ModItems.simple("efficiency_upgrade").get(), 4));
                fillEnergy(be, 1);
            });
            machine(context, sp, ModBlocks.COMBUSTION_GENERATOR.get(), "gui_combustion_generator", be ->
                    be.setItem(0, new ItemStack(Items.COAL, 12)));

            // Alle Blöcke nebeneinander in der Welt (Modelle, Texturen, Statuslampe)
            BlockPos base = context.computeOnClient(c -> BlockPos.containing(c.player.position()).north(3));
            Block[] row = {ModBlocks.CRUSHER.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(),
                    ModBlocks.COMBUSTION_GENERATOR.get(), ModBlocks.COPPER_CABLE.get(), ModBlocks.COPPER_CABLE.get()};
            for (int i = 0; i < row.length; i++) {
                // Vorderseite zum Spieler (Süden); Kabel ignorieren die Eigenschaft nicht, daher nur für Maschinen
                String state = i < 4 ? "[facing=south]" : "";
                BlockPos p = base.east(i - 2);
                sp.getServer().runCommand("setblock %d %d %d %s%s".formatted(p.getX(), p.getY(), p.getZ(),
                        BuiltInRegistries.BLOCK.getKey(row[i]), state));
            }
            // Zweite Reihe darüber: alle einfachen Blöcke (Erze, Quarzsand)
            int k = 0;
            for (var entry : ModBlocks.simpleBlocks().values()) {
                setBlock(sp, base.above().east(k - 2), entry.get());
                k++;
            }
            context.waitTicks(5);
            // Zerkleinerer laufen lassen (grüne Lampe), Generator mit Kohle
            BlockPos crusherPos = base.east(-2);
            BlockPos generatorPos = base.east(1);
            sp.getServer().runOnServer(server -> {
                ServerLevel level = sp.getConnection().getServerLevel();
                if (level.getBlockEntity(crusherPos) instanceof MachineBlockEntity be) {
                    be.refreshLayout();
                    be.setItem(0, new ItemStack(Items.RAW_IRON, 8));
                    fillEnergy(be, 1);
                }
                if (level.getBlockEntity(generatorPos) instanceof MachineBlockEntity be) {
                    be.refreshLayout();
                    be.setItem(0, new ItemStack(Items.COAL, 4));
                }
            });
            context.waitTicks(20);
            context.getInput().lookAt(base);
            context.waitTicks(5);
            clearChat(context);
            context.takeScreenshot("world_machines");
        }
    }

    private static void machine(ClientGameTestContext context, TestSingleplayerContext sp, Block block, String name,
                                Consumer<MachineBlockEntity> setup) {
        BlockPos pos = context.computeOnClient(c -> BlockPos.containing(c.player.position()).east(2).above());
        setBlock(sp, pos, block);
        context.waitFor(c -> c.level.getBlockState(pos).is(block));
        sp.getServer().runOnServer(server -> {
            ServerLevel level = sp.getConnection().getServerLevel();
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
                be.refreshLayout();
                setup.accept(be);
            }
        });
        context.waitTicks(10);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(MachineScreen.class);
        context.waitTicks(10);
        clearChat(context);
        context.takeScreenshot(name);
        context.setScreen(() -> null);
        sp.getServer().runCommand("setblock %d %d %d minecraft:air".formatted(pos.getX(), pos.getY(), pos.getZ()));
        context.waitTick();
    }

    /** Befehlsrückmeldungen im Chat würden die Bilder verdecken. */
    private static void clearChat(ClientGameTestContext context) {
        context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
        context.waitTick();
    }

    private static void setBlock(TestSingleplayerContext sp, BlockPos pos, Block block) {
        sp.getServer().runCommand("setblock %d %d %d %s".formatted(pos.getX(), pos.getY(), pos.getZ(),
                BuiltInRegistries.BLOCK.getKey(block)));
    }

    /** Energie auf einen Anteil 1/divisor der Kapazität setzen, damit die Leiste etwas zeigt. */
    private static void fillEnergy(MachineBlockEntity be, int divisor) {
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity() / divisor);
    }
}
