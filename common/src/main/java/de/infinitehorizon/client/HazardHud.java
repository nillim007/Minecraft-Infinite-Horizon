package de.infinitehorizon.client;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.network.HazardStatePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * HUD der Umwelt (8.4): Belastungsleisten je Gefahr (sichtbar ab {@code show_from}, grün/gelb/rot nach Belastung),
 * O₂-Vorrat des Anzugs (Farben umgekehrt) sowie Temperatur und Strahlung als Text, wenn sie außerhalb des Schutzes ohne
 * Anzug liegen. Leisten über der Hungerleiste, nach oben gestapelt, über den Luftblasen, wenn diese sichtbar sind.
 * Beim Erscheinen einer Leiste ertönt ihr Warnton (24.6). Werte aus environment.json ({@code hud}); Daten aus dem
 * {@link HazardStatePayload}.
 */
public final class HazardHud {

    private static volatile de.infinitehorizon.network.EventStatePayload event;
    private static volatile long eventReceived;

    /** Ereignisstand vom Server (19). */
    public static void receiveEvent(de.infinitehorizon.network.EventStatePayload payload) {
        event = payload;
        eventReceived = System.currentTimeMillis();
    }

    /** Ticks seit dem letzten Ereignisstand (für die Restzeit). */
    private static long eventAge() {
        return (System.currentTimeMillis() - eventReceived) / 50;
    }


    private static final int GREEN = 0xFF55DD55;
    private static final int YELLOW = 0xFFFFDD44;
    private static final int RED = 0xFFFF5544;
    private static final int BACK = 0xA0000000;
    private static final int TEXT = 0xFFFFFFFF;
    /** Abstand der Hungerleiste vom unteren Rand und Zeilenhöhe der Vanilla-Statusleisten. */
    private static final int FOOD_ROW_Y = 39;
    private static final int VANILLA_ROW = 10;

    private static @Nullable HazardStatePayload state;
    private static final boolean[] SHOWN = new boolean[HazardStatePayload.BAR_IDS.length];

    private HazardHud() {
    }

    private static JsonObject hud() {
        return BalanceManager.getOrBundled().entry("environment", "hud").orElse(new JsonObject());
    }

