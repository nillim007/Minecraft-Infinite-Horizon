package de.infinitehorizon.client;

import de.infinitehorizon.network.ShipyardStatePayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Werft-Terminal (21.1): 8 Bauabschnitte mit Fortschrittsbalken; der Tooltip eines Balkens nennt die fehlenden Mengen.
 * Fertige Abschnitte grün, der laufende Bau mit Restzeit. Aus Rechtecken gezeichnet.
 */
public class ShipyardScreen extends Screen {

    private static final int W = 370;
    private static final int ROW = 22;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;
    private static final int WARN = 0xFFB02020;
    private static final int BAR_BG = 0xFF373737;
    private static final int BAR = 0xFF4A90D0;
    private static final int DONE = 0xFF40B040;
    private static final int BUILDING = 0xFFE0A020;
    private static final int BAR_X = 150;
    private static final int BAR_W = 80;
    private static final int BAR_H = 8;
    private static final int REFRESH_TICKS = 40;
    private static final int TICKS_PER_SECOND = 20;

    private ShipyardStatePayload state;
    private int left;
    private int top;
    private int ticks;

    public ShipyardScreen(ShipyardStatePayload state) {
        super(Component.translatable("block.infinite_horizon.shipyard_terminal"));
        this.state = state;
    }

    public static void receive(ShipyardStatePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof ShipyardScreen s && s.state.terminal().equals(p.terminal())) {
            boolean rebuild = s.state.canStart() != p.canStart() || s.state.shared() != p.shared();
            s.state = p;
            if (rebuild) {
                s.rebuildWidgets();
            }
        } else {
            mc.setScreenAndShow(new ShipyardScreen(p));
        }
    }

    public ShipyardStatePayload state() {
        return state;
    }

    /** Höhe: Kopf, Abschnitte und unten eine Zeile für den Knopf „Partner“. */
    private int panelHeight() {
        return 40 + state.sections().size() * ROW + SHARE_ROW;
    }

    private static final int SHARE_ROW = 20;

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - panelHeight()) / 2;
        // 21.3: nach Abschnitt 8 erscheint „Start“
        net.minecraft.client.gui.components.Button start = net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.infinite_horizon.shipyard.start"),
                b -> Services.NETWORK.sendToServer(new ShipyardStatePayload.Start(state.terminal())))
                .bounds(left + W - 88, top + 4, 80, 16).build();
        start.visible = state.canStart();
        addRenderableWidget(start);
        // 23.3: Werft für Partner freigeben (Server prüft CONFIGURE)
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.infinite_horizon.cargo_terminal.partners." + (state.shared() ? "on" : "off")),
                b -> Services.NETWORK.sendToServer(new ShipyardStatePayload.Share(state.terminal(), !state.shared())))
                .bounds(left + W - 88, top + panelHeight() - SHARE_ROW - 2, 80, 16).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % REFRESH_TICKS == 0) {
            Services.NETWORK.sendToServer(new ShipyardStatePayload.Request(state.terminal()));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + panelHeight(), PANEL);
    }

    private static Component name(String id, boolean fluid) {
        Identifier i = Identifier.parse(id);
        return Component.translatable((fluid ? "block." : "item.") + i.getNamespace() + "." + i.getPath());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, left + 8, top + 6, TEXT, false);
        if (!state.valid()) {
            g.text(font, Component.translatable("gui.infinite_horizon.shipyard.invalid"), left + 8, top + 18, WARN, false);
        } else if (!state.hasDock()) {
            g.text(font, Component.translatable("gui.infinite_horizon.shipyard.no_dock"), left + 8, top + 18, WARN, false);
        }
        List<Component> tip = null;
        for (int i = 0; i < state.sections().size(); i++) {
            ShipyardStatePayload.Section s = state.sections().get(i);
            int y = top + 32 + i * ROW;
            g.text(font, Component.literal((i + 1) + " ").append(Component.translatable("gui.infinite_horizon.shipyard.section." + (i + 1))),
                    left + 8, y, TEXT, false);
            boolean done = i < state.built();
            boolean building = i == state.built() && state.remaining() > 0;
            int bx = left + BAR_X;
            g.fill(bx - 1, y - 1, bx + BAR_W + 1, y + BAR_H + 1, BAR_BG);
            int w = (int) Math.round(Math.min(1, s.progress()) * BAR_W);
            g.fill(bx, y, bx + w, y + BAR_H, done ? DONE : building ? BUILDING : BAR);
            Component status = done ? Component.translatable("gui.infinite_horizon.shipyard.built")
                    : building ? Component.translatable("gui.infinite_horizon.shipyard.building", formatTime(state.remaining()))
                    : Component.literal((int) Math.floor(s.progress() * 100) + " %");
            g.text(font, status, bx + BAR_W + 6, y, TEXT, false);
            if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= y - 1 && mouseY < y + BAR_H + 1) {
                tip = new ArrayList<>();
                for (ShipyardStatePayload.Material m : s.materials()) {
                    long miss = m.needed() - m.delivered();
                    tip.add(Component.translatable(miss > 0 ? "gui.infinite_horizon.shipyard.missing" : "gui.infinite_horizon.shipyard.complete",
                            name(m.id(), m.fluid()), m.delivered(), m.needed(), m.fluid() ? "mB" : ""));
                }
            }
        }
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    /** Restzeit als mm:ss bzw. h:mm:ss (Ticks → Sekunden). */
    static String formatTime(long ticks) {
        long s = ticks / TICKS_PER_SECOND;
        long h = s / 3600;
        long m = s % 3600 / 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s % 60) : String.format("%d:%02d", m, s % 60);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
