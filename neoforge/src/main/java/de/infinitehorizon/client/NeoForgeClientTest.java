package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.client.handbook.HandbookClient;
import de.infinitehorizon.compat.RecipeViewerHooks;
import de.infinitehorizon.client.handbook.HandbookScreen;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.multiblock.GhostPreview;
import de.infinitehorizon.multiblock.MultiblockController;
import de.infinitehorizon.multiblock.MultiblockPattern;
import de.infinitehorizon.machine.FluidPumpBlockEntity;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Automatischer Client-Test für NeoForge (nur mit {@code -Dinfinite_horizon.clienttest=true}, Run {@code runClientTest}).
 * Legt eine Flachland-Welt an, öffnet jedes Maschinen-GUI per echtem Rechtsklick, macht Screenshots, klickt den Knopf
 * „Seite“ und prüft den Config-Sync. Ergebnis: {@code <spielordner>/infinite_horizon-clienttest.txt} (PASS/FAIL, DONE),
 * Bilder unter {@code screenshots/}. Fabric nutzt dafür die Client-GameTest-API (IhClientGameTests).
 */
public final class NeoForgeClientTest {

    /** Sicherheitsgrenze, falls ein Schritt hängt (Ticks). */
    private static final int TIMEOUT_TICKS = 6000;

    private static final List<String> RESULTS = new ArrayList<>();
    private static final List<Step> STEPS = new ArrayList<>();
    private static int tick;
    private static int stepIndex;
    private static int stepTick;
    private static boolean finished;

    /** Ein Schritt läuft jeden Tick, bis er true liefert. */
    private interface Step {
        boolean run(Minecraft mc, int ticksInStep);
    }