    private static double num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsDouble() : 0;
    }

    /** Neuer Zustand vom Server; spielt Warntöne für neu erscheinende Leisten. */
    public static void receive(HazardStatePayload payload) {
        JsonObject hud = hud();
        double from = num(hud, "warning_sound_from");
        JsonObject sounds = hud.has("warning_sounds") ? hud.getAsJsonObject("warning_sounds") : new JsonObject();
        for (int i = 0; i < SHOWN.length; i++) {
            boolean now = payload.percentOf(i) / 100.0 >= from && payload.percentOf(i) > 0;
            if (now && !SHOWN[i] && sounds.has(HazardStatePayload.BAR_IDS[i])) {
                Identifier id = Identifier.parse(sounds.get(HazardStatePayload.BAR_IDS[i]).getAsString());
                BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(e ->
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, 1.0F)));
            }
            SHOWN[i] = now;
        }
        state = payload;
    }

    /** Für Tests und Spielwechsel: Zustand vergessen. */
    public static void clear() {
        state = null;
        java.util.Arrays.fill(SHOWN, false);
    }

    public static @Nullable HazardStatePayload state() {
        return state;
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        HazardStatePayload s = state;
        if (s == null || mc.player == null) {
            return;
        }
        JsonObject hud = hud();
        int barW = (int) num(hud, "bar_width");
        int barH = (int) num(hud, "bar_height");
        double showFrom = num(hud, "show_from");
        double yellowFrom = num(hud, "yellow_from");
        double redFrom = num(hud, "red_from");
        Font font = mc.font;
        int right = g.guiWidth() / 2 + 91;
        int left = right - barW;
        boolean air = mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                || mc.player.getAirSupply() < mc.player.getMaxAirSupply();
        int y = g.guiHeight() - FOOD_ROW_Y - (air ? 2 * VANILLA_ROW : VANILLA_ROW);
        int row = barH + font.lineHeight + 1;

        for (int i = 0; i < HazardStatePayload.BAR_IDS.length; i++) {
            double f = s.percentOf(i) / 100.0;
            if (f < showFrom || s.percentOf(i) == 0) {
                continue;
            }
            int color = f >= redFrom ? RED : f >= yellowFrom ? YELLOW : GREEN;
            y -= row;
            bar(g, font, left, y, barW, barH, f, color,
                    Component.translatable("hud." + InfiniteHorizon.MOD_ID + "." + HazardStatePayload.BAR_IDS[i]));
        }
        if (s.suitOxygen() >= 0 && !s.breathable()) {
            int capacity = Suits.supply().capacity();
            double f = capacity <= 0 ? 0 : Math.min(1.0, s.suitOxygen() / (double) capacity);
            // Farben umgekehrt: gelb unter 1 − yellow_from, rot unter 1 − red_from
            int color = f < 1 - redFrom ? RED : f < 1 - yellowFrom ? YELLOW : GREEN;
            y -= row;
            bar(g, font, left, y, barW, barH, f, color,
                    Component.translatable("hud." + InfiniteHorizon.MOD_ID + ".suit_oxygen", s.suitOxygen(), capacity));
        }
        // Ereignis (19): HUD-Symbol als Zeile mit Restzeit
        de.infinitehorizon.network.EventStatePayload ev = event;
        if (ev != null && ev.phase() > 0 && !ev.event().isEmpty()) {
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(ev.event());
            y -= row;
            g.text(font, Component.translatable("hud." + InfiniteHorizon.MOD_ID + ".event." + (ev.phase() == 1 ? "warning" : "active"),
                    Component.translatable("event." + id.getNamespace() + "." + id.getPath()), Math.max(0, ev.ticks() - (eventAge())) / 20),
                    left, y, ev.phase() == 1 ? YELLOW : RED, true);
        }
        // Hitzezeit der Venus-Oberfläche (8.4, 9.1): Leiste wie der Anzugvorrat
        de.infinitehorizon.environment.VenusHeat heat = de.infinitehorizon.environment.VenusHeat.rules();
        if (s.venusHeatLeft() >= 0 && heat != null) {
            double f = heat.duration() <= 0 ? 0 : Math.min(1.0, s.venusHeatLeft() / (double) heat.duration());
            int color = f < 1 - redFrom ? RED : f < 1 - yellowFrom ? YELLOW : GREEN;
            y -= row;
            bar(g, font, left, y, barW, barH, f, color, Component.translatable("hud." + InfiniteHorizon.MOD_ID + ".venus_heat",
                    (s.venusHeatLeft() + 19) / 20));
        }
        JsonObject tol = BalanceManager.getOrBundled().entry("environment", "no_suit_tolerance").orElse(new JsonObject());
        StringBuilder info = new StringBuilder();
        if (s.temperature() < num(tol, "temp_min") || s.temperature() > num(tol, "temp_max")) {
            info.append(Component.translatable("hud." + InfiniteHorizon.MOD_ID + ".temperature_value", s.temperature()).getString());
        }
        if (s.radiation() > num(tol, "radiation_max")) {
            if (!info.isEmpty()) {
                info.append("  ");
            }
            info.append(Component.translatable("hud." + InfiniteHorizon.MOD_ID + ".radiation_value", s.radiation()).getString());
        }
        if (!info.isEmpty()) {
            y -= font.lineHeight + 1;
            g.text(font, info.toString(), right - font.width(info.toString()), y, TEXT, true);
        }
    }

    private static void bar(GuiGraphicsExtractor g, Font font, int left, int y, int w, int h, double f, int color,
                            Component label) {
        g.text(font, label.getString(), left, y, TEXT, true);
        int by = y + font.lineHeight;
        g.fill(left - 1, by - 1, left + w + 1, by + h + 1, BACK);
        g.fill(left, by, left + (int) Math.round(w * f), by + h, color);
    }
}
