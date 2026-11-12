package de.infinitehorizon.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.ResearchRegistry;
import de.infinitehorizon.network.ResearchUnlockPayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.research.ResearchService;
import de.infinitehorizon.research.ResearchTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Forschungsterminal, Reiter „Forschung“ (11.3): Forschungsbaum wie der Vanilla-Erfolgsbildschirm auf einer
 * verschiebbaren Fläche (ziehen oder scrollen). 4 Spalten = Äste, 6 Zeilen = Stufen; mehrere Knoten einer Zelle stehen
 * nebeneinander nach {@code position.slot}. Knoten 26 × 26 px mit Item-Symbol, Rahmen grau (gesperrt), gelb
 * (verfügbar), grün (erforscht), Linien zu den Voraussetzungen. Zeilenköpfe mit Tor-Text und Haken, oben die FP.
 * Gezeichnet aus Rechtecken wie das Maschinen-GUI (24.5). Der Server entscheidet ({@code ResearchUnlockPayload}).
 */
public class ResearchTerminalScreen extends AbstractContainerScreen<ResearchTerminalMenu> {

    public static final int NODE = 26;
    private static final int GAP = 6;
    private static final int CELL_PAD = 6;
    private static final int ROW_HEADER_W = 104;
    private static final int COL_HEADER_H = 14;
    private static final int HEADER_H = 22;
    private static final int TAB_H = 14;
    private static final int MARGIN = 8;
    private static final int MAX_W = 420;
    private static final int MAX_H = 260;

    private static final int PANEL = 0xFFC6C6C6;
    private static final int VIEW = 0xFF2B2B33;
    private static final int GRID = 0xFF3A3A44;
    private static final int HEAD = 0xFF4A4A56;
    private static final int TEXT = 0xFF404040;
    private static final int LIGHT_TEXT = 0xFFE0E0E0;
    private static final int LINE = 0xFF9A9AA8;
    private static final int FRAME_LOCKED = 0xFF7A7A7A;
    private static final int FRAME_AVAILABLE = 0xFFF0C020;
    private static final int FRAME_RESEARCHED = 0xFF40D040;
    private static final int NODE_FILL = 0xFF1E1E24;
    private static final int TAB = 0xFFE8E8E8;

    private double scrollX;
    private double scrollY;
    private boolean dragging;
    /** Knotenpositionen in Inhaltskoordinaten (oben links), neu bei jedem Bild. */
    private final Map<Identifier, int[]> layout = new LinkedHashMap<>();
    private int contentW;
    private int contentH;
    private int[] columnX = new int[0];
    private int rowH;
    private int panelW = MAX_W;
    private int panelH = MAX_H;

