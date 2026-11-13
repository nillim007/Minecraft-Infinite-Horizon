package de.infinitehorizon.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.network.AgencyStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Planetenatlas (24.2): links die Ziele mit Dimension nach Ring, rechts zum gewählten Ziel Status, Entdecker, ab
 * „Sonde angekommen“ die Umweltwerte, besuchte Biome, Erkundungsziele und Landefeuer. Daten aus
 * {@link AgencyStatePayload#atlas()}; aus Rechtecken gezeichnet wie das Maschinen-GUI.
 */
public class AtlasScreen extends Screen {

    private static final int W = 320;
    private static final int H = 200;
    private static final int PAD = 8;
    private static final int LINE = 10;
    private static final int ROW = 16;
    private static final int LIST_W = 96;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;
    private static final int DIM = 0xFF707070;

    private final List<Planet> planets = new ArrayList<>();
    private @Nullable Identifier selected;
    private int left;
    private int top;

    public AtlasScreen() {
        super(Component.translatable("item." + InfiniteHorizon.MOD_ID + ".planetary_atlas"));
    }

    public static void open() {
        Minecraft.getInstance().setScreenAndShow(new AtlasScreen());
    }

    /** Ziel wählen (auch für Tests). */
    public void select(Identifier dimension) {
        selected = dimension;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        planets.clear();
        var connection = Minecraft.getInstance().getConnection();
        for (Planet p : PlanetRegistry.client().all()) {
            if (connection == null || connection.levels().contains(ResourceKey.create(Registries.DIMENSION, p.dimension()))) {
                planets.add(p);
            }
        }
        planets.sort(Comparator.comparingInt(Planet::ring).thenComparing(p -> p.dimension().toString()));
        int y = top + PAD + 2 * LINE;
        for (Planet p : planets) {
            addRenderableWidget(Button.builder(planetName(p.dimension()), b -> select(p.dimension()))
                    .bounds(left + PAD, y, LIST_W, ROW - 2).build());
            y += ROW;
        }
        if (selected == null && !planets.isEmpty()) {
            selected = planets.getFirst().dimension();
        }
    }

    private static Component planetName(Identifier dim) {
        return Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath());
    }

    private static JsonObject atlas() {
        AgencyStatePayload s = AgencyClientState.get();
        try {
            return s == null ? new JsonObject() : JsonParser.parseString(s.atlas()).getAsJsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private static boolean contains(JsonObject o, String key, String value) {
        if (!o.has(key)) {
            return false;
        }
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (e.getAsString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, PANEL);
        g.text(font, title, left + PAD, top + PAD, TEXT, false);
        if (selected == null) {
            return;
        }
        JsonObject a = atlas();
        String dim = selected.toString();
        int x = left + 2 * PAD + LIST_W;
        int y = top + PAD + 2 * LINE;
        int maxW = W - (x - left) - PAD;
        g.text(font, planetName(selected), x, y, TEXT, false);
        y += LINE + 2;
        boolean visited = contains(a, "visited", dim);
        boolean probe = contains(a, "probe", dim);
        String status = visited ? "visited" : probe ? "probe" : "unknown";
        g.text(font, Component.translatable("gui.infinite_horizon.atlas.status." + status), x, y, TEXT, false);
        y += LINE;
        JsonObject disc = a.has("discoverers") ? a.getAsJsonObject("discoverers") : new JsonObject();
        Component discoverer = disc.has(dim) ? Component.literal(disc.get(dim).getAsString())
                : Component.translatable("gui.infinite_horizon.atlas.none");
        g.text(font, Component.translatable("gui.infinite_horizon.atlas.discoverer", discoverer), x, y, TEXT, false);
        y += LINE + 2;
        Planet p = PlanetRegistry.client().planet(selected).orElse(null);
        if (p != null && (visited || probe)) {
            g.text(font, Component.translatable("gui.infinite_horizon.atlas.values",
                    String.format(Locale.ROOT, "%.2f", p.gravity()),
                    String.format(Locale.ROOT, "%.0f", p.temperatureDay()), String.format(Locale.ROOT, "%.0f", p.temperatureNight()),
                    Component.translatableWithFallback("gui.infinite_horizon.atlas.pressure." + p.pressure().name().toLowerCase(Locale.ROOT),
                            p.pressure().name().toLowerCase(Locale.ROOT)), p.radiation()), x, y, TEXT, false);
            y += LINE + 2;
        }
        y = list(g, a.has("biomes") && a.getAsJsonObject("biomes").has(dim) ? a.getAsJsonObject("biomes").getAsJsonArray(dim)
                : new JsonArray(), "biomes", x, y, maxW, e -> {
                    Identifier id = Identifier.parse(e.getAsString());
                    return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
                });
        y = list(g, filter(a, "structures", dim), "structures", x, y, maxW, e -> {
            JsonObject s = e.getAsJsonObject();
            Identifier id = Identifier.parse(s.get("id").getAsString());
            return Component.translatable("gui.infinite_horizon.atlas.at",
                    Component.translatable("structure." + id.getNamespace() + "." + id.getPath()), s.get("x").getAsInt(), s.get("z").getAsInt());
        });
        list(g, filter(a, "beacons", dim), "beacons", x, y, maxW, e -> {
            JsonObject s = e.getAsJsonObject();
            String name = s.get("name").getAsString();
            Component n = name.isEmpty() ? Component.translatable("gui.infinite_horizon.beacon.default_name",
                    s.get("x").getAsInt(), s.get("z").getAsInt()) : Component.literal(name);
            return Component.translatable("gui.infinite_horizon.atlas.at", n, s.get("x").getAsInt(), s.get("z").getAsInt());
        });
    }

    private static JsonArray filter(JsonObject a, String key, String dim) {
        JsonArray out = new JsonArray();
        if (a.has(key)) {
            for (JsonElement e : a.getAsJsonArray(key)) {
                if (e.getAsJsonObject().get("dimension").getAsString().equals(dim)) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    private int list(GuiGraphicsExtractor g, JsonArray entries, String key, int x, int y, int maxW,
                     java.util.function.Function<JsonElement, Component> label) {
        if (entries.isEmpty() || y > top + H - PAD - LINE) {
            return y;
        }
        g.text(font, Component.translatable("gui.infinite_horizon.atlas." + key), x, y, TEXT, false);
        y += LINE;
        for (JsonElement e : entries) {
            if (y > top + H - PAD - LINE) {
                break;
            }
            g.text(font, font.substrByWidth(label.apply(e), maxW).getString(), x + PAD, y, DIM, false);
            y += LINE;
        }
        return y + 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
