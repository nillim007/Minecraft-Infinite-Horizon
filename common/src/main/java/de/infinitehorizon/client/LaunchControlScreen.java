package de.infinitehorizon.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.network.LaunchRequestPayload;
import de.infinitehorizon.network.LaunchStatePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.space.LaunchControlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Raketen-Computer (12.4) im {@code launch_control}: links die Checkliste mit Haken oder Kreuz und erstem Fehler,
 * rechts Teile, Masse, Schub-Gewicht, Treibstoff und Ziele (grün/gelb/grau, 12.2); bei einer Sonde die Sondenziele mit
 * Start-Knöpfen (12.3). Daten vom Server ({@link LaunchStatePayload}); der Client rechnet nichts. Aus Rechtecken
 * gezeichnet wie das Maschinen-GUI (24.5).
 */
public class LaunchControlScreen extends AbstractContainerScreen<LaunchControlMenu> {

    private static final int W = 320;
    private static final int H = 196;
    private static final int PAD = 8;
    private static final int LINE = 11;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;
    private static final int OK = 0xFF208020;
    private static final int BAD = 0xFFB02020;
    private static final int GREEN = 0xFF208020;
    private static final int YELLOW = 0xFFA08000;
    private static final int GRAY = 0xFF808080;

    private static volatile @Nullable LaunchStatePayload latest;
    private @Nullable Object builtFor;
    private final List<AbstractWidget> probeButtons = new ArrayList<>();