    private NeoForgeClientTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("infinite_horizon.clienttest");
    }

    /** Vom ClientTickEvent aufgerufen. */
    public static void tick() {
        if (finished) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (STEPS.isEmpty()) {
            plan();
        }
        tick++;
        if (tick > TIMEOUT_TICKS) {
            fail("timeout", "Schritt " + stepIndex + " hängt");
            finish(mc);
            return;
        }
        if (stepIndex >= STEPS.size()) {
            finish(mc);
            return;
        }
        boolean done;
        try {
            done = STEPS.get(stepIndex).run(mc, stepTick++);
        } catch (Throwable t) {
            fail("schritt_" + stepIndex, t.toString());
            done = true;
        }
        if (done) {
            stepIndex++;
            stepTick = 0;
        }
    }

    // ------------------------------------------------------------------ Ablauf

    private static final AtomicReference<BlockPos> POS = new AtomicReference<>();

    private static void plan() {
        // 1. Titelbildschirm abwarten, Flachland-Welt im Kreativmodus anlegen
        STEPS.add((mc, t) -> mc.gui.screen() instanceof TitleScreen && t > 20);
        STEPS.add((mc, t) -> {
            CreateWorldScreen.openFresh(mc, () -> { });
            if (!(mc.gui.screen() instanceof CreateWorldScreen cws)) {
                throw new IllegalStateException("CreateWorldScreen nicht geöffnet");
            }
            WorldCreationUiState ui = cws.getUiState();
            ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(
                    ui.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            ui.setSeed("1");
            if (!pressButton(mc.gui.screen(), "selectWorld.create")) {
                throw new IllegalStateException("Knopf „Welt erstellen“ nicht gefunden");
            }
            return true;
        });
        // 2. Warten, bis Spieler und integrierter Server bereit sind
        STEPS.add((mc, t) -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null && t > 100);
        // 3. Config-Sync
        STEPS.add((mc, t) -> {
            String freq = ServerConfig.client().getString("event_frequency");
            check("config_sync", "normal".equals(freq), "event_frequency = " + freq);
            return true;
        });
        // 4. Knopf „Seite“ im Tank-GUI → Server-Ausgangsseite Nord → Süd
        openMachine(ModBlocks.FLUID_TANK.get(), "neoforge_gui_fluid_tank", be -> { }, (mc, t) -> {
            if (t == 0 && mc.gui.screen() instanceof MachineScreen screen) {
                screen.cycleOutputSide();
            }
            if (t < 10) {
                return false;
            }
            String side = onServer(mc, level -> level.getBlockEntity(POS.get()) instanceof MachineBlockEntity be
                    ? be.outputSide().getSerializedName() : "?");
            check("side_button", "south".equals(side), "Ausgangsseite " + side + ", erwartet south");
            return true;
        });
        // 5. Alle Maschinen-GUIs fotografieren
        openMachine(ModBlocks.CRUSHER.get(), "neoforge_gui_crusher", be -> {
            be.setItem(0, new ItemStack(Items.RAW_IRON, 8));
            half(be);
        }, null);
        openMachine(ModBlocks.ELECTRIC_FURNACE.get(), "neoforge_gui_electric_furnace", be -> {
            be.setItem(0, new ItemStack(ModItems.simple("titanium_dust").get(), 4));
            be.setItem(be.layout().upgradeStart(), new ItemStack(ModItems.simple("speed_upgrade").get(), 2));
            half(be);
        }, null);
        openMachine(ModBlocks.PRESS.get(), "neoforge_gui_press", be -> {
            be.setItem(0, new ItemStack(ModItems.simple("titanium_ingot").get(), 16));
            be.setItem(be.layout().moldStart(), new ItemStack(ModItems.simple("press_mold_plate").get()));
            half(be);
        }, null);
        openMachine(ModBlocks.COMBUSTION_GENERATOR.get(), "neoforge_gui_combustion_generator",
                be -> be.setItem(0, new ItemStack(Items.COAL, 12)), null);
        openMachine(ModBlocks.FLUID_TANK.get(), "neoforge_gui_fluid_tank_oil", be ->
                ((FluidTankBlockEntity) be).tank().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("crude_oil"), 9000)), null);
        openMachine(ModBlocks.FLUID_PUMP.get(), "neoforge_gui_fluid_pump", be -> {
            ((FluidPumpBlockEntity) be).tank().setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 4000));
            half(be);
        }, null);
        // 6. M2d: Montagetisch, Elektrolyseur mit Knopf „Tank“, Raffinerie mit Aufbau und Geisterprojektion
        openMachine(ModBlocks.ASSEMBLY_TABLE.get(), "neoforge_gui_assembly_table", be -> {
            be.setItem(0, new ItemStack(ModItems.simple("silicon").get(), 2));
            be.setItem(1, new ItemStack(ModItems.simple("copper_wire").get(), 4));
            be.setItem(2, new ItemStack(ModItems.simple("plastic").get(), 1));
            half(be);
        }, null);
        openMachine(ModBlocks.ELECTROLYZER.get(), "neoforge_gui_electrolyzer", be -> {
            be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(Identifier.withDefaultNamespace("water"), 8000));
            be.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("hydrogen"), 6000));
            be.fluidTanks().get(2).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("oxygen"), 3000));
            half(be);
        }, (mc, t) -> {
            if (t == 0 && mc.gui.screen() instanceof MachineScreen screen) {
                screen.cycleTank();
            }
            if (t < 10) {
                return false;
            }
            int selected = onServer(mc, level -> level.getBlockEntity(POS.get()) instanceof MachineBlockEntity be ? be.selectedTank() : -1);
            check("tank_button", selected == 1, "Tankwahl " + selected + ", erwartet 1");
            Screenshot.grab(mc.gameDirectory, "neoforge_gui_electrolyzer_tank2.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            return true;
        });
        openMachine(ModBlocks.REFINERY_CONTROLLER.get(), "neoforge_gui_refinery_controller", Direction.WEST,
                NeoForgeClientTest::refineryCasings, be -> {
                    be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("crude_oil"), 8000));
                    be.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("kerosene"), 3000));
                    half(be);
                }, (mc, t) -> {
                    check("refinery_complete", onServer(mc, level -> level.getBlockEntity(POS.get())
                            instanceof MultiblockController c && c.structureComplete()), "Aufbau unvollständig");
                    return true;
                }, false);
        ghostPreview();
        // 7. M2e: Treibstoffgenerator und Akku
        openMachine(ModBlocks.FUEL_GENERATOR.get(), "neoforge_gui_fuel_generator", be ->
                be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("kerosene"), 9000)), null);
        openMachine(ModBlocks.BATTERY.get(), "neoforge_gui_battery", be -> {
            half(be);
            be.setItem(0, new ItemStack(ModItems.BATTERY.get()));
        }, null);
        laterMilestones();
        handbook();
        planetPage();
        recipeViewer();
    }

    // ------------------------------------------------------------------ M3–M9

    /** GUIs ab M3 (Fabric prüft sie im Client-Test ebenso): Maschinen, Terminals mit eigenem Bildschirm, echte Klicks. */
    private static void laterMilestones() {
        Block[] machines = {ModBlocks.OXYGEN_GENERATOR.get(), ModBlocks.ANALYZER.get(), ModBlocks.HIGH_TEMPERATURE_FURNACE.get(),
                ModBlocks.CRYO_LIQUEFIER.get(), ModBlocks.HYDROPONICS_BASIN.get(), ModBlocks.ZERO_G_FABRICATOR.get(),
                ModBlocks.FUSION_REACTOR.get(), ModBlocks.FISSION_REACTOR.get(), ModBlocks.DRILLING_RIG.get(),
                ModBlocks.PUMP_STATION.get()};
        for (Block b : machines) {
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
            openMachine(b, "neoforge_gui_" + id, NeoForgeClientTest::half, null);
        }
        // Forschungsterminal: Reiter mit echten Mausklicks (26.3: linke Taste = 1)
        openBlock(ModBlocks.RESEARCH_TERMINAL.get(), "neoforge_gui_research_terminal", be -> { },
                de.infinitehorizon.client.ResearchTerminalScreen.class, (mc, t) -> {
                    var screen = (de.infinitehorizon.client.ResearchTerminalScreen) mc.gui.screen();
                    if (t == 0) {
                        int[] c = screen.tabCenter(de.infinitehorizon.client.ResearchTerminalScreen.TAB_AGENCY);
                        clickAt(screen, c[0], c[1]);
                    } else if (t == 10) {
                        check("research_tab_agency", screen.tab() == de.infinitehorizon.client.ResearchTerminalScreen.TAB_AGENCY,
                                "Reiter " + screen.tab());
                        grab(mc, "neoforge_gui_research_agency");
                        int[] c = screen.tabCenter(de.infinitehorizon.client.ResearchTerminalScreen.TAB_PARTNERS);
                        clickAt(screen, c[0], c[1]);
                    } else if (t == 20) {
                        check("research_tab_partners", screen.tab() == de.infinitehorizon.client.ResearchTerminalScreen.TAB_PARTNERS,
                                "Reiter " + screen.tab());
                        grab(mc, "neoforge_gui_research_partners");
                        return true;
                    }
                    return false;
                });
        // Frachtterminal: Freigabe für Partner per Knopf
        openBlock(ModBlocks.CARGO_TERMINAL.get(), "neoforge_gui_cargo_terminal", be -> {
            if (be instanceof MachineBlockEntity m) {
                m.refreshLayout();
            }
        }, de.infinitehorizon.client.CargoTerminalScreen.class, (mc, t) -> {
            if (t == 0) {
                clickLabel(mc.gui.screen(), Component.translatable("gui.infinite_horizon.cargo_terminal.partners.off"));
                return false;
            }
            if (t < 10) {
                return false;
            }
            boolean shared = onServer(mc, level -> level.getBlockEntity(POS.get())
                    instanceof de.infinitehorizon.logistics.CargoTerminalBlockEntity c && c.shared());
            check("cargo_terminal_partners", shared, "nach dem Klick nicht freigegeben");
            return true;
        });
        // Handelsposten: Tausch per Knopf
        openBlock(ModBlocks.TRADING_POST.get(), "neoforge_gui_trading_post", be -> {
            if (be instanceof de.infinitehorizon.logistics.TradingPostBlockEntity tp) {
                tp.setOffer(0, new ItemStack(Items.IRON_INGOT, 4));
                tp.setOffer(1, new ItemStack(Items.EMERALD, 1));
                tp.insertItem(new ItemStack(Items.EMERALD, 2), false);
                be.getLevel().players().forEach(pl -> pl.getInventory().add(new ItemStack(Items.IRON_INGOT, 4)));
            }
        }, de.infinitehorizon.client.TradingPostScreen.class, (mc, t) -> {
            if (t == 0) {
                clickLabel(mc.gui.screen(), Component.translatable("gui.infinite_horizon.trading_post.trade"));
                return false;
            }
            if (t < 10) {
                return false;
            }
            int emeralds = mc.player.getInventory().countItem(Items.EMERALD);
            check("trading_post_trade", emeralds == 1, "Smaragde " + emeralds);
            onServer(mc, level -> {
                level.players().forEach(pl -> pl.getInventory().clearContent());
                if (level.getBlockEntity(POS.get()) instanceof de.infinitehorizon.logistics.TradingPostBlockEntity tp) {
                    tp.clearContent();
                }
                return null;
            });
            return true;
        });
        // Logistik-Computer und Werft-Terminal öffnen eigene Bildschirme über das Netzwerk
        openBlock(ModBlocks.LOGISTICS_COMPUTER.get(), "neoforge_gui_logistics_computer", be -> { },
                de.infinitehorizon.client.LogisticsScreen.class, null);
        openBlock(ModBlocks.SHIPYARD_TERMINAL.get(), "neoforge_gui_shipyard_terminal", be -> { },
                de.infinitehorizon.client.ShipyardScreen.class, null);
    }

    /** Handbuch: Planetenseite in Blaupausen-Optik mit Atlas-Kasten (24.1, M9e). */
    private static void planetPage() {
        STEPS.add((mc, t) -> {
            HandbookClient.open("infinite_horizon:phase_2/mond");
            return true;
        });
        STEPS.add((mc, t) -> mc.gui.screen() instanceof HandbookScreen && t > 10);
        STEPS.add((mc, t) -> {
            HandbookScreen s = (HandbookScreen) mc.gui.screen();
            check("handbook_planet_page", "infinite_horizon:phase_2/mond".equals(s.currentPage()), "offen " + s.currentPage());
            grab(mc, "neoforge_gui_handbook_planet");
            mc.setScreenAndShow(null);
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
    }

    /** Linksklick als Maus-Ereignis an den Bildschirm (GUI-Koordinaten). */
    private static boolean clickAt(Screen screen, double x, double y) {
        var ev = new net.minecraft.client.input.MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        boolean hit = screen.mouseClicked(ev, false);
        screen.mouseReleased(ev);
        return hit;
    }

    /** Klickt auf den sichtbaren, aktiven Knopf, dessen Text {@code label} enthält. */
    private static boolean clickLabel(Screen screen, Component label) {
        String want = label.getString();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget w && w.visible && w.active && w.getMessage().getString().contains(want)) {
                return clickAt(screen, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
            }
        }
        fail("klick_" + want, "Knopf nicht gefunden");
        return false;
    }

    /**
     * Wie {@link #openMachine}, aber für Blöcke mit eigenem Bildschirm ({@code screen}): setzen, einrichten, per
     * Rechtsklick öffnen, fotografieren, {@code extra} ausführen, schließen und entfernen.
     */
    private static void openBlock(Block block, String name, Consumer<net.minecraft.world.level.block.entity.BlockEntity> setup,
                                  Class<? extends Screen> screen, Step extra) {
        STEPS.add((mc, t) -> {
            BlockPos pos = mc.player.blockPosition().east(2);
            POS.set(pos);
            onServer(mc, level -> {
                var state = block.defaultBlockState();
                if (state.hasProperty(MachineBlock.FACING)) {
                    state = state.setValue(MachineBlock.FACING, Direction.WEST);
                }
                level.setBlockAndUpdate(pos, state);
                var be = level.getBlockEntity(pos);
                if (be != null) {
                    setup.accept(be);
                }
                return null;
            });
            return true;
        });
        STEPS.add((mc, t) -> t > 10);
        STEPS.add((mc, t) -> {
            BlockPos pos = POS.get();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false));
            return true;
        });
        STEPS.add((mc, t) -> {
            if (screen.isInstance(mc.gui.screen())) {
                return t > 15;
            }
            if (t > 100) {
                fail(name + "_open", "Bildschirm hat sich nicht geöffnet (Screen: " + mc.gui.screen() + ")");
                return true;
            }
            return false;
        });
        STEPS.add((mc, t) -> {
            if (screen.isInstance(mc.gui.screen())) {
                grab(mc, name);
                pass(name + "_open");
            }
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
        if (extra != null) {
            STEPS.add((mc, t) -> !screen.isInstance(mc.gui.screen()) || extra.run(mc, t));
        }
        STEPS.add((mc, t) -> {
            if (mc.player != null) {
                mc.player.closeContainer();
            }
            mc.setScreenAndShow(null);
            onServer(mc, level -> {
                level.setBlockAndUpdate(POS.get(), Blocks.AIR.defaultBlockState());
                return null;
            });
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    /**
     * JEI (nur im Client-Test geladen, 3.9): Plugin aktiv, Rezepte angekommen, Kategorien Raffinieren und Montage öffnen
     * sich; Screenshots für die Sichtprüfung.
     */
    private static void recipeViewer() {
        if (!Boolean.getBoolean("infinite_horizon.clienttest.jei")) {
            return;
        }
        STEPS.add((mc, t) -> {
            boolean ok = "jei".equals(RecipeViewerHooks.active) && RecipeViewerHooks.showCategory != null;
            check("jei_plugin", ok, "aktiv=" + RecipeViewerHooks.active);
            check("jei_recipes", RecipeViewerHooks.recipeCount > 0, "Rezepte: " + RecipeViewerHooks.recipeCount);
            // Taste H über JEI-Items (24.1): Haken gesetzt, ohne Maus über einem Item kommt nichts, kein Suchfeld aktiv
            check("jei_h_hook", RecipeViewerHooks.hoveredStack != null && RecipeViewerHooks.typing != null
                    && !RecipeViewerHooks.isTyping(), "Haken fehlen oder Suchfeld aktiv");
            if (ok) {
                RecipeViewerHooks.showCategory.accept("refining");
            }
            return true;
        });
        STEPS.add((mc, t) -> t > 20);
        STEPS.add((mc, t) -> {
            Screenshot.grab(mc.gameDirectory, "neoforge_jei_refining.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            check("jei_category_open", mc.gui.screen() != null && RecipeViewerHooks.showCategory != null,
                    "Screen: " + mc.gui.screen());
            if (RecipeViewerHooks.showCategory != null) {
                RecipeViewerHooks.showCategory.accept("assembly");
            }
            return true;
        });
        STEPS.add((mc, t) -> t > 20);
        STEPS.add((mc, t) -> {
            Screenshot.grab(mc.gameDirectory, "neoforge_jei_assembly.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            mc.setScreenAndShow(null);
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    /** Handbuch (24.1, 27.3 M2f): öffnen, einem Link folgen, Seite eines Items finden; Screenshots. */
    private static void handbook() {
        STEPS.add((mc, t) -> {
            HandbookClient.open("infinite_horizon:start/willkommen");
            return true;
        });
        STEPS.add((mc, t) -> mc.gui.screen() instanceof HandbookScreen && t > 10);
        STEPS.add((mc, t) -> {
            Screenshot.grab(mc.gameDirectory, "neoforge_gui_handbook.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            HandbookScreen s = (HandbookScreen) mc.gui.screen();
            String link = s.firstLink();
            boolean followed = link != null && s.openPage(link) && link.equals(s.currentPage());
            check("handbook_link", followed, "Link " + link + ", offen " + s.currentPage());
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
        STEPS.add((mc, t) -> {
            Screenshot.grab(mc.gameDirectory, "neoforge_gui_handbook_link.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            String page = HandbookClient.pageFor(new ItemStack(ModBlocks.CRUSHER.get()));
            check("handbook_item_page", page != null, "keine Seite für den Zerkleinerer");
            check("handbook_gift", mc.player.getInventory().countItem(ModItems.ENGINEERS_HANDBOOK.get()) <= 1,
                    "mehr als ein Handbuch");
            mc.player.closeContainer();
            mc.setScreenAndShow(null);
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    /** Gehäuse der Raffinerie um den Controller (Vorderseite Westen) aus dem Muster der Daten. */
    private static void refineryCasings(ServerLevel level, BlockPos controller, boolean place) {
        MultiblockPattern p = MultiblockPattern.forMachine(BalanceManager.get(), "infinite_horizon:refinery_controller").orElseThrow();
        for (BlockPos pos : p.partPositions(controller, Direction.WEST)) {
            level.setBlockAndUpdate(pos, place ? ModBlocks.REFINERY_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * Ein Gehäuse fehlt; Sneak-Rechtsklick mit leerer Hand auf den Controller startet die Geisterprojektion und öffnet
     * kein GUI (4.1). Danach wird der Aufbau entfernt.
     */
    private static void ghostPreview() {
        STEPS.add((mc, t) -> {
            BlockPos pos = POS.get();
            onServer(mc, level -> {
                // Gehäuse in der Vorderseite (vom Spieler aus sichtbar)
                level.setBlockAndUpdate(pos.above(2).south(), Blocks.AIR.defaultBlockState());
                // leere Hand: aufgesammelte Maschineninhalte aus den vorigen Schritten entfernen
                level.players().forEach(pl -> pl.getInventory().clearContent());
                return null;
            });
            mc.options.keyShift.setDown(true);
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
        STEPS.add((mc, t) -> {
            BlockPos pos = POS.get();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false));
            return true;
        });
        STEPS.add((mc, t) -> t > 10);
        STEPS.add((mc, t) -> {
            BlockPos pos = POS.get();
            check("ghost_preview", GhostPreview.active(mc.level, pos) && mc.gui.screen() == null,
                    "aktiv=" + GhostPreview.active(mc.level, pos) + ", Screen=" + mc.gui.screen());
            mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos.above(2).south()));
            return true;
        });
        STEPS.add((mc, t) -> t > 10);
        STEPS.add((mc, t) -> {
            Screenshot.grab(mc.gameDirectory, "neoforge_world_refinery_ghost.png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            mc.options.keyShift.setDown(false);
            BlockPos pos = POS.get();
            onServer(mc, level -> {
                refineryCasings(level, pos, false);
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                return null;
            });
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    private static void half(MachineBlockEntity be) {
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity() / 2);
    }

    /**
     * Setzt {@code block} neben den Spieler, füllt ihn auf dem Server, öffnet das GUI per Rechtsklick (über das Netzwerk),
     * fotografiert es und führt optional {@code extra} aus; danach wird der Block entfernt.
     */
    private static void openMachine(Block block, String name, Consumer<MachineBlockEntity> setup, Step extra) {
        openMachine(block, name, Direction.NORTH, null, setup, extra, true);
    }

    /** Umgebung einer Maschine (z. B. Gehäuse eines Multiblocks): {@code place} = setzen, sonst entfernen. */
    private interface Around {
        void apply(ServerLevel level, BlockPos pos, boolean place);
    }

    private static void openMachine(Block block, String name, Direction facing, Around around,
                                    Consumer<MachineBlockEntity> setup, Step extra, boolean remove) {
        STEPS.add((mc, t) -> {
            BlockPos pos = mc.player.blockPosition().east(2);
            POS.set(pos);
            onServer(mc, level -> {
                if (around != null) {
                    around.apply(level, pos, true);
                }
                level.setBlockAndUpdate(pos, block.defaultBlockState().hasProperty(MachineBlock.FACING)
                        ? block.defaultBlockState().setValue(MachineBlock.FACING, facing) : block.defaultBlockState());
                if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
                    be.refreshLayout();
                    setup.accept(be);
                }
                return null;
            });
            return true;
        });
        STEPS.add((mc, t) -> t > 10);
        STEPS.add((mc, t) -> {
            BlockPos pos = POS.get();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false));
            return true;
        });
        STEPS.add((mc, t) -> {
            if (mc.gui.screen() instanceof MachineScreen) {
                return t > 15;
            }
            if (t > 100) {
                fail(name + "_open", "GUI hat sich nicht geöffnet (Screen: " + mc.gui.screen() + ")");
                return true;
            }
            return false;
        });
        STEPS.add((mc, t) -> {
            if (mc.gui.screen() instanceof MachineScreen) {
                Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
                pass(name + "_open");
            }
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
        if (extra != null) {
            STEPS.add(extra);
        }
        STEPS.add((mc, t) -> {
            if (mc.player != null) {
                mc.player.closeContainer();
            }
            BlockPos pos = POS.get();
            if (remove) {
                onServer(mc, level -> {
                    if (around != null) {
                        around.apply(level, pos, false);
                    }
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    return null;
                });
            }
            return true;
        });
        STEPS.add((mc, t) -> t > 5);
    }

    // ------------------------------------------------------------------ Hilfen

    /** Führt {@code fn} auf dem Server-Thread aus und wartet auf das Ergebnis. */
    private static <T> T onServer(Minecraft mc, java.util.function.Function<ServerLevel, T> fn) {
        MinecraftServer server = mc.getSingleplayerServer();
        ServerLevel level = server.overworld();
        return server.submit(() -> fn.apply(level)).join();
    }

    private static boolean pressButton(Screen screen, String translationKey) {
        String text = Component.translatable(translationKey).getString();
        MouseButtonInfo click = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button b && text.equals(b.getMessage().getString())) {
                b.onPress(click);
                return true;
            }
            if (child instanceof LayoutElement layout) {
                boolean[] found = {false};
                layout.visitWidgets((AbstractWidget w) -> {
                    if (!found[0] && w instanceof Button b && text.equals(b.getMessage().getString())) {
                        b.onPress(click);
                        found[0] = true;
                    }
                });
                if (found[0]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass(name);
        } else {
            fail(name, detail);
        }
    }

    private static void pass(String name) {
        RESULTS.add("PASS " + name);
        InfiniteHorizon.LOG.info("[Client-Test] PASS {}", name);
    }

    private static void fail(String name, String detail) {
        RESULTS.add("FAIL " + name + ": " + detail);
        InfiniteHorizon.LOG.error("[Client-Test] FAIL {}: {}", name, detail);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        RESULTS.add("DONE");
        try {
            Files.write(mc.gameDirectory.toPath().resolve("infinite_horizon-clienttest.txt"), RESULTS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            InfiniteHorizon.LOG.error("[Client-Test] Ergebnis nicht schreibbar", e);
        }
        InfiniteHorizon.LOG.info("[Client-Test] fertig: {}", RESULTS);
        mc.stop();
    }
}
