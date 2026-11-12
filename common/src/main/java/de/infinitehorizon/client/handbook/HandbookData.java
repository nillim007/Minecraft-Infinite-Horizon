package de.infinitehorizon.client.handbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handbuch-Daten (25.6) aus {@code assets/<ns>/handbook/**}: Kapitel ({@code _chapter.json}) und Seiten aller
 * Namespaces, damit andere Mods Kapitel und Seiten ergänzen können (HandbookLoader aus 27.3).
 */
public final class HandbookData {

    public record Chapter(String id, String titleKey, String icon, int order) {
    }

    public record Page(String id, String chapter, String titleKey, String icon, int order, List<String> items,
                       JsonArray elements) {
    }

    /** Geladenes Handbuch: Kapitel in Reihenfolge, Seiten je Kapitel, Seite je ID, Seite je Item. */
    public record Handbook(List<Chapter> chapters, Map<String, List<Page>> pagesByChapter, Map<String, Page> pages,
                           Map<String, String> itemPage) {

        public static final Handbook EMPTY = new Handbook(List.of(), Map.of(), Map.of(), Map.of());

        /** Erste Seite (für ein leeres Lesezeichen). */
        public Page firstPage() {
            for (Chapter c : chapters) {
                List<Page> ps = pagesByChapter.getOrDefault(c.id(), List.of());
                if (!ps.isEmpty()) {
                    return ps.getFirst();
                }
            }
            return null;
        }
    }

    private HandbookData() {
    }

    public static Handbook load(ResourceManager rm) {
        Map<Identifier, Resource> files = rm.listResources("handbook", id -> id.getPath().endsWith(".json"));
        List<Chapter> chapters = new ArrayList<>();
        List<Page> pages = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> e : files.entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (e.getKey().getPath().endsWith("/_chapter.json")) {
                    chapters.add(new Chapter(o.get("id").getAsString(), o.get("title").getAsString(), str(o, "icon"),
                            o.has("order") ? o.get("order").getAsInt() : 0));
                } else {
                    List<String> items = new ArrayList<>();
                    if (o.has("items") && o.get("items").isJsonArray()) {
                        for (JsonElement it : o.getAsJsonArray("items")) {
                            items.add(it.getAsString());
                        }
                    }
                    pages.add(new Page(o.get("id").getAsString(), o.get("chapter").getAsString(), o.get("title").getAsString(),
                            str(o, "icon"), o.has("order") ? o.get("order").getAsInt() : 0, List.copyOf(items),
                            o.has("elements") ? o.getAsJsonArray("elements") : new JsonArray()));
                }
            } catch (Exception ex) {
                InfiniteHorizon.LOG.warn("Handbuchdatei {} nicht lesbar: {}", e.getKey(), ex.getMessage());
            }
        }
        chapters.sort(Comparator.comparingInt(Chapter::order).thenComparing(Chapter::id));
        pages.sort(Comparator.comparingInt(Page::order).thenComparing(Page::id));
        Map<String, List<Page>> byChapter = new LinkedHashMap<>();
        Map<String, Page> byId = new LinkedHashMap<>();
        Map<String, String> itemPage = new LinkedHashMap<>();
        for (Page p : pages) {
            byChapter.computeIfAbsent(p.chapter(), k -> new ArrayList<>()).add(p);
            byId.put(p.id(), p);
            for (String item : p.items()) {
                itemPage.putIfAbsent(item, p.id());
            }
        }
        return new Handbook(List.copyOf(chapters), byChapter, byId, itemPage);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }
}
