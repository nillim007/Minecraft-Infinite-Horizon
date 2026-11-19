package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.HazardTracker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Gemeinsame Werkzeuge der Spieltests im echten Client: Welt anlegen, Schritte mit gesammelten Befunden, Befehle in der
 * Dimension des Spielers, Reisen, Bilder, echte Mausklicks auf Knöpfe eines GUI.
 */
public abstract class IhPlaytestBase implements FabricClientGameTest {

    protected ClientGameTestContext context;
    protected TestSingleplayerContext sp;
    protected final List<String> problems = new ArrayList<>();

    /** Name für {@code -PclientTestOnly=…}. */
    protected abstract String name();

    /** Die Schritte des Spieltests (je {@link #step}). */
    protected abstract void steps();

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!IhClientGameTests.only(name())) {
            return;
        }
        this.context = context;
        try (TestSingleplayerContext sp = context.worldBuilder()
                .adjustSettings(c -> {
                    c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    c.setGenerateStructures(true);
                }).create()) {
            this.sp = sp;
            sp.getConnection().waitForChunksRender();
            cmd("time set 6000");
            steps();
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("Spieltest " + name() + ": " + String.join(" | ", problems));
        }
    }

    /** Teleport in der Dimension des Spielers ({@code tp} von der Konsole ginge in die Overworld). */
    protected void tpHere(double x, double y, double z, float yaw, float pitch) {
        cmd(String.format(java.util.Locale.ROOT, "execute as @p at @s run tp @s %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    protected void step(String name, Runnable r) {
        // ein toter Spieler aus dem vorigen Schritt wird erst wiederbelebt
        if (context.computeOnClient(c -> c.player.isDeadOrDying())) {
            context.runOnClient(c -> c.player.respawn());
            context.waitTicks(20);
            context.setScreen(() -> null);
        }
        int before = problems.size();
        try {
            r.run();
        } catch (RuntimeException | AssertionError e) {
            problems.add(name + ": " + e.getMessage());
        }
        InfiniteHorizon.LOG.info("SPIELTEST {} {}", problems.size() == before ? "OK" : "FEHLER", name);
        try {
            context.setScreen(() -> null);
            cmd("gamemode creative @p");
            cmd("effect clear @p");
        } catch (RuntimeException ignored) {
            // nächster Schritt setzt neu auf
        }
    }

    protected void check(boolean ok, String what) {
        if (!ok) {
            problems.add(what);
            InfiniteHorizon.LOG.warn("SPIELTEST Befund: {}", what);
        }
    }

    protected void cmd(String c) {
        sp.getServer().runCommand(c);
    }

    protected static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().getFirst();
    }

    protected <T> T server(Function<ServerPlayer, T> f) {
        return sp.getServer().computeOnServer(s -> f.apply(player(s)));
    }

    protected void go(String planet) {
        go(planet, null);
    }

    protected void go(String planet, int[] xz) {
        String id = planet.contains(":") ? planet : "infinite_horizon:" + planet;
        cmd("execute as @p run infinite_horizon goto " + id + (xz == null ? "" : " " + xz[0] + " " + xz[1]));
        context.waitTicks(40);
        sp.getConnection().waitForChunksRender();
        String dim = context.computeOnClient(c -> c.level.dimension().identifier().toString());
        if (!id.equals(dim)) {
            throw new AssertionError("goto " + planet + " landet in " + dim);
        }
        cmd("effect give @p minecraft:night_vision infinite 0 true");
    }

    protected void equip(String suit, String... modules) {
        sp.getServer().runOnServer(s -> IhGameTestsM7f.equip(player(s), suit, List.of(modules)));
    }

    /** Gefahren am echten Ort des Spielers (Server, 8.2). */
    protected String hazards() {
        return server(p -> HazardTracker.evaluate(p, HazardTracker.rules()).hazards().toString());
    }

    /** Höchste Belastung, die das HUD gerade zeigt (0–100). */
    protected int hudMax() {
        return context.computeOnClient(c -> {
            var st = de.infinitehorizon.client.HazardHud.state();
            int max = 0;
            for (int i = 0; st != null && i < st.percent().length; i++) {
                max = Math.max(max, st.percentOf(i));
            }
            return max;
        });
    }

    protected void shot(String name) {
        context.runOnClient(c -> c.gui.hud.getChat().clearMessages(false));
        context.waitTick();
        context.takeScreenshot(name);
    }

    protected double clientY() {
        return context.computeOnClient(c -> c.player.getY());
    }

    /** Linksklick als Maus-Ereignis an den offenen Bildschirm an GUI-Koordinaten (Reiter, Slots, Textfelder). */
    protected boolean clickAt(double x, double y) {
        return context.computeOnClient(c -> {
            var screen = c.gui.screen();
            if (screen == null) {
                return false;
            }
            var ev = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(
                    com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            boolean hit = screen.mouseClicked(ev, false);
            screen.mouseReleased(ev);
            return hit;
        });
    }

    /**
     * Klickt mit der echten Maus auf den sichtbaren Knopf des offenen GUI, dessen Text {@code text} enthält
     * (Übersetzung im Client). Gibt zurück, ob es ihn gab.
     */
    protected boolean click(Component text) {
        String want = context.computeOnClient(c -> text.getString());
        return clickWhere(m -> m.contains(want));
    }

    /**
     * Wie {@link #click}, aber mit eigener Bedingung für den Knopftext (z. B. genau „1“). Der Klick geht als Maus-Ereignis
     * an den offenen Bildschirm (GUI-Koordinaten, Trefferprüfung wie bei einem echten Klick).
     */
    protected boolean clickWhere(java.util.function.Predicate<String> match) {
        boolean hit = context.computeOnClient(c -> {
            var screen = c.gui.screen();
            if (screen == null) {
                return false;
            }
            for (var e : screen.children()) {
                if (e instanceof AbstractWidget w && w.visible && w.active && match.test(w.getMessage().getString())) {
                    var ev = new net.minecraft.client.input.MouseButtonEvent(w.getX() + w.getWidth() / 2.0,
                            w.getY() + w.getHeight() / 2.0, new net.minecraft.client.input.MouseButtonInfo(
                            com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)); // 26.3: links = 1
                    boolean viaScreen = screen.mouseClicked(ev, false);
                    screen.mouseReleased(ev);
                    InfiniteHorizon.LOG.info("SPIELTEST Klick auf „{}“: {}", w.getMessage().getString(), viaScreen);
                    return true;
                }
            }
            return false;
        });
        context.waitTicks(3);
        return hit;
    }
}
