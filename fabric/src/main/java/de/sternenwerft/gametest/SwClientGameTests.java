package de.sternenwerft.gametest;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.multiblock.GhostPreview;
import net.minecraft.resources.Identifier;
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

import java.util.List;
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

            // Config-Sync (3A.5): der Client kennt nach dem Beitritt die Serverwerte
            String freq = context.computeOnClient(c -> de.sternenwerft.data.ServerConfig.client().getString("event_frequency"));
            if (!"normal".equals(freq)) {
                throw new AssertionError("SyncConfigPayload nicht angekommen: event_frequency = " + freq);
            }

            sideButtonChangesServer(context, sp);

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
            machine(context, sp, ModBlocks.FLUID_TANK.get(), "gui_fluid_tank", be -> {
                ((de.sternenwerft.machine.FluidTankBlockEntity) be).tank().setStackUnchecked(
                        de.sternenwerft.core.fluid.SwFluidStack.of(de.sternenwerft.Sternenwerft.id("crude_oil"), 9000));
                be.setItem(be.layout().fillStart(), new ItemStack(Items.BUCKET));
            });
            machine(context, sp, ModBlocks.FLUID_PUMP.get(), "gui_fluid_pump", be -> {
                ((de.sternenwerft.machine.FluidPumpBlockEntity) be).tank().setStackUnchecked(
                        de.sternenwerft.core.fluid.SwFluidStack.of(net.minecraft.resources.Identifier.withDefaultNamespace("water"), 4000));
                be.setItem(be.layout().upgradeStart(), new ItemStack(ModItems.simple("speed_upgrade").get(), 1));
                fillEnergy(be, 2);
            });
            machine(context, sp, ModBlocks.COMBUSTION_GENERATOR.get(), "gui_combustion_generator", be ->
                    be.setItem(0, new ItemStack(Items.COAL, 12)));
            machine(context, sp, ModBlocks.ASSEMBLY_TABLE.get(), "gui_assembly_table", be -> {
                be.setItem(0, new ItemStack(ModItems.simple("silicon").get(), 2));
                be.setItem(1, new ItemStack(ModItems.simple("copper_wire").get(), 4));
                be.setItem(2, new ItemStack(ModItems.simple("plastic").get(), 1));
                be.setItem(be.layout().fillStart(), new ItemStack(Items.BUCKET));
                fillEnergy(be, 2);
            });
            tankButtonChangesServer(context, sp);
            refinery(context, sp);

            // Alle Blöcke nebeneinander in der Welt (Modelle, Texturen, Statuslampe)
            BlockPos base = context.computeOnClient(c -> BlockPos.containing(c.player.position()).north(3));
            Block[] row = {ModBlocks.CRUSHER.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(),
                    ModBlocks.COMBUSTION_GENERATOR.get(), ModBlocks.COPPER_CABLE.get(), ModBlocks.COPPER_CABLE.get()};
            // Dritte Reihe: Maschinen und Teile aus M2d (Ports im Modus Eingang/Ausgang)
            String[] row3 = {"assembly_table[facing=south]", "electrolyzer[facing=south]", "refinery_controller[facing=south]",
                    "refinery_casing", "item_port", "fluid_port[mode=output]", "energy_port"};
            for (int i = 0; i < row3.length; i++) {
                BlockPos p = base.above(2).east(i - 2);
                sp.getServer().runCommand("setblock %d %d %d sternenwerft:%s".formatted(p.getX(), p.getY(), p.getZ(), row3[i]));
            }
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
            context.getInput().lookAt(base.above());
            context.waitTicks(5);
            clearChat(context);
            context.takeScreenshot("world_machines");
        }
    }

    /** Knopf „Tank“ im Elektrolyseur: wählt Tank 2 der Ausgänge (Sauerstoff) auf dem Server (4.1). */
    private static void tankButtonChangesServer(ClientGameTestContext context, TestSingleplayerContext sp) {
        BlockPos pos = openMachine(context, sp, ModBlocks.ELECTROLYZER.get(), be -> {
            List<SwFluidTank> tanks = be.fluidTanks();
            tanks.get(0).setStackUnchecked(SwFluidStack.of(Identifier.withDefaultNamespace("water"), 8000));
            tanks.get(1).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("hydrogen"), 6000));
            tanks.get(2).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("oxygen"), 3000));
            be.setItem(be.layout().upgradeStart(), new ItemStack(ModItems.simple("speed_upgrade").get(), 1));
            fillEnergy(be, 2);
        });
        context.takeScreenshot("gui_electrolyzer");
        context.runOnClient(c -> ((MachineScreen) c.gui.screen()).cycleTank());
        context.waitTicks(5);
        int selected = sp.getServer().computeOnServer(server ->
                sp.getConnection().getServerLevel().getBlockEntity(pos) instanceof MachineBlockEntity be ? be.selectedTank() : -1);
        clearChat(context);
        context.takeScreenshot("gui_electrolyzer_tank2");
        closeAndRemove(context, sp, pos);
        if (selected != 1) {
            throw new AssertionError("Knopf „Tank“: Server-Tankwahl ist " + selected + ", erwartet 1");
        }
    }

    /**
     * Raffinerie (4.3): Aufbau per fill-Befehl, Controller mit Blick zum Spieler (Westen), GUI mit Statuslampe; danach
     * fehlt ein Gehäuse und Sneak-Rechtsklick mit leerer Hand startet die Geisterprojektion (kein GUI).
     */
    private static void refinery(ClientGameTestContext context, TestSingleplayerContext sp) {
        BlockPos pos = context.computeOnClient(c -> BlockPos.containing(c.player.position()).east(2).above());
        // Vorderseite Westen: Aufbau nach Norden/Süden (±1), Osten (2 nach hinten) und 4 nach oben
        sp.getServer().runCommand("fill %d %d %d %d %d %d sternenwerft:refinery_casing".formatted(
                pos.getX(), pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 1));
        sp.getServer().runCommand("setblock %d %d %d sternenwerft:refinery_controller[facing=west]".formatted(
                pos.getX(), pos.getY(), pos.getZ()));
        context.waitFor(c -> c.level.getBlockState(pos).is(ModBlocks.REFINERY_CONTROLLER.get()));
        sp.getServer().runOnServer(server -> {
            if (sp.getConnection().getServerLevel().getBlockEntity(pos) instanceof MachineBlockEntity be) {
                be.refreshLayout();
                be.fluidTanks().get(0).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("crude_oil"), 8000));
                be.fluidTanks().get(1).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("kerosene"), 3000));
                fillEnergy(be, 2);
            }
        });
        context.waitTicks(10);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(MachineScreen.class);
        context.waitTicks(10);
        clearChat(context);
        context.takeScreenshot("gui_refinery_controller");
        context.setScreen(() -> null);
        // Gehäuse in der Vorderseite entfernen (vom Spieler aus sichtbar) → Geisterprojektion
        BlockPos gap = pos.above(2).south();
        sp.getServer().runCommand("setblock %d %d %d minecraft:air".formatted(gap.getX(), gap.getY(), gap.getZ()));
        // leere Hand: aufgesammelte Maschineninhalte aus den vorigen Schritten entfernen
        sp.getServer().runCommand("clear @a");
        context.waitTicks(5);
        context.getInput().holdKey(options -> options.keyShift);
        context.waitTicks(3);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
        boolean ghost = context.computeOnClient(c -> GhostPreview.active(c.level, pos));
        boolean noGui = context.computeOnClient(c -> c.gui.screen() == null);
        context.getInput().lookAt(gap);
        context.waitTicks(10);
        clearChat(context);
        context.takeScreenshot("world_refinery_ghost");
        context.getInput().releaseKey(options -> options.keyShift);
        context.waitTicks(3);
        sp.getServer().runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(
                pos.getX(), pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 1));
        context.waitTicks(3);
        if (!ghost || !noGui) {
            throw new AssertionError("Geisterprojektion aktiv=" + ghost + ", ohne GUI=" + noGui);
        }
    }

    /** Knopf „Seite“ im echten GUI: schickt MachineConfigPayload, der Server ändert die Ausgangsseite (Nord → Süd). */
    private static void sideButtonChangesServer(ClientGameTestContext context, TestSingleplayerContext sp) {
        BlockPos pos = context.computeOnClient(c -> BlockPos.containing(c.player.position()).east(2).above());
        setBlock(sp, pos, ModBlocks.FLUID_TANK.get());
        context.waitFor(c -> c.level.getBlockState(pos).is(ModBlocks.FLUID_TANK.get()));
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(MachineScreen.class);
        context.waitTicks(5);
        context.runOnClient(c -> ((MachineScreen) c.gui.screen()).cycleOutputSide());
        context.waitTicks(5);
        String side = sp.getServer().computeOnServer(server ->
                sp.getConnection().getServerLevel().getBlockEntity(pos) instanceof MachineBlockEntity be ? be.outputSide().getSerializedName() : "?");
        clearChat(context);
        context.takeScreenshot("gui_fluid_tank_side_south");
        context.setScreen(() -> null);
        sp.getServer().runCommand("setblock %d %d %d minecraft:air".formatted(pos.getX(), pos.getY(), pos.getZ()));
        context.waitTick();
        if (!"south".equals(side)) {
            throw new AssertionError("Knopf „Seite“: Server-Ausgangsseite ist " + side + ", erwartet south");
        }
    }

    private static void machine(ClientGameTestContext context, TestSingleplayerContext sp, Block block, String name,
                                Consumer<MachineBlockEntity> setup) {
        BlockPos pos = openMachine(context, sp, block, setup);
        clearChat(context);
        context.takeScreenshot(name);
        closeAndRemove(context, sp, pos);
    }

    /** Setzt die Maschine neben den Spieler, füllt sie und öffnet ihr GUI per Rechtsklick; das GUI bleibt offen. */
    private static BlockPos openMachine(ClientGameTestContext context, TestSingleplayerContext sp, Block block,
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
        return pos;
    }

    private static void closeAndRemove(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos pos) {
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
