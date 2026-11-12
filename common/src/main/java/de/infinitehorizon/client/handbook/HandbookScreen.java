package de.infinitehorizon.client.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.handbook.HandbookMarkup;
import de.infinitehorizon.recipe.AmountMultiplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Buch-GUI des Ingenieurs-Handbuchs (24.1, 25.6), Notizbuch-Stil aus Rechtecken (Blaupausen-Optik ab M9).
 * Links Inhaltsverzeichnis mit Suche, rechts die Seite mit Bildlauf; Links, Item-Namen und Rezept-Symbole sind anklickbar,
 * der Zurück-Knopf geht durch den Verlauf. Unbekannte Elementtypen werden übersprungen.
 */
public class HandbookScreen extends Screen {

    // Platzhalter-Optik (Notizbuch): Papier, Linien, Schrift
    private static final int PAPER = 0xFFF4EBD0;
    private static final int PAPER_DARK = 0xFFE6D9B4;
    private static final int EDGE = 0xFF6B5532;
    private static final int TEXT = 0xFF2B2B2B;
    private static final int MUTED = 0xFF6E6656;
    private static final int HEADING = 0xFF1D3B6A;
    private static final int LINK = 0xFF1F5FBF;
    private static final int ITEM = 0xFF2E7D32;
    private static final int SELECTED = 0xFFD8C99A;
    private static final int TIP = 0xFFDCEBD0;
    private static final int NOTE = 0xFFD7E4F2;
    private static final int WARNING = 0xFFF5DCC0;
    private static final int MAX_W = 420;
    private static final int MAX_H = 260;
    private static final int TOC_W = 120;
    private static final int PAD = 6;
    private static final int LINE = 10;
    private static final int ICON = 18;
    private static final int SCROLL_STEP = 12;

    /** Ein Stück Text an fester Stelle (Koordinaten relativ zum Seiteninhalt). */
    private record Run(int x, int y, Component text, int width, @Nullable String link, @Nullable String item) {
    }

    /** Ein Item-Symbol (Rezepte, Steckbrief). */
    private record Icon(int x, int y, ItemStack stack, @Nullable String label) {
    }

    /** Hintergrundfläche (Hinweiskasten, Tabellenkopf). */
    private record Box(int x1, int y1, int x2, int y2, int color) {
    }

    /** Eintrag im Inhaltsverzeichnis. */
    private record TocEntry(String pageId, Component label, boolean chapter) {
    }

    private final Deque<String> history = new ArrayDeque<>();
    private HandbookData.Handbook book = HandbookData.Handbook.EMPTY;
    private HandbookData.@Nullable Page page;
    private final List<Run> runs = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
    private final List<TocEntry> toc = new ArrayList<>();
    private @Nullable String requested;
    private EditBox search;
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int contentH;
    private double scroll;
    private double tocScroll;

    public HandbookScreen(@Nullable String pageId) {
        super(Component.translatable("item." + InfiniteHorizon.MOD_ID + ".engineers_handbook"));
        this.requested = pageId;
    }

    // ------------------------------------------------------------------ Aufbau