    public ResearchTerminalScreen(ResearchTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MAX_W, MAX_H);
    }

    @Override
    protected void init() {
        super.init();
        // Fläche passt sich dem Fenster an (panelW/-Height sind in 26.3 fest)
        panelW = Math.min(MAX_W, width - 2 * MARGIN);
        panelH = Math.min(MAX_H, height - 2 * MARGIN);
        leftPos = (width - panelW) / 2;
        topPos = (height - panelH) / 2;
    }

    // ------------------------------------------------------------------ Aufbau

    private int viewX() {
        return leftPos + MARGIN;
    }

    private int viewY() {
        return topPos + TAB_H + HEADER_H;
    }

    private int viewW() {
        return panelW - 2 * MARGIN;
    }

    private int viewH() {
        return panelH - TAB_H - HEADER_H - MARGIN;
    }

    /** Ordnet die Knoten an: Spaltenbreite nach der vollsten Zelle des Asts. */
    private void computeLayout(ResearchRegistry reg) {
        layout.clear();
        int columns = ResearchRegistry.BRANCHES.size();
        int maxRow = 1;
        int[] perColumn = new int[columns];
        Map<String, List<ResearchRegistry.Node>> cells = new LinkedHashMap<>();
        for (ResearchRegistry.Node n : reg.nodes()) {
            if (n.column() < 0 || n.column() >= columns) {
                continue;
            }
            cells.computeIfAbsent(n.column() + ":" + n.row(), k -> new ArrayList<>()).add(n);
            maxRow = Math.max(maxRow, n.row());
        }
        maxRow = Math.max(maxRow, reg.tiers().isEmpty() ? 1 : reg.tiers().lastKey());
        cells.forEach((k, list) -> {
            int c = Integer.parseInt(k.substring(0, k.indexOf(':')));
            perColumn[c] = Math.max(perColumn[c], list.size());
        });
        columnX = new int[columns + 1];
        columnX[0] = ROW_HEADER_W;
        for (int c = 0; c < columns; c++) {
            int n = Math.max(1, perColumn[c]);
            columnX[c + 1] = columnX[c] + 2 * CELL_PAD + n * NODE + (n - 1) * GAP;
        }
        rowH = NODE + 2 * CELL_PAD;
        contentW = columnX[columns];
        contentH = COL_HEADER_H + maxRow * rowH;
        cells.forEach((k, list) -> {
            list.sort(java.util.Comparator.comparingInt(ResearchRegistry.Node::slot).thenComparing(n -> n.id().toString()));
            for (int i = 0; i < list.size(); i++) {
                ResearchRegistry.Node n = list.get(i);
                int x = columnX[n.column()] + CELL_PAD + i * (NODE + GAP);
                int y = COL_HEADER_H + (n.row() - 1) * rowH + CELL_PAD;
                layout.put(n.id(), new int[]{x, y});
            }
        });
        clampScroll();
    }

    private void clampScroll() {
        scrollX = Math.max(0, Math.min(scrollX, Math.max(0, contentW - viewW())));
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, contentH - viewH())));
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        ResearchRegistry reg = ResearchRegistry.client();
        computeLayout(reg);
        int x = leftPos;
        int y = topPos;
        // Reiter „Forschung“ (M4c: „Agentur“)
        int tabW = font.width(Component.translatable("gui.infinite_horizon.research.tab.research")) + 12;
        g.fill(x, y, x + tabW, y + TAB_H + 1, PANEL);
        g.text(font, Component.translatable("gui.infinite_horizon.research.tab.research"), x + 6, y + 3, TEXT, false);
        g.fill(x, y + TAB_H, x + panelW, y + panelH, PANEL);
        // Kopf: Agentur und FP
        g.text(font, AgencyClientState.name(), x + MARGIN, y + TAB_H + 4, TEXT, false);
        Component fp = Component.translatable("gui.infinite_horizon.research.fp", formatFp(AgencyClientState.fp()));
        g.text(font, fp, x + panelW - MARGIN - font.width(fp), y + TAB_H + 4, TEXT, false);
        if (!AgencyClientState.researchRequired()) {
            Component free = Component.translatable("gui.infinite_horizon.research.not_required");
            g.text(font, free, x + MARGIN, y + TAB_H + 13, TEXT, false);
        }

        int vx = viewX();
        int vy = viewY();
        g.fill(vx - 1, vy - 1, vx + viewW() + 1, vy + viewH() + 1, 0xFF000000);
        g.fill(vx, vy, vx + viewW(), vy + viewH(), VIEW);
        g.enableScissor(vx, vy, vx + viewW(), vy + viewH());
        int ox = vx - (int) scrollX;
        int oy = vy - (int) scrollY;
        Set<Identifier> researched = AgencyClientState.researched();
        Set<Integer> open = AgencyClientState.openTiers();

        // Raster, Spalten- und Zeilenköpfe
        for (int c = 0; c < ResearchRegistry.BRANCHES.size(); c++) {
            g.fill(ox + columnX[c], oy, ox + columnX[c + 1] - 1, oy + COL_HEADER_H - 1, HEAD);
            Component branch = Component.translatable("gui.infinite_horizon.research.branch." + ResearchRegistry.BRANCHES.get(c));
            g.centeredText(font, branch, ox + (columnX[c] + columnX[c + 1]) / 2, oy + 3, LIGHT_TEXT);
        }
        int rows = (contentH - COL_HEADER_H) / Math.max(1, rowH);
        for (int r = 1; r <= rows; r++) {
            int ry = oy + COL_HEADER_H + (r - 1) * rowH;
            g.fill(ox, ry, ox + contentW, ry + 1, GRID);
            g.fill(ox, ry + 1, ox + ROW_HEADER_W - 1, ry + rowH, HEAD);
            boolean isOpen = open.contains(r);
            MutableComponent head = Component.translatable(ResearchRegistry.tierNameKey(r));
            g.text(font, font.substrByWidth(Component.literal(r + " ").append(head), ROW_HEADER_W - 16).getString(),
                    ox + 4, ry + 5, LIGHT_TEXT, false);
            if (isOpen) {
                g.text(font, "✔", ox + ROW_HEADER_W - 12, ry + 5, FRAME_RESEARCHED, false);
            }
            Component gate = reg.tier(r).map(t -> gateText(t.gate())).orElse(Component.empty());
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(gate, ROW_HEADER_W - 8);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.text(font, lines.get(i), ox + 4, ry + 16 + i * 9, isOpen ? 0xFFA0E0A0 : 0xFFB0B0B0, false);
            }
        }
        for (int c = 1; c <= ResearchRegistry.BRANCHES.size(); c++) {
            g.fill(ox + columnX[c] - 1, oy, ox + columnX[c], oy + contentH, GRID);
        }

        // Linien zu den Voraussetzungen
        for (ResearchRegistry.Node n : reg.nodes()) {
            int[] to = layout.get(n.id());
            if (to == null) {
                continue;
            }
            for (Identifier req : n.requires()) {
                int[] from = layout.get(req);
                if (from != null) {
                    connect(g, ox + from[0] + NODE / 2, oy + from[1] + NODE / 2, ox + to[0] + NODE / 2, oy + to[1] + NODE / 2);
                }
            }
        }
        // Knoten
        ResearchRegistry.Node hovered = null;
        for (ResearchRegistry.Node n : reg.nodes()) {
            int[] p = layout.get(n.id());
            if (p == null) {
                continue;
            }
            int nx = ox + p[0];
            int ny = oy + p[1];
            ResearchService.NodeState state = ResearchService.state(n, researched, open);
            int frame = switch (state) {
                case RESEARCHED -> FRAME_RESEARCHED;
                case AVAILABLE -> FRAME_AVAILABLE;
                default -> FRAME_LOCKED;
            };
            g.fill(nx, ny, nx + NODE, ny + NODE, frame);
            g.fill(nx + 2, ny + 2, nx + NODE - 2, ny + NODE - 2, NODE_FILL);
            g.item(icon(n), nx + (NODE - 16) / 2, ny + (NODE - 16) / 2);
            if (inView(mouseX, mouseY) && mouseX >= nx && mouseX < nx + NODE && mouseY >= ny && mouseY < ny + NODE) {
                hovered = n;
            }
        }
        g.disableScissor();
        if (hovered != null) {
            g.setComponentTooltipForNextFrame(font, tooltip(hovered, ResearchService.state(hovered, researched, open)), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // Titel und Inventar-Beschriftung entfallen (eigener Kopf)
    }

    /** Linie im rechten Winkel (waagerecht, senkrecht, waagerecht), 1 px. */
    private static void connect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        int mid = (x1 + x2) / 2;
        g.fill(Math.min(x1, mid), y1, Math.max(x1, mid) + 1, y1 + 1, LINE);
        g.fill(mid, Math.min(y1, y2), mid + 1, Math.max(y1, y2) + 1, LINE);
        g.fill(Math.min(mid, x2), y2, Math.max(mid, x2) + 1, y2 + 1, LINE);
    }

    private static ItemStack icon(ResearchRegistry.Node n) {
        var item = BuiltInRegistries.ITEM.getOptional(n.icon()).orElse(Items.BOOK);
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }

    /** FP aus Hundertsteln mit 2 Nachkommastellen (11.3). */
    public static String formatFp(int hundredths) {
        return String.format(Locale.ROOT, "%d.%02d", hundredths / 100, hundredths % 100);
    }

    private List<Component> tooltip(ResearchRegistry.Node n, ResearchService.NodeState state) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable(n.nameKey()).withStyle(ChatFormatting.WHITE));
        tip.add(Component.translatable("gui.infinite_horizon.research.cost", n.cost()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("gui.infinite_horizon.research.state." + state.name().toLowerCase(Locale.ROOT))
                .withStyle(state == ResearchService.NodeState.RESEARCHED ? ChatFormatting.GREEN
                        : state == ResearchService.NodeState.AVAILABLE ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        if (!n.unlocks().isEmpty()) {
            tip.add(Component.translatable("gui.infinite_horizon.research.unlocks").withStyle(ChatFormatting.GRAY));
            for (ResearchRegistry.Unlock u : n.unlocks()) {
                tip.add(Component.literal(" • ").append(unlockName(u)).withStyle(ChatFormatting.GRAY));
            }
        }
        List<Identifier> missing = new ArrayList<>();
        for (Identifier req : n.requires()) {
            if (!AgencyClientState.researched().contains(req)) {
                missing.add(req);
            }
        }
        if (!missing.isEmpty() && state != ResearchService.NodeState.RESEARCHED) {
            tip.add(Component.translatable("gui.infinite_horizon.research.requires").withStyle(ChatFormatting.RED));
            for (Identifier req : missing) {
                Component name = ResearchRegistry.client().node(req).<Component>map(r -> Component.translatable(r.nameKey()))
                        .orElse(Component.literal(req.toString()));
                tip.add(Component.literal(" • ").append(name).withStyle(ChatFormatting.RED));
            }
        }
        return tip;
    }

    private static Component unlockName(ResearchRegistry.Unlock u) {
        if (u.type().equals(ResearchRegistry.Unlock.PROBE_TARGET)) {
            return Component.translatable("gui.infinite_horizon.research.probe_target",
                    Component.translatable("planet." + u.id().getNamespace() + "." + u.id().getPath()));
        }
        return BuiltInRegistries.ITEM.getOptional(u.id()).filter(i -> i != Items.AIR)
                .<Component>map(i -> i.getName(new ItemStack(i)))
                .orElse(Component.translatable("gui.infinite_horizon.research.unknown_unlock"));
    }

    /** Tor-Text (25.3) für den Zeilenkopf. */
    private static Component gateText(JsonObject gate) {
        if (gate == null || !gate.has("type")) {
            return Component.empty();
        }
        String type = gate.get("type").getAsString();
        String key = "gui.infinite_horizon.research.gate." + type;
        return switch (type) {
            case "has_block", "machine_completed" -> Component.translatable(key, blockName(gate.get("block").getAsString()));
            case "enter_dimension" -> {
                Identifier d = Identifier.parse(gate.get("dimension").getAsString());
                yield Component.translatable(key, Component.translatable("planet." + d.getNamespace() + "." + d.getPath()));
            }
            case "crafted_items" -> {
                MutableComponent list = Component.empty();
                boolean first = true;
                for (String k : new String[]{"items", "fluids"}) {
                    if (gate.has(k) && gate.get(k).isJsonArray()) {
                        for (JsonElement e : gate.getAsJsonArray(k)) {
                            list.append(first ? Component.empty() : Component.literal(", ")).append(blockName(e.getAsString()));
                            first = false;
                        }
                    }
                }
                yield Component.translatable(key, list);
            }
            case "all" -> {
                MutableComponent all = Component.empty();
                boolean first = true;
                for (JsonElement e : gate.getAsJsonArray("gates")) {
                    all.append(first ? Component.empty() : Component.literal(" + ")).append(gateText(e.getAsJsonObject()));
                    first = false;
                }
                yield all;
            }
            default -> Component.literal(type);
        };
    }

    private static Component blockName(String id) {
        Identifier i = Identifier.parse(id);
        return BuiltInRegistries.ITEM.getOptional(i).filter(it -> it != Items.AIR)
                .<Component>map(it -> it.getName(new ItemStack(it)))
                .orElse(Component.translatable("block." + i.getNamespace() + "." + i.getPath()));
    }

    // ------------------------------------------------------------------ Eingaben

    private boolean inView(double mx, double my) {
        return mx >= viewX() && mx < viewX() + viewW() && my >= viewY() && my < viewY() + viewH();
    }

    private ResearchRegistry.@Nullable Node nodeAt(double mx, double my) {
        if (!inView(mx, my)) {
            return null;
        }
        double cx = mx - viewX() + scrollX;
        double cy = my - viewY() + scrollY;
        for (Map.Entry<Identifier, int[]> e : layout.entrySet()) {
            int[] p = e.getValue();
            if (cx >= p[0] && cx < p[0] + NODE && cy >= p[1] && cy < p[1] + NODE) {
                return ResearchRegistry.client().node(e.getKey()).orElse(null);
            }
        }
        return null;
    }

    /** Klick auf einen verfügbaren Knoten: Anfrage an den Server (er prüft alles, 11.3). */
    public boolean clickNode(Identifier id) {
        ResearchRegistry.Node n = ResearchRegistry.client().node(id).orElse(null);
        if (n == null || ResearchService.state(n, AgencyClientState.researched(), AgencyClientState.openTiers())
                != ResearchService.NodeState.AVAILABLE) {
            return false;
        }
        Services.NETWORK.sendToServer(new ResearchUnlockPayload(id));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            ResearchRegistry.Node n = nodeAt(event.x(), event.y());
            if (n != null) {
                clickNode(n.id());
                return true;
            }
            if (inView(event.x(), event.y())) {
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            scrollX -= dx;
            scrollY -= dy;
            clampScroll();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (inView(mx, my)) {
            // Umschalt = waagerecht (wie Vanilla-Listen)
            if (dx != 0 || minecraft != null && minecraft.hasShiftDown()) {
                scrollX -= (dx != 0 ? dx : dy) * NODE;
            } else {
                scrollY -= dy * NODE;
            }
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    /** Für Tests: Inhaltsposition eines Knotens (oben links) oder null. */
    public int @Nullable [] nodePosition(Identifier id) {
        return layout.get(id);
    }
}
