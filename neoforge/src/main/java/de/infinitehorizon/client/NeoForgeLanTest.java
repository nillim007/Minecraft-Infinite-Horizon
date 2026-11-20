package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * LAN-Test mit zwei echten NeoForge-Clients ({@code -Dinfinite_horizon.lantest=host|guest}, Runs {@code lanHost} und
 * {@code lanGuest}). Der Host legt eine Welt an, öffnet sie im LAN auf {@code infinite_horizon.lantest.port} und schreibt
 * {@code lan-ready.txt}; der Gast startet danach mit {@code --quickPlayMultiplayer} und tritt bei. Geprüft wird, dass der
 * Gast ohne Trennung in die Welt kommt, Mod-Inhalte und Config ankommen und der Host ihn als Spieler sieht. Anlass:
 * Ein Gast wurde abgewiesen, weil der Host Registry-Einträge eines nur dort installierten Mods schickte (JEI).
 * Ergebnis je Client: {@code <spielordner>/infinite_horizon-lantest.txt} (PASS/FAIL, DONE).
 */
public final class NeoForgeLanTest {

    private static final int TIMEOUT_TICKS = 12000;
    /** Wie lange der Gast verbunden bleiben muss (Ticks), damit eine spätere Trennung auffällt. */
    private static final int STAY_TICKS = 200;

    private static final List<String> RESULTS = new ArrayList<>();
    private static int tick;
    private static int phase;
    private static int phaseTick;
    private static boolean finished;

    private NeoForgeLanTest() {
    }

    public static String role() {
        return System.getProperty("infinite_horizon.lantest", "");
    }

    public static boolean enabled() {
        return !role().isEmpty();
    }

    private static int port() {
        return Integer.getInteger("infinite_horizon.lantest.port", 25599);
    }

    /** Vom ClientTickEvent aufgerufen. */
    public static void tick() {
        if (finished) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (++tick > TIMEOUT_TICKS) {
            fail("timeout", "Phase " + phase + " hängt");
            finish(mc);
            return;
        }
        try {
            boolean next = "host".equals(role()) ? host(mc, phaseTick++) : guest(mc, phaseTick++);
            if (next) {
                phase++;
                phaseTick = 0;
            }
        } catch (Throwable t) {
            fail("phase_" + phase, t.toString());
            finish(mc);
        }
    }

    // ------------------------------------------------------------------ Host

    private static boolean host(Minecraft mc, int t) throws IOException {
        switch (phase) {
            case 0 -> {
                return mc.gui.screen() instanceof TitleScreen && t > 20;
            }
            case 1 -> {
                CreateWorldScreen.openFresh(mc, () -> { });
                if (!(mc.gui.screen() instanceof CreateWorldScreen cws)) {
                    throw new IllegalStateException("CreateWorldScreen nicht geöffnet");
                }
                WorldCreationUiState ui = cws.getUiState();
                ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(
                        ui.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
                ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                ui.setSeed("1");
                if (!NeoForgeClientTest.pressButton(mc.gui.screen(), "selectWorld.create")) {
                    throw new IllegalStateException("Knopf „Welt erstellen“ nicht gefunden");
                }
                return true;
            }
            case 2 -> {
                return mc.player != null && mc.level != null && mc.getSingleplayerServer() != null && t > 100;
            }
            case 3 -> {
                IntegratedServer server = mc.getSingleplayerServer();
                // Die Test-Clients sind nicht bei Microsoft angemeldet; ohne das scheitert der Gast an „Invalid session“,
                // bevor überhaupt Registries abgeglichen werden. Echte Spieler sind angemeldet (nur im Test).
                server.setUsesAuthentication(false);
                boolean published = server.publishServer(MinecraftServer.MultiplayerScope.LAN, false, port());
                check("lan_publish", published && server.isPublished(), "Port " + port());
                Files.writeString(mc.gameDirectory.toPath().resolve("lan-ready.txt"), Integer.toString(port()), StandardCharsets.UTF_8);
                return true;
            }
            case 4 -> {
                // Gast abwarten (er startet erst nach lan-ready.txt)
                return mc.getSingleplayerServer().getPlayerCount() >= 2;
            }
            case 5 -> {
                MinecraftServer server = mc.getSingleplayerServer();
                check("lan_guest_joined", List.of(server.getPlayerNames()).contains("Gast"), String.join(", ", server.getPlayerNames()));
                return true;
            }
            case 6 -> {
                // Gast bleibt eine Weile und meldet sich dann ab; danach ist der Host fertig
                if (mc.getSingleplayerServer().getPlayerCount() <= 1) {
                    pass("lan_guest_left");
                    finish(mc);
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ Gast

    private static boolean guest(Minecraft mc, int t) {
        if (mc.gui.screen() instanceof DisconnectedScreen screen) {
            fail("lan_join", "getrennt: " + screen.getNarrationMessage().getString());
            finish(mc);
            return false;
        }
        switch (phase) {
            case 0 -> {
                // quickPlay verbindet von selbst; warten, bis die Welt da ist
                return mc.player != null && mc.level != null && mc.getConnection() != null && mc.getSingleplayerServer() == null;
            }
            case 1 -> {
                if (t < STAY_TICKS) {
                    return false;
                }
                pass("lan_join");
                // Mod-Inhalte und Config vom Host angekommen
                check("lan_registry", BuiltInRegistries.BLOCK.getKey(ModBlocks.CRUSHER.get()).getNamespace().equals(InfiniteHorizon.MOD_ID),
                        "Zerkleinerer fehlt");
                String freq = ServerConfig.client().getString("event_frequency");
                check("lan_config_sync", "normal".equals(freq), "event_frequency = " + freq);
                return true;
            }
            case 2 -> {
                mc.disconnectFromWorld(net.minecraft.network.chat.Component.literal("LAN-Test fertig"));
                return true;
            }
            case 3 -> {
                if (t > 40) {
                    finish(mc);
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ Ergebnis

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass(name);
        } else {
            fail(name, detail);
        }
    }

    private static void pass(String name) {
        RESULTS.add("PASS " + name);
    }

    private static void fail(String name, String detail) {
        RESULTS.add("FAIL " + name + " | " + detail);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        RESULTS.add("DONE");
        try {
            Path out = mc.gameDirectory.toPath().resolve("infinite_horizon-lantest.txt");
            Files.write(out, RESULTS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            InfiniteHorizon.LOG.error("[LAN-Test] Ergebnis nicht schreibbar", e);
        }
        InfiniteHorizon.LOG.info("[LAN-Test] {} fertig: {}", role(), RESULTS);
        mc.stop();
    }
}