    @Override
    protected void init() {
        book = HandbookClient.handbook();
        panelW = Math.min(width - 20, MAX_W);
        panelH = Math.min(height - 20, MAX_H);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, left + PAD, top + PAD, TOC_W - 2 * PAD, 14,
                Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".handbook.search"));
        search.setHint(Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".handbook.search").withStyle(ChatFormatting.GRAY));
        search.setValue(previous);
        search.setResponder(q -> rebuildToc());
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> back())
                .bounds(left + TOC_W + PAD, top + PAD - 1, 14, 14)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".handbook.back")))
                .build());
        rebuildToc();
        String target = page != null ? page.id() : requested;
        HandbookData.Page p = target == null ? null : book.pages().get(target);
        show(p != null ? p : book.firstPage(), false);
    }

    /** Öffnet eine Seite; {@code remember} legt die alte in den Verlauf. */
    public void show(HandbookData.@Nullable Page next, boolean remember) {
        if (remember && page != null && next != page) {
            history.push(page.id());
        }
        page = next;
        scroll = 0;
        if (next != null) {
            HandbookClient.lastPage = next.id();
        }
        layoutPage();
    }

    /** Öffnet die Seite mit dieser ID (Link). */
    public boolean openPage(String id) {
        HandbookData.Page p = book.pages().get(id);
        if (p == null) {
            return false;
        }
        show(p, true);
        return true;
    }

    private void back() {
        while (!history.isEmpty()) {
            HandbookData.Page p = book.pages().get(history.pop());
            if (p != null) {
                show(p, false);
                return;
            }
        }
    }

    /** Aktuell offene Seite (für Tests). */
    public @Nullable String currentPage() {
        return page == null ? null : page.id();
    }

    private void rebuildToc() {
        toc.clear();
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (HandbookData.Chapter c : book.chapters()) {
            List<TocEntry> entries = new ArrayList<>();
            for (HandbookData.Page p : book.pagesByChapter().getOrDefault(c.id(), List.of())) {
                Component title = Component.translatable(p.titleKey());
                if (q.isEmpty() || matches(p, title, q)) {
                    entries.add(new TocEntry(p.id(), title, false));
                }
            }
            if (!entries.isEmpty()) {
                toc.add(new TocEntry("", Component.translatable(c.titleKey()), true));
                toc.addAll(entries);
            }
        }
        tocScroll = 0;
    }

    /** Suche: Seitentitel und Item-Namen (24.1). */
    private static boolean matches(HandbookData.Page p, Component title, String q) {
        if (title.getString().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String id : p.items()) {
            if (stackOf(id, 1).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Seitenlayout

    private int contentX() {
        return left + TOC_W + PAD;
    }

    private int contentY() {
        return top + PAD + 18;
    }

    private int contentW() {
        return panelW - TOC_W - 2 * PAD;
    }

    private int contentBottom() {
        return top + panelH - PAD;
    }

    private void layoutPage() {
        runs.clear();
        icons.clear();
        boxes.clear();
        images.clear();
        if (page == null) {
            contentH = 0;
            return;
        }
        int w = contentW() - 4;
        int y = 0;
        for (JsonElement el : page.elements()) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String type = e.has("type") ? e.get("type").getAsString() : "";
            switch (type) {
                case "heading" -> {
                    y += 4;
                    y = text(tr(e, "key"), 0, y, w, HEADING, true);
                    y += 2;
                }
                case "text" -> y = text(tr(e, "key"), 0, y, w, TEXT, false) + 4;
                case "list" -> {
                    JsonArray keys = e.getAsJsonArray("keys");
                    boolean ordered = e.has("ordered") && e.get("ordered").getAsBoolean();
                    for (int i = 0; i < keys.size(); i++) {
                        String bullet = ordered ? (i + 1) + "." : "•";
                        runs.add(new Run(0, y, Component.literal(bullet).withColor(TEXT), font.width(bullet), null, null));
                        y = text(Component.translatable(keys.get(i).getAsString()).getString(), 12, y, w - 12, TEXT, false);
                    }
                    y += 4;
                }
                case "callout" -> {
                    String style = e.has("style") ? e.get("style").getAsString() : "note";
                    int color = switch (style) {
                        case "tip" -> TIP;
                        case "warning" -> WARNING;
                        default -> NOTE;
                    };
                    int start = y;
                    y = text(tr(e, "key"), 6, y + 3, w - 10, TEXT, false) + 3;
                    boxes.add(new Box(0, start, w, y, color));
                    y += 4;
                }
                case "machine" -> y = machine(e, y, w) + 4;
                case "table" -> y = table(e, y, w) + 4;
                case "recipes" -> {
                    for (JsonElement r : e.getAsJsonArray("recipes")) {
                        y = recipe(r.getAsJsonObject(), y, w) + 4;
                    }
                }
                case "image" -> y = image(e, y, w) + 4;
                default -> {
                    // unbekannte Elemente überspringt das GUI (25.6)
                }
            }
        }
        contentH = y;
    }

    private static String tr(JsonObject e, String key) {
        return e.has(key) ? Component.translatable(e.get(key).getAsString()).getString() : "";
    }

    /** Fließtext mit Markup; Rückgabe: y unter der letzten Zeile. */
    private int text(String raw, int x0, int y, int width, int color, boolean allBold) {
        int x = x0;
        int space = font.width(" ");
        boolean any = false;
        for (HandbookMarkup.Segment seg : HandbookMarkup.parse(raw)) {
            String str;
            String link = null;
            String item = null;
            int c = color;
            boolean bold = allBold || seg.bold();
            boolean underline = false;
            switch (seg) {
                case HandbookMarkup.Text t -> str = t.text();
                case HandbookMarkup.Link l -> {
                    str = l.label();
                    link = l.target();
                    c = LINK;
                    underline = true;
                }
                case HandbookMarkup.ItemRef ir -> {
                    str = stackOf(ir.id(), 1).getHoverName().getString();
                    item = ir.id();
                    c = ITEM;
                    underline = true;
                }
            }
            for (String piece : str.split("(?<= )")) {
                boolean trailing = piece.endsWith(" ");
                String word = trailing ? piece.substring(0, piece.length() - 1) : piece;
                if (!word.isEmpty()) {
                    Style st = Style.EMPTY.withColor(c).withBold(bold).withUnderlined(underline);
                    MutableComponent comp = Component.literal(word).withStyle(st);
                    int ww = font.width(comp);
                    if (x + ww > x0 + width && x > x0) {
                        x = x0;
                        y += LINE;
                    }
                    runs.add(new Run(x, y, comp, ww, link, item));
                    x += ww;
                    any = true;
                }
                if (trailing) {
                    x += space;
                }
            }
        }
        return any ? y + LINE : y;
    }

    /** Steckbrief: Block-Symbol und Zeilen „Wert: Inhalt“. */
    private int machine(JsonObject e, int y, int w) {
        String block = e.has("block") ? e.get("block").getAsString() : "";
        int start = y;
        icons.add(new Icon(2, y + 2, stackOf(block, 1), null));
        int labelW = 0;
        JsonArray rows = e.has("rows") ? e.getAsJsonArray("rows") : new JsonArray();
        for (JsonElement r : rows) {
            labelW = Math.max(labelW, font.width(Component.translatable(r.getAsJsonArray().get(0).getAsString()).getString()) + 6);
        }
        labelW = Math.min(labelW, (w - ICON - 4) / 2);
        for (JsonElement r : rows) {
            JsonArray row = r.getAsJsonArray();
            String label = Component.translatable(row.get(0).getAsString()).getString();
            String value = row.size() > 1 ? Component.translatable(row.get(1).getAsString()).getString() : "";
            int ly = text("**" + label + "**", ICON + 6, y + 2, labelW, TEXT, false);
            int vy = text(value, ICON + 6 + labelW, y + 2, w - ICON - 6 - labelW, TEXT, false);
            y = Math.max(ly, vy);
        }
        boxes.add(new Box(0, start, w, y + 2, PAPER_DARK));
        return y + 2;
    }

    /** Tabelle mit gleich breiten Spalten; Kopfzeile fett. */
    private int table(JsonObject e, int y, int w) {
        JsonArray headers = e.has("headers") ? e.getAsJsonArray("headers") : new JsonArray();
        JsonArray rows = e.has("rows") ? e.getAsJsonArray("rows") : new JsonArray();
        int cols = Math.max(1, headers.size());
        int colW = w / cols;
        int start = y;
        int next = y;
        for (int c = 0; c < headers.size(); c++) {
            next = Math.max(next, text(Component.translatable(headers.get(c).getAsString()).getString(), c * colW + 2, y + 1,
                    colW - 4, TEXT, true));
        }
        boxes.add(new Box(0, start, w, next + 1, PAPER_DARK));
        y = next + 2;
        for (JsonElement r : rows) {
            JsonArray row = r.getAsJsonArray();
            int rowEnd = y;
            for (int c = 0; c < row.size() && c < cols; c++) {
                rowEnd = Math.max(rowEnd, text(Component.translatable(row.get(c).getAsString()).getString(), c * colW + 2, y,
                        colW - 4, TEXT, false));
            }
            y = rowEnd + 1;
        }
        return y;
    }

    /** Rezept als Symbole: Zutaten (oder 3 × 3-Raster), Pfeil, Ergebnisse; darunter Station, Dauer, Energie, Fluide. */
    private int recipe(JsonObject r, int y, int w) {
        int x = 0;
        int rowY = y;
        double multiplier = "infinite_horizon:assembly".equals(str(r, "type"))
                ? ServerConfig.client().getDouble("recipe_amount_multiplier") : 1.0;
        if (r.has("pattern") && r.has("key")) {
            JsonArray pattern = r.getAsJsonArray("pattern");
            JsonObject key = r.getAsJsonObject("key");
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row).getAsString();
                for (int col = 0; col < line.length(); col++) {
                    String ch = String.valueOf(line.charAt(col));
                    if (key.has(ch) && !key.getAsJsonArray(ch).isEmpty()) {
                        icons.add(new Icon(col * ICON, y + row * ICON, stackOf(key.getAsJsonArray(ch).get(0).getAsString(), 1), null));
                    }
                }
            }
            x = 3 * ICON + 4;
            rowY = y + ICON;
            y += pattern.size() * ICON;
        } else if (r.has("ingredients")) {
            JsonArray ings = r.getAsJsonArray("ingredients");
            int[] counts = new int[ings.size()];
            int[] stacks = new int[ings.size()];
            String[] ids = new String[ings.size()];
            for (int i = 0; i < ings.size(); i++) {
                JsonObject ing = ings.get(i).getAsJsonObject();
                ids[i] = ing.getAsJsonArray("items").isEmpty() ? "" : ing.getAsJsonArray("items").get(0).getAsString();
                counts[i] = ing.has("count") ? ing.get("count").getAsInt() : 1;
                stacks[i] = stackOf(ids[i], 1).getMaxStackSize();
            }
            // Mengen-Multiplikator (22.3) beim Anzeigen von Montagetisch-Zutaten, Wert aus der synchronisierten Config
            if (multiplier > 0 && multiplier != 1.0) {
                counts = AmountMultiplier.scale(counts, stacks, multiplier, 9);
            }
            for (int i = 0; i < ids.length; i++) {
                if (x + ICON > w - 3 * ICON) {
                    x = 0;
                    y += ICON;
                }
                icons.add(new Icon(x, y, stackOf(ids[i], counts[i]), null));
                x += ICON;
            }
            rowY = y;
            y += ICON;
        }
        if (r.has("mold") && !r.get("mold").isJsonNull()) {
            icons.add(new Icon(x, rowY, stackOf(r.get("mold").getAsString(), 1),
                    Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".handbook.mold").getString()));
            x += ICON;
        }
        runs.add(new Run(x + 2, rowY + 4, Component.literal("→").withColor(TEXT), font.width("→"), null, null));
        x += font.width("→") + 6;
        if (r.has("results")) {
            for (JsonElement res : r.getAsJsonArray("results")) {
                JsonObject o = res.getAsJsonObject();
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                String label = o.has("chance") && o.get("chance").getAsDouble() < 1.0
                        ? Math.round(o.get("chance").getAsDouble() * 100) + " %" : null;
                icons.add(new Icon(x, rowY, stackOf(str(o, "item"), count), label));
                x += ICON;
            }
        }
        // Fluide, Station, Dauer und Energie als kleine Zeile darunter
        StringBuilder info = new StringBuilder();
        if (r.has("fluid") && r.get("fluid").isJsonObject()) {
            info.append(fluidText(r.getAsJsonObject("fluid"))).append(" → ");
        }
        if (r.has("fluid_results")) {
            List<String> outs = new ArrayList<>();
            for (JsonElement f : r.getAsJsonArray("fluid_results")) {
                outs.add(fluidText(f.getAsJsonObject()));
            }
            info.append(String.join(" + ", outs)).append("  ");
        }
        if (r.has("station")) {
            info.append(stackOf(str(r, "station"), 1).getHoverName().getString());
        }
        if (r.has("ticks") && !r.get("ticks").isJsonNull()) {
            int ticks = r.get("ticks").getAsInt();
            info.append(" · ").append(ticks).append(" t");
            if (r.has("energy_per_tick") && !r.get("energy_per_tick").isJsonNull()) {
                info.append(" · ").append(r.get("energy_per_tick").getAsLong() * ticks).append(" J");
            }
        }
        if (!info.isEmpty()) {
            y = text(info.toString(), 0, y + 1, w, MUTED, false);
        }
        return y;
    }

    private static String fluidText(JsonObject f) {
        String id = str(f, "fluid");
        Identifier key = Identifier.tryParse(id);
        String name = key == null ? id : Component.translatable("block." + key.getNamespace() + "." + key.getPath()).getString();
        return (f.has("amount") ? f.get("amount").getAsLong() : 0) + " mB " + name;
    }

    /** Bild aus den Ressourcen (Breite/Höhe aus den Daten, höchstens Seitenbreite). */
    private int image(JsonObject e, int y, int w) {
        Identifier tex = Identifier.tryParse(str(e, "texture"));
        if (tex == null || minecraft == null || minecraft.getResourceManager().getResource(tex).isEmpty()) {
            return y;
        }
        int iw = Math.min(w, e.has("width") ? e.get("width").getAsInt() : w);
        int ih = e.has("height") ? e.get("height").getAsInt() : iw;
        images.add(new Image(0, y, iw, ih, tex));
        return y + ih;
    }

    private record Image(int x, int y, int w, int h, Identifier texture) {
    }

    private final List<Image> images = new ArrayList<>();

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static ItemStack stackOf(String id, int count) {
        Identifier key = Identifier.tryParse(id);
        Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
        return item == null || item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item, Math.max(1, count));
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(left - 2, top - 2, left + panelW + 2, top + panelH + 2, EDGE);
        g.fill(left, top, left + panelW, top + panelH, PAPER);
        g.fill(left + TOC_W, top + 2, left + TOC_W + 1, top + panelH - 2, EDGE);

        // Inhaltsverzeichnis
        int tx = left + PAD;
        int ty = top + PAD + 18;
        int tocBottom = top + panelH - PAD;
        g.enableScissor(left, ty, left + TOC_W - 1, tocBottom);
        int yy = ty - (int) tocScroll;
        for (TocEntry e : toc) {
            if (!e.chapter() && page != null && e.pageId().equals(page.id())) {
                g.fill(tx - 2, yy - 1, left + TOC_W - 3, yy + LINE - 1, SELECTED);
            }
            Component label = e.chapter() ? e.label().copy().withStyle(ChatFormatting.BOLD) : e.label();
            g.text(font, font.substrByWidth(label, TOC_W - PAD * 2 - (e.chapter() ? 0 : 6)).getString(),
                    tx + (e.chapter() ? 0 : 6), yy, e.chapter() ? HEADING : TEXT, false);
            yy += LINE;
        }
        g.disableScissor();

        // Seite: Titel, Inhalt mit Bildlauf
        int cx = contentX();
        int cy = contentY();
        if (page != null) {
            g.text(font, Component.translatable(page.titleKey()).withStyle(ChatFormatting.BOLD), cx + 18, top + PAD + 2, HEADING, false);
        }
        int maxScroll = Math.max(0, contentH - (contentBottom() - cy));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int oy = cy - (int) scroll;
        g.enableScissor(cx, cy, cx + contentW(), contentBottom());
        for (Box b : boxes) {
            g.fill(cx + b.x1(), oy + b.y1(), cx + b.x2(), oy + b.y2(), b.color());
        }
        for (Image im : images) {
            g.blit(RenderPipelines.GUI_TEXTURED, im.texture(), cx + im.x(), oy + im.y(), 0, 0, im.w(), im.h(), im.w(), im.h());
        }
        Component tip = null;
        for (Run r : runs) {
            g.text(font, r.text(), cx + r.x(), oy + r.y(), TEXT, false);
            if (r.item() != null && inside(mouseX, mouseY, cx + r.x(), oy + r.y(), r.width(), LINE) && mouseY < contentBottom()) {
                tip = stackOf(r.item(), 1).getHoverName();
            }
        }
        for (Icon ic : icons) {
            int ix = cx + ic.x();
            int iy = oy + ic.y();
            g.item(ic.stack(), ix, iy);
            g.itemDecorations(font, ic.stack(), ix, iy, null);
            if (inside(mouseX, mouseY, ix, iy, 16, 16) && mouseY < contentBottom() && mouseY >= cy) {
                MutableComponent t = ic.stack().getHoverName().copy();
                if (ic.label() != null) {
                    t.append(Component.literal(" (" + ic.label() + ")").withStyle(ChatFormatting.GRAY));
                }
                tip = t;
            }
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(10, (contentBottom() - cy) * (contentBottom() - cy) / Math.max(1, contentH));
            int barY = cy + (int) ((contentBottom() - cy - barH) * (scroll / maxScroll));
            g.fill(left + panelW - 4, barY, left + panelW - 2, barY + barH, EDGE);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (tip != null) {
            g.setTooltipForNextFrame(tip, mouseX, mouseY);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ Eingaben

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mx = event.x();
        double my = event.y();
        // Inhaltsverzeichnis
        int ty = top + PAD + 18;
        if (inside(mx, my, left, ty, TOC_W, top + panelH - PAD - ty)) {
            int idx = (int) ((my - ty + tocScroll) / LINE);
            if (idx >= 0 && idx < toc.size() && !toc.get(idx).chapter()) {
                openPage(toc.get(idx).pageId());
                return true;
            }
            return false;
        }
        int cx = contentX();
        int cy = contentY();
        if (my < cy || my >= contentBottom()) {
            return false;
        }
        int oy = cy - (int) scroll;
        for (Run r : runs) {
            if (inside(mx, my, cx + r.x(), oy + r.y(), r.width(), LINE)) {
                if (r.link() != null) {
                    return openPage(r.link());
                }
                if (r.item() != null) {
                    return openItemPage(r.item());
                }
            }
        }
        for (Icon ic : icons) {
            if (inside(mx, my, cx + ic.x(), oy + ic.y(), 16, 16)) {
                return openItemPage(BuiltInRegistries.ITEM.getKey(ic.stack().getItem()).toString());
            }
        }
        return false;
    }

    /** Öffnet die Seite, die das Item in {@code items} nennt (24.1). */
    public boolean openItemPage(String itemId) {
        String target = book.itemPage().get(itemId);
        return target != null && openPage(target);
    }

    /** Erster Link der aktuellen Seite (für den Client-Test). */
    public @Nullable String firstLink() {
        for (Run r : runs) {
            if (r.link() != null && book.pages().containsKey(r.link())) {
                return r.link();
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx < left + TOC_W) {
            tocScroll = Math.max(0, Math.min(tocScroll - dy * SCROLL_STEP, Math.max(0, toc.size() * LINE - (panelH - 30))));
        } else {
            scroll -= dy * SCROLL_STEP;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