    public LaunchControlScreen(LaunchControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    /** Payload-Empfang (Client-Thread). */
    public static void receive(LaunchStatePayload payload) {
        latest = payload;
    }

    private @Nullable JsonObject state() {
        LaunchStatePayload p = latest;
        if (p == null || !p.pos().equals(menu.pos())) {
            return null;
        }
        try {
            return JsonParser.parseString(p.json()).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected void init() {
        super.init();
        builtFor = null;
        rebuildButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (builtFor != latest) {
            rebuildButtons();
        }
    }

    private void rebuildButtons() {
        probeButtons.forEach(this::removeWidget);
        probeButtons.clear();
        builtFor = latest;
        JsonObject s = state();
        if (s == null || !"probe".equals(str(s, "load")) || s.get("countdown").getAsInt() > 0) {
            return;
        }
        int x = leftPos + W / 2 + PAD;
        int y = topPos + PAD + 2 * LINE;
        for (JsonElement e : s.getAsJsonArray("probe_targets")) {
            JsonObject t = e.getAsJsonObject();
            Identifier dim = Identifier.parse(t.get("dimension").getAsString());
            Component label = Component.translatable("gui.infinite_horizon.launch.probe_start", planet(dim));
            Button b = Button.builder(label, btn -> Services.NETWORK.sendToServer(LaunchRequestPayload.probe(menu.pos(), dim)))
                    .bounds(x, y, W / 2 - 2 * PAD, LINE + 3).build();
            b.active = allOk(s);
            probeButtons.add(addRenderableWidget(b));
            y += LINE + 6;
        }
    }

    /** Für Tests: offene Zeilen der Checkliste. */
    public List<String> failedChecks() {
        List<String> out = new ArrayList<>();
        JsonObject s = state();
        if (s != null) {
            for (JsonElement e : s.getAsJsonArray("lines")) {
                if (!e.getAsJsonObject().get("ok").getAsBoolean()) {
                    out.add(e.getAsJsonObject().get("key").getAsString());
                }
            }
        }
        return out;
    }

    private static boolean allOk(JsonObject s) {
        for (JsonElement e : s.getAsJsonArray("lines")) {
            if (!e.getAsJsonObject().get("ok").getAsBoolean()) {
                return false;
            }
        }
        return true;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : "";
    }

    private static Component planet(Identifier dim) {
        return Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + W, y + H, PANEL);
        g.text(font, title, x + PAD, y + PAD, TEXT, false);
        JsonObject s = state();
        if (s == null) {
            g.text(font, Component.translatable("gui.infinite_horizon.launch.waiting"), x + PAD, y + PAD + 2 * LINE, TEXT, false);
            return;
        }
        int countdown = s.get("countdown").getAsInt();
        if (countdown > 0) {
            Component c = Component.translatable("gui.infinite_horizon.launch.countdown", (countdown + 19) / 20);
            g.text(font, c, x + W - PAD - font.width(c), y + PAD, BAD, false);
        }
        // Checkliste
        int ly = y + PAD + 2 * LINE;
        g.text(font, Component.translatable("gui.infinite_horizon.launch.checklist"), x + PAD, ly - LINE - 2, TEXT, false);
        for (JsonElement e : s.getAsJsonArray("lines")) {
            JsonObject l = e.getAsJsonObject();
            boolean ok = l.get("ok").getAsBoolean();
            g.text(font, ok ? "✔" : "✘", x + PAD, ly, ok ? OK : BAD, false);
            g.text(font, Component.translatable("gui.infinite_horizon.launch.check." + str(l, "key")), x + PAD + 10, ly, TEXT, false);
            ly += LINE;
            if (!ok && l.has("error")) {
                MutableComponent err = Component.translatable("gui.infinite_horizon.launch.error." + str(l, "error"),
                        l.get("a").getAsInt(), l.get("b").getAsInt());
                g.text(font, font.substrByWidth(err, W / 2 - PAD - 14).getString(), x + PAD + 14, ly, BAD, false);
                ly += LINE;
            }
        }
        // Raketen-Computer
        int rx = x + W / 2 + PAD;
        int ry = y + PAD + LINE;
        if (s.has("rocket")) {
            JsonObject r = s.getAsJsonObject("rocket");
            g.text(font, Component.translatable("gui.infinite_horizon.launch.rocket", r.get("tier").getAsInt()), rx, ry, TEXT, false);
            ry += LINE;
            g.text(font, Component.translatable("gui.infinite_horizon.launch.mass",
                    String.format(Locale.ROOT, "%.2f", r.get("mass").getAsDouble()), r.get("thrust").getAsInt(),
                    String.format(Locale.ROOT, "%.2f", r.get("ratio").getAsDouble())), rx, ry, TEXT, false);
            ry += LINE;
            long cap = r.get("capacity").getAsLong();
            for (JsonElement f : r.getAsJsonArray("fuel")) {
                JsonObject fo = f.getAsJsonObject();
                Identifier fluid = Identifier.parse(fo.get("fluid").getAsString());
                g.text(font, Component.translatable("gui.infinite_horizon.launch.fuel",
                        Component.translatable("block." + fluid.getNamespace() + "." + fluid.getPath()),
                        fo.get("amount").getAsLong(), cap), rx, ry, TEXT, false);
                ry += LINE;
            }
            ry += 3;
            g.text(font, Component.translatable("gui.infinite_horizon.launch.destinations"), rx, ry, TEXT, false);
            ry += LINE;
            for (JsonElement d : s.getAsJsonArray("destinations")) {
                JsonObject o = d.getAsJsonObject();
                boolean unlocked = o.get("unlocked").getAsBoolean();
                String reach = str(o, "reach");
                int color = !unlocked ? GRAY : "both".equals(reach) ? GREEN : "one_way".equals(reach) ? YELLOW : GRAY;
                MutableComponent line = Component.translatable("gui.infinite_horizon.launch.destination",
                        planet(Identifier.parse(o.get("dimension").getAsString())), o.get("out").getAsLong(), o.get("back").getAsLong());
                if (!unlocked) {
                    line.append(Component.translatable("gui.infinite_horizon.launch.locked").withStyle(ChatFormatting.ITALIC));
                }
                g.text(font, font.substrByWidth(line, W / 2 - 2 * PAD).getString(), rx, ry, color, false);
                ry += LINE;
            }
        } else if ("probe".equals(str(s, "load"))) {
            g.text(font, Component.translatable("gui.infinite_horizon.launch.probe"), rx, ry, TEXT, false);
            if (s.getAsJsonArray("probe_targets").isEmpty()) {
                g.text(font, Component.translatable("gui.infinite_horizon.launch.no_probe_targets"), rx, ry + LINE, GRAY, false);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // eigener Kopf
    }
}
