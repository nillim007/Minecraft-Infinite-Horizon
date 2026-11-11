package de.sternenwerft.client;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.data.ServerConfig;
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
    }

    private static void half(MachineBlockEntity be) {
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity() / 2);
    }

    /**
     * Setzt {@code block} neben den Spieler, füllt ihn auf dem Server, öffnet das GUI per Rechtsklick (über das Netzwerk),
     * fotografiert es und führt optional {@code extra} aus; danach wird der Block entfernt.
     */
    private static void openMachine(Block block, String name, Consumer<MachineBlockEntity> setup, Step extra) {
        STEPS.add((mc, t) -> {
            BlockPos pos = mc.player.blockPosition().east(2);
            POS.set(pos);
            onServer(mc, level -> {
                level.setBlockAndUpdate(pos, block.defaultBlockState().hasProperty(MachineBlock.FACING)
                        ? block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH) : block.defaultBlockState());
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
            onServer(mc, level -> {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                return null;
            });
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
