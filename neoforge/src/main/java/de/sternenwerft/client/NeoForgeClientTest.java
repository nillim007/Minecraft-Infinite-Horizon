package de.sternenwerft.client;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.client.handbook.HandbookClient;
import de.sternenwerft.compat.RecipeViewerHooks;
import de.sternenwerft.client.handbook.HandbookScreen;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.ServerConfig;
import de.sternenwerft.multiblock.GhostPreview;
import de.sternenwerft.multiblock.MultiblockController;
import de.sternenwerft.multiblock.MultiblockPattern;
import de.sternenwerft.machine.FluidPumpBlockEntity;
import de.sternenwerft.machine.FluidTankBlockEntity;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModItems;
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
 * Automatischer Client-Test für NeoForge (nur mit {@code -Dsternenwerft.clienttest=true}, Run {@code runClientTest}).
 * Legt eine Flachland-Welt an, öffnet jedes Maschinen-GUI per echtem Rechtsklick, macht Screenshots, klickt den Knopf
 * „Seite“ und prüft den Config-Sync. Ergebnis: {@code <spielordner>/sternenwerft-clienttest.txt} (PASS/FAIL, DONE),
 * Bilder unter {@code screenshots/}. Fabric nutzt dafür die Client-GameTest-API (SwClientGameTests).
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
        return Boolean.getBoolean("sternenwerft.clienttest");
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
                ((FluidTankBlockEntity) be).tank().setStackUnchecked(SwFluidStack.of(Sternenwerft.id("crude_oil"), 9000)), null);
        openMachine(ModBlocks.FLUID_PUMP.get(), "neoforge_gui_fluid_pump", be -> {
            ((FluidPumpBlockEntity) be).tank().setStackUnchecked(SwFluidStack.of(Identifier.withDefaultNamespace("water"), 4000));
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
            be.fluidTanks().get(0).setStackUnchecked(SwFluidStack.of(Identifier.withDefaultNamespace("water"), 8000));
            be.fluidTanks().get(1).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("hydrogen"), 6000));
            be.fluidTanks().get(2).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("oxygen"), 3000));
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
                    be.fluidTanks().get(0).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("crude_oil"), 8000));
                    be.fluidTanks().get(1).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("kerosene"), 3000));
                    half(be);
                }, (mc, t) -> {
                    check("refinery_complete", onServer(mc, level -> level.getBlockEntity(POS.get())
                            instanceof MultiblockController c && c.structureComplete()), "Aufbau unvollständig");
                    return true;
                }, false);
        ghostPreview();
        // 7. M2e: Treibstoffgenerator und Akku
        openMachine(ModBlocks.FUEL_GENERATOR.get(), "neoforge_gui_fuel_generator", be ->
                be.fluidTanks().get(0).setStackUnchecked(SwFluidStack.of(Sternenwerft.id("kerosene"), 9000)), null);
        openMachine(ModBlocks.BATTERY.get(), "neoforge_gui_battery", be -> {
            half(be);
            be.setItem(0, new ItemStack(ModItems.BATTERY.get()));
        }, null);
        handbook();
        recipeViewer();
    }

    /**
     * JEI (nur im Client-Test geladen, 3.9): Plugin aktiv, Rezepte angekommen, Kategorien Raffinieren und Montage öffnen
     * sich; Screenshots für die Sichtprüfung.
     */
    private static void recipeViewer() {
        if (!Boolean.getBoolean("sternenwerft.clienttest.jei")) {
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
            HandbookClient.open("sternenwerft:start/willkommen");
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
        MultiblockPattern p = MultiblockPattern.forMachine(BalanceManager.get(), "sternenwerft:refinery_controller").orElseThrow();
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
        Sternenwerft.LOG.info("[Client-Test] PASS {}", name);
    }

    private static void fail(String name, String detail) {
        RESULTS.add("FAIL " + name + ": " + detail);
        Sternenwerft.LOG.error("[Client-Test] FAIL {}: {}", name, detail);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        RESULTS.add("DONE");
        try {
            Files.write(mc.gameDirectory.toPath().resolve("sternenwerft-clienttest.txt"), RESULTS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Sternenwerft.LOG.error("[Client-Test] Ergebnis nicht schreibbar", e);
        }
        Sternenwerft.LOG.info("[Client-Test] fertig: {}", RESULTS);
        mc.stop();
    }
}
