package de.infinitehorizon.client;

import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.Route;
import de.infinitehorizon.logistics.RouteRules;
import de.infinitehorizon.network.LogisticsEditPayload;
import de.infinitehorizon.network.LogisticsStatePayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Logistik-Computer (20.4): links die Routen der Agentur, rechts der Editor (Start, Ziel, Startbedingung mit N und X,
 * Fracht-Filter, Raketen aus dem Hangar des Starts) und die Statusliste der Raketen. Aus Rechtecken gezeichnet.
 */
public class LogisticsScreen extends Screen {

    private static final int W = 320;
    private static final int H = 226;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;
    private static final int WARN = 0xFFB02020;
    private static final int LIST_W = 112;
    private static final int ROW = 20;
    private static final int MAX_ROWS = 8;
    private static final int REFRESH_TICKS = 40;

    private LogisticsStatePayload state;
    private @Nullable UUID routeId;
    private int home;
    private int target = 1;
    private int condition;
    private String amount = "1200";
    private String item = "";
    private String filter = "";
    private TreeSet<Integer> rockets = new TreeSet<>();
    private @Nullable EditBox amountBox;
    private @Nullable EditBox itemBox;
    private @Nullable EditBox filterBox;
    private int left;
    private int top;
    private int ticks;

    public LogisticsScreen(LogisticsStatePayload state) {
        super(Component.translatable("block.infinite_horizon.logistics_computer"));
        this.state = state;
        if (!state.routes().isEmpty()) {
            load(state.routes().getFirst());
        }
    }

