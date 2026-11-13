package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.client.MachineScreen;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.client.handbook.HandbookClient;
import de.infinitehorizon.client.handbook.HandbookScreen;
import de.infinitehorizon.compat.RecipeViewerHooks;
import de.infinitehorizon.multiblock.GhostPreview;
import net.minecraft.resources.Identifier;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
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
public class IhClientGameTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder()
                .adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            sp.getConnection().waitForChunksRender();

            // Config-Sync (3A.5): der Client kennt nach dem Beitritt die Serverwerte
            String freq = context.computeOnClient(c -> de.infinitehorizon.data.ServerConfig.client().getString("event_frequency"));
            if (!"normal".equals(freq)) {
                throw new AssertionError("SyncConfigPayload nicht angekommen: event_frequency = " + freq);
            }

            // Geschenk beim ersten Join (24.1): genau ein Handbuch im Inventar
            int books = context.computeOnClient(c -> c.player.getInventory().countItem(ModItems.ENGINEERS_HANDBOOK.get()));
            if (books != 1) {
                throw new AssertionError("Handbuch-Geschenk: " + books + " Stück, erwartet 1");
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
                ((de.infinitehorizon.machine.FluidTankBlockEntity) be).tank().setStackUnchecked(
                        de.infinitehorizon.core.fluid.IhFluidStack.of(de.infinitehorizon.InfiniteHorizon.id("crude_oil"), 9000));
                be.setItem(be.layout().fillStart(), new ItemStack(Items.BUCKET));
            });
            machine(context, sp, ModBlocks.FLUID_PUMP.get(), "gui_fluid_pump", be -> {
                ((de.infinitehorizon.machine.FluidPumpBlockEntity) be).tank().setStackUnchecked(
                        de.infinitehorizon.core.fluid.IhFluidStack.of(net.minecraft.resources.Identifier.withDefaultNamespace("water"), 4000));
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
            machine(context, sp, ModBlocks.FUEL_GENERATOR.get(), "gui_fuel_generator", be -> {
                be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("kerosene"), 9000));
                be.setItem(be.layout().fillStart(), new ItemStack(Items.BUCKET));
            });
            machine(context, sp, ModBlocks.BATTERY.get(), "gui_battery", be -> {
                fillEnergy(be, 2);
                be.setItem(0, new ItemStack(ModItems.BATTERY.get()));
            });
            handbook(context);
            recipeViewer(context);
            researchTree(context, sp);
            // M4b: Analysator (mit Besitzer, Probe und Strom) und Solarpanel
            machine(context, sp, ModBlocks.ANALYZER.get(), "gui_analyzer", be -> {
                var server = be.getLevel().getServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                be.setOwner(de.infinitehorizon.agency.AgencyManager.ensureFor(player).id());
                be.setItem(0, de.infinitehorizon.research.SampleTool.create(InfiniteHorizon.id("bauxite_ore"),
                        net.minecraft.resources.Identifier.withDefaultNamespace("overworld")));
                fillEnergy(be, 2);
            });
            machine(context, sp, ModBlocks.SOLAR_PANEL.get(), "gui_solar_panel", be -> fillEnergy(be, 4));

            // Alle Blöcke nebeneinander in der Welt (Modelle, Texturen, Statuslampe)
            BlockPos base = context.computeOnClient(c -> BlockPos.containing(c.player.position()).north(3));
            Block[] row = {ModBlocks.CRUSHER.get(), ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.PRESS.get(),
                    ModBlocks.COMBUSTION_GENERATOR.get(), ModBlocks.COPPER_CABLE.get(), ModBlocks.COPPER_CABLE.get()};
            // Dritte Reihe: Maschinen und Teile aus M2d (Ports im Modus Eingang/Ausgang)
            String[] row3 = {"assembly_table[facing=south]", "electrolyzer[facing=south]", "refinery_controller[facing=south]",
                    "refinery_casing", "item_port", "fluid_port[mode=output]", "energy_port", "fuel_generator[facing=south]",
                    "battery[facing=south]"};
            for (int i = 0; i < row3.length; i++) {
                BlockPos p = base.above(2).east(i - 2);
                sp.getServer().runCommand("setblock %d %d %d infinite_horizon:%s".formatted(p.getX(), p.getY(), p.getZ(), row3[i]));
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

            moon(context, sp);
        }
    }

    /**
     * Mond (27.5 M3c): {@code goto moon}, Monster mit Atemmaske (13.5), Blick zur Erde am Himmel (25.1: Azimut 60°,
     * Höhe 40° → Yaw 240, Pitch −40); Screenshots.
     */
    private static void moon(ClientGameTestContext context, TestSingleplayerContext sp) {
        sp.getServer().runCommand("time set 6000");
        sp.getServer().runCommand("execute as @p run infinite_horizon goto moon");
        context.waitTicks(40);
        sp.getConnection().waitForChunksRender();
        String dim = context.computeOnClient(c -> c.level.dimension().identifier().toString());
        if (!"infinite_horizon:moon".equals(dim)) {
            throw new AssertionError("goto moon: Spieler ist in " + dim);
        }
        int bodies = context.computeOnClient(c -> de.infinitehorizon.client.PlanetVisuals.bodies().size());
        if (bodies == 0) {
            throw new AssertionError("Mondhimmel: keine Himmelskörper (Planeten-Sync?)");
        }
        // Landeplatz protokollieren (Spalte, Höhenkarten), Nachtsicht für helle Screenshots
        sp.getServer().runOnServer(server -> {
            net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            net.minecraft.server.level.ServerLevel moon = p.level();
            BlockPos at = p.blockPosition();
            StringBuilder col = new StringBuilder("Mond-Landeplatz " + at + ", Höhenkarte WORLD_SURFACE "
                    + moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ())
                    + ", Spalte:");
            for (int y = at.getY() + 64; y >= moon.getMinY(); y -= 8) {
                col.append(' ').append(y).append('=').append(net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getKey(moon.getBlockState(new BlockPos(at.getX(), y, at.getZ())).getBlock()).getPath());
            }
            de.infinitehorizon.InfiniteHorizon.LOG.info(col.toString());
            int surface = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ());
            if (at.getY() < surface) {
                throw new AssertionError("goto moon: Landung bei y " + at.getY() + " unter der Oberfläche (y " + surface + ")");
            }
        });
        sp.getServer().runCommand("effect give @p minecraft:night_vision infinite 0 true");
        context.runOnClient(c -> {
            c.player.setYRot(0);
            c.player.setXRot(10);
        });
        context.waitTicks(2);
        for (String cmd : new String[]{"summon minecraft:creeper ~ ~ ~4", "summon minecraft:spider ~2.5 ~ ~5",
                "summon minecraft:zombie ~-2.5 ~ ~5"}) {
            sp.getServer().runCommand("execute as @p at @s run " + cmd + " {NoAI:1b,PersistenceRequired:1b}");
        }
        context.waitTicks(20);
        clearChat(context);
        context.takeScreenshot("world_moon_mobs");
        // HUD (27.5 M3d): im Vakuum ohne Anzug steigt die Belastung, mit Anzug zeigt die Leiste den O₂-Vorrat
        sp.getServer().runCommand("gamemode survival @p");
        context.waitTicks(60);
        int oxygenBar = context.computeOnClient(c -> {
            var st = de.infinitehorizon.client.HazardHud.state();
            return st == null ? -1 : st.percentOf(0);
        });
        if (oxygenBar <= 0) {
            throw new AssertionError("HUD: keine Sauerstoff-Belastung im Vakuum (" + oxygenBar + ")");
        }
        clearChat(context);
        context.takeScreenshot("hud_hazard");
        for (String slot : new String[]{"head|helmet", "chest|chestplate[infinite_horizon:oxygen=600]", "legs|leggings", "feet|boots"}) {
            String[] p = slot.split("\\|");
            sp.getServer().runCommand("item replace entity @p armor." + p[0] + " with infinite_horizon:pressure_suit_" + p[1]);
        }
        context.waitTicks(40);
        int suitO2 = context.computeOnClient(c -> {
            var st = de.infinitehorizon.client.HazardHud.state();
            return st == null ? -2 : st.suitOxygen();
        });
        if (suitO2 <= 0) {
            throw new AssertionError("HUD: Anzug-O₂ fehlt (" + suitO2 + ")");
        }
        clearChat(context);
        context.takeScreenshot("hud_suit");
        sp.getServer().runCommand("gamemode creative @p");
        // Kuppel (27.5 M3e): Generator mit Strom und Sauerstoff vor dem Spieler, Lichtpunkte auf der Hülle
        sp.getServer().runOnServer(server -> {
            net.minecraft.server.level.ServerPlayer pl = server.getPlayerList().getPlayers().getFirst();
            BlockPos at = pl.blockPosition().relative(pl.getDirection(), 3);
            pl.level().setBlockAndUpdate(at, ModBlocks.DOME_GENERATOR_T1.get().defaultBlockState());
            if (pl.level().getBlockEntity(at) instanceof de.infinitehorizon.dome.DomeGeneratorBlockEntity dome) {
                dome.refreshLayout();
                dome.energyStorage().setAmountUnchecked(dome.energyStorage().getCapacity());
                dome.tank().setStackUnchecked(de.infinitehorizon.core.fluid.IhFluidStack.of(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("infinite_horizon", "oxygen"), 2000));
            }
        });
        context.waitTicks(40);
        int domes = context.computeOnClient(c -> de.infinitehorizon.client.DomeVisuals.domes().size());
        if (domes == 0) {
            throw new AssertionError("Kuppel: DomeStatePayload nicht angekommen");
        }
        clearChat(context);
        context.takeScreenshot("world_moon_dome");
        // Gelände von oben, dann der Himmel mit der Erde (frei von Hängen und Wänden)
        context.runOnClient(c -> {
            c.player.getAbilities().flying = true;
            c.player.onUpdateAbilities();
        });
        sp.getServer().runCommand("execute as @p at @s run tp @s ~ ~24 ~-8 0 35");
        context.waitTicks(40);
        sp.getConnection().waitForChunksRender();
        context.runOnClient(c -> {
            c.player.setYRot(0);
            c.player.setXRot(35);
        });
        context.waitTicks(10);
        context.takeScreenshot("world_moon_terrain");
        context.runOnClient(c -> {
            c.player.setYRot(240);
            c.player.setXRot(-40);
        });
        context.waitTicks(20);
        context.takeScreenshot("world_moon_sky");
    }

    /** Handbuch (24.1, 27.3 M2f): öffnen, einem Link folgen, Seite eines Items finden; Screenshots. */
    private static void handbook(ClientGameTestContext context) {
        context.runOnClient(c -> HandbookClient.open("infinite_horizon:start/willkommen"));
        context.waitForScreen(HandbookScreen.class);
        context.waitTicks(10);
        clearChat(context);
        context.takeScreenshot("gui_handbook");
        String link = context.computeOnClient(c -> ((HandbookScreen) c.gui.screen()).firstLink());
        context.runOnClient(c -> ((HandbookScreen) c.gui.screen()).openPage(link));
        context.waitTicks(5);
        String now = context.computeOnClient(c -> ((HandbookScreen) c.gui.screen()).currentPage());
        context.takeScreenshot("gui_handbook_link");
        String crusherPage = context.computeOnClient(c -> HandbookClient.pageFor(new ItemStack(ModBlocks.CRUSHER.get())));
        context.runOnClient(c -> ((HandbookScreen) c.gui.screen()).openPage(crusherPage));
        context.waitTicks(5);
        context.takeScreenshot("gui_handbook_crusher");
        context.setScreen(() -> null);
        context.waitTick();
        if (link == null || !link.equals(now) || crusherPage == null) {
            throw new AssertionError("Handbuch: Link=" + link + ", offen=" + now + ", Zerkleinerer-Seite=" + crusherPage);
        }
    }

    /**
     * REI (nur mit -PwithRei geladen, 3.9): Plugin aktiv, Rezepte angekommen, Kategorien Raffinieren und Montage öffnen
     * sich; Screenshots für die Sichtprüfung.
     */
    private static void recipeViewer(ClientGameTestContext context) {
        if (!Boolean.getBoolean("infinite_horizon.clienttest.rei")) {
            return;
        }
        context.waitTicks(20);
        String active = RecipeViewerHooks.active;
        int recipes = RecipeViewerHooks.recipeCount;
        if (!"rei".equals(active) || recipes <= 0 || RecipeViewerHooks.showCategory == null
                || RecipeViewerHooks.hoveredStack == null || RecipeViewerHooks.typing == null) {
            throw new AssertionError("REI: aktiv=" + active + ", Rezepte=" + recipes);
        }
        context.runOnClient(c -> RecipeViewerHooks.showCategory.accept("refining"));
        context.waitTicks(20);
        clearChat(context);
        context.takeScreenshot("rei_refining");
        boolean open = context.computeOnClient(c -> c.gui.screen() != null);
        context.runOnClient(c -> RecipeViewerHooks.showCategory.accept("assembly"));
        context.waitTicks(20);
        context.takeScreenshot("rei_assembly");
        context.setScreen(() -> null);
        context.waitTicks(5);
        if (!open) {
            throw new AssertionError("REI: Kategorie Raffinieren hat sich nicht geöffnet");
        }
    }

    /**
     * Forschungsbaum (11.3, 27.7 M4a): Stufe 1 offen, 12,34 FP, {@code suit_t1} erforscht. Das Terminal per
     * Rechtsklick öffnen und fotografieren; ein Klick auf den verfügbaren Knoten {@code gas_storage} erforscht ihn auf
     * dem Server (FP danach 2,34).
     */
    private static void researchTree(ClientGameTestContext context, TestSingleplayerContext sp) {
        Identifier suit = InfiniteHorizon.id("suit_t1");
        Identifier gas = InfiniteHorizon.id("gas_storage");
        sp.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var data = de.infinitehorizon.agency.AgencyData.get(server);
            var agency = data.agencyOf(player.getUUID()).orElseThrow();
            agency.progress().openTier(1);
            agency.progress().research(suit);
            agency.progress().addFp(1234);
            data.setDirty();
            de.infinitehorizon.research.ResearchService.syncAgency(server, agency);
        });
        BlockPos pos = context.computeOnClient(c -> BlockPos.containing(c.player.position()).east(2).above());
        setBlock(sp, pos, ModBlocks.RESEARCH_TERMINAL.get());
        context.waitFor(c -> c.level.getBlockState(pos).is(ModBlocks.RESEARCH_TERMINAL.get()));
        context.waitTicks(5);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(de.infinitehorizon.client.ResearchTerminalScreen.class);
        context.waitTicks(10);
        int nodes = context.computeOnClient(c -> de.infinitehorizon.data.ResearchRegistry.client().nodes().size());
        if (nodes == 0) {
            throw new AssertionError("Forschungsbaum: keine Knoten auf dem Client (SyncDataPayload)");
        }
        clearChat(context);
        context.takeScreenshot("gui_research_terminal");
        boolean sent = context.computeOnClient(c -> c.gui.screen() instanceof de.infinitehorizon.client.ResearchTerminalScreen r
                && r.clickNode(gas));
        if (!sent) {
            throw new AssertionError("Forschungsbaum: gas_storage nicht verfügbar");
        }
        context.waitTicks(10);
        boolean researched = sp.getServer().computeOnServer(server -> de.infinitehorizon.agency.AgencyData.get(server)
                .agencyOf(server.getPlayerList().getPlayers().getFirst().getUUID()).orElseThrow().progress().isResearched(gas));
        int fp = context.computeOnClient(c -> de.infinitehorizon.client.AgencyClientState.fp());
        if (!researched || fp != 234) {
            throw new AssertionError("Forschungsbaum: gas_storage erforscht=" + researched + ", FP auf dem Client " + fp);
        }
        context.takeScreenshot("gui_research_terminal_after");
        closeAndRemove(context, sp, pos);
    }

    /** Knopf „Tank“ im Elektrolyseur: wählt Tank 2 der Ausgänge (Sauerstoff) auf dem Server (4.1). */
    private static void tankButtonChangesServer(ClientGameTestContext context, TestSingleplayerContext sp) {
        BlockPos pos = openMachine(context, sp, ModBlocks.ELECTROLYZER.get(), be -> {
            List<IhFluidTank> tanks = be.fluidTanks();
            tanks.get(0).setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 8000));
            tanks.get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("hydrogen"), 6000));
            tanks.get(2).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("oxygen"), 3000));
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
        sp.getServer().runCommand("fill %d %d %d %d %d %d infinite_horizon:refinery_casing".formatted(
                pos.getX(), pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 1));
        sp.getServer().runCommand("setblock %d %d %d infinite_horizon:refinery_controller[facing=west]".formatted(
                pos.getX(), pos.getY(), pos.getZ()));
        context.waitFor(c -> c.level.getBlockState(pos).is(ModBlocks.REFINERY_CONTROLLER.get()));
        sp.getServer().runOnServer(server -> {
            if (sp.getConnection().getServerLevel().getBlockEntity(pos) instanceof MachineBlockEntity be) {
                be.refreshLayout();
                be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("crude_oil"), 8000));
                be.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("kerosene"), 3000));
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