    /** Antwort des Servers: offenes GUI aktualisieren oder neu öffnen. */
    public static void receive(LogisticsStatePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof LogisticsScreen s && s.state.computer().equals(p.computer())) {
            s.update(p);
        } else {
            mc.setScreenAndShow(new LogisticsScreen(p));
        }
    }

    private void update(LogisticsStatePayload p) {
        keepText();
        state = p;
        if (routeId != null && p.routes().stream().noneMatch(r -> r.id().equals(routeId))) {
            routeId = null;
        }
        rebuildWidgets();
    }

    public LogisticsStatePayload state() {
        return state;
    }

    private void keepText() {
        if (amountBox != null) {
            amount = amountBox.getValue();
            item = itemBox.getValue();
            filter = filterBox.getValue();
        }
    }

    private int terminalIndex(net.minecraft.resources.Identifier dim, net.minecraft.core.BlockPos pos) {
        for (int i = 0; i < state.terminals().size(); i++) {
            LogisticsStatePayload.Terminal t = state.terminals().get(i);
            if (t.dimension().equals(dim) && t.pos().equals(pos)) {
                return i;
            }
        }
        return 0;
    }

    private void load(Route r) {
        routeId = r.id();
        home = terminalIndex(r.homeDimension(), r.home());
        target = terminalIndex(r.targetDimension(), r.target());
        condition = RouteRules.Condition.byId(r.condition()).ordinal();
        amount = Long.toString(r.amount());
        item = r.item();
        filter = String.join(", ", r.filter());
        rockets = new TreeSet<>(r.rockets());
    }

    private void newRoute() {
        keepText();
        routeId = null;
        rockets = new TreeSet<>();
        filter = "";
        rebuildWidgets();
    }

    private Component terminalName(int i) {
        if (state.terminals().isEmpty()) {
            return Component.literal("-");
        }
        LogisticsStatePayload.Terminal t = state.terminals().get(Math.floorMod(i, state.terminals().size()));
        String path = t.dimension().getPath();
        return Component.translatableWithFallback("planet." + t.dimension().getNamespace() + "." + path, path)
                .append(" " + t.pos().getX() + " " + t.pos().getZ());
    }

    private static Component planetName(net.minecraft.resources.Identifier dim) {
        return Component.translatableWithFallback("planet." + dim.getNamespace() + "." + dim.getPath(), dim.getPath());
    }

    private LogisticsStatePayload.@Nullable Terminal terminal(int i) {
        return state.terminals().isEmpty() ? null : state.terminals().get(Math.floorMod(i, state.terminals().size()));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        // Routenliste
        List<Route> routes = state.routes();
        for (int i = 0; i < Math.min(MAX_ROWS, routes.size()); i++) {
            Route r = routes.get(i);
            // in der Liste nur die Himmelskörper, die Koordinaten stehen im Editor
            Component label = Component.literal((i + 1) + ": ").append(planetName(r.homeDimension()))
                    .append(" → ").append(planetName(r.targetDimension()));
            Button b = Button.builder(label, x -> {
                keepText();
                load(r);
                rebuildWidgets();
            }).bounds(left + 8, top + 20 + i * ROW, LIST_W, ROW - 2).build();
            b.active = !r.id().equals(routeId);
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.logistics.new"), b -> newRoute())
                .bounds(left + 8, top + H - 26, LIST_W / 2 - 2, 18).build());
        Button delete = Button.builder(Component.translatable("gui.infinite_horizon.logistics.delete"), b -> send(LogisticsEditPayload.DELETE))
                .bounds(left + 8 + LIST_W / 2 + 2, top + H - 26, LIST_W / 2 - 2, 18).build();
        delete.active = routeId != null;
        addRenderableWidget(delete);

        // Editor
        int x0 = left + LIST_W + 16;
        int w = W - LIST_W - 24;
        int n = Math.max(1, state.terminals().size());
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.logistics.start", terminalName(home)), b -> {
            keepText();
            home = (home + 1) % n;
            rockets.clear();
            rebuildWidgets();
        }).bounds(x0, top + 20, w, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.logistics.target", terminalName(target)), b -> {
            keepText();
            target = (target + 1) % n;
            rebuildWidgets();
        }).bounds(x0, top + 42, w, 18).build());
        RouteRules.Condition c = RouteRules.Condition.values()[condition];
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.logistics.condition."
                + c.id()), b -> {
            keepText();
            condition = (condition + 1) % RouteRules.Condition.values().length;
            rebuildWidgets();
        }).bounds(x0, top + 64, w, 18).build());
        amountBox = new EditBox(font, x0 + 16, top + 88, 56, 16, Component.literal("N"));
        amountBox.setValue(amount);
        amountBox.setMaxLength(9);
        addRenderableWidget(amountBox);
        itemBox = new EditBox(font, x0 + 96, top + 88, w - 96, 16, Component.literal("X"));
        itemBox.setMaxLength(64);
        itemBox.setValue(item);
        itemBox.setHint(Component.translatable("gui.infinite_horizon.logistics.item_hint"));
        addRenderableWidget(itemBox);
        filterBox = new EditBox(font, x0, top + 120, w - 40, 16, Component.translatable("gui.infinite_horizon.logistics.cargo"));
        filterBox.setMaxLength(512);
        filterBox.setValue(filter);
        filterBox.setHint(Component.translatable("gui.infinite_horizon.logistics.all"));
        addRenderableWidget(filterBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.logistics.hand"), b -> addHeld())
                .bounds(x0 + w - 36, top + 119, 36, 18).build());
        // Raketen im Hangar des Starts
        LogisticsStatePayload.Terminal t = terminal(home);
        int slots = t == null ? 0 : t.hangar().size();
        for (int i = 0; i < slots; i++) {
            int slot = i;
            boolean has = !t.hangar().get(i).isEmpty();
            Component label = Component.literal((rockets.contains(i) ? "✔" : "") + (i + 1));
            Button b = Button.builder(label, x -> {
                keepText();
                if (!rockets.remove(slot)) {
                    rockets.add(slot);
                }
                rebuildWidgets();
            }).bounds(x0 + 52 + i * 26, top + 142, 24, 18).build();
            b.active = has || rockets.contains(i);
            addRenderableWidget(b);
        }
        Button save = Button.builder(Component.translatable("gui.infinite_horizon.logistics.save"), b -> send(LogisticsEditPayload.SAVE))
                .bounds(x0, top + 166, 90, 18).build();
        save.active = state.terminals().size() > 1;
        addRenderableWidget(save);
    }

    /** Item in der Hand zum Fracht-Filter hinzufügen. */
    private void addHeld() {
        if (minecraft == null || minecraft.player == null || filterBox == null) {
            return;
        }
        var stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<String> parts = new ArrayList<>(Arrays.stream(filterBox.getValue().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (!parts.contains(id)) {
            parts.add(id);
        }
        filterBox.setValue(String.join(", ", parts));
    }

    private void send(int action) {
        keepText();
        LogisticsStatePayload.Terminal h = terminal(home);
        LogisticsStatePayload.Terminal t = terminal(target);
        if (h == null || t == null) {
            return;
        }
        List<String> f = Arrays.stream(filter.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        long n;
        try {
            n = amount.isEmpty() ? 0 : Long.parseLong(amount);
        } catch (NumberFormatException e) {
            n = 0;
        }
        Route r = new Route(routeId != null ? routeId : UUID.randomUUID(), new UUID(0, 0), h.dimension(), h.pos(),
                t.dimension(), t.pos(), f, RouteRules.Condition.values()[condition].id(), n, item.trim(),
                List.copyOf(rockets), 0);
        Services.NETWORK.sendToServer(new LogisticsEditPayload(state.computer(), action, r));
    }

    @Override
    public void tick() {
        super.tick();
        // Statusliste regelmäßig neu holen (Restzeiten)
        if (++ticks % REFRESH_TICKS == 0 && !state.terminals().isEmpty()) {
            LogisticsStatePayload.Terminal h = state.terminals().getFirst();
            Services.NETWORK.sendToServer(new LogisticsEditPayload(state.computer(), LogisticsEditPayload.REFRESH,
                    new Route(new UUID(0, 0), new UUID(0, 0), h.dimension(), h.pos(), h.dimension(), h.pos(), List.of(),
                            "", 0, "", List.of(), 0)));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int x0 = left + LIST_W + 16;
        g.text(font, title, left + 8, top + 6, TEXT, false);
        if (state.routes().isEmpty()) {
            g.text(font, Component.translatable("gui.infinite_horizon.logistics.no_routes"), left + 8, top + 24, TEXT, false);
        }
        g.text(font, Component.literal("N"), x0, top + 92, TEXT, false);
        g.text(font, Component.literal("X"), x0 + 82, top + 92, TEXT, false);
        g.text(font, Component.translatable("gui.infinite_horizon.logistics.cargo"), x0, top + 109, TEXT, false);
        g.text(font, Component.translatable("gui.infinite_horizon.logistics.rockets"), x0, top + 147, TEXT, false);
        int y = top + 190;
        if (state.terminals().size() < 2) {
            g.text(font, Component.translatable("gui.infinite_horizon.logistics.need_terminals"), x0, y, WARN, false);
            y += 10;
        }
        if (routeId != null) {
            if (state.warnings().contains(routeId)) {
                g.text(font, Component.translatable("gui.infinite_horizon.logistics.fuel_warning"), x0, y, WARN, false);
                y += 10;
            }
            for (CargoFlights.RocketStatus s : state.statuses()) {
                if (s.route().equals(routeId) && y < top + H - 4) {
                    Component line = Component.translatable("gui.infinite_horizon.logistics.status." + s.status(),
                            s.slot() + 1, s.remaining() / 20);
                    g.text(font, line, x0, y, TEXT, false);
                    y += 10;
                }
            }
        }
        if (!state.message().isEmpty() && y < top + H - 4) {
            g.text(font, Component.translatable(state.message()), x0 + 96, top + 171, TEXT, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
