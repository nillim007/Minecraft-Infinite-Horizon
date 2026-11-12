package de.infinitehorizon.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Darstellung der Planeten auf dem Client (25.1, 13.5): feste Himmelskörper ({@code sky.celestial_bodies}) und ob
 * Monster eine Atemmaske tragen. Daten aus dem {@code SyncDataPayload} ({@link PlanetRegistry#client()}).
 */
public final class PlanetVisuals {

    /**
     * Ein Himmelskörper: Sprite im Atlas {@code celestials}, Größe in der Einheit der Vanilla-Sonne (Sonne = 30),
     * Azimut in Grad ab Norden im Uhrzeigersinn, Höhe in Grad über dem Horizont.
     */
    public record Body(Identifier sprite, float size, float azimuth, float elevation) {
    }

    private static @Nullable PlanetRegistry cachedFor;
    private static @Nullable Identifier cachedDim;
    private static List<Body> cached = List.of();

    private PlanetVisuals() {
    }

    private static @Nullable Planet currentPlanet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return PlanetRegistry.client().planet(mc.level.dimension().identifier()).orElse(null);
    }

    /** Feste Himmelskörper der aktuellen Dimension (leer ohne Planeten-Eintrag). */
    public static List<Body> bodies() {
        Minecraft mc = Minecraft.getInstance();
        Identifier dim = mc.level == null ? null : mc.level.dimension().identifier();
        PlanetRegistry reg = PlanetRegistry.client();
        if (reg == cachedFor && dim != null && dim.equals(cachedDim)) {
            return cached;
        }
        cachedFor = reg;
        cachedDim = dim;
        cached = parse(currentPlanet());
        return cached;
    }

    static List<Body> parse(@Nullable Planet planet) {
        if (planet == null || !planet.raw().has("sky") || !planet.raw().get("sky").isJsonObject()) {
            return List.of();
        }
        JsonObject sky = planet.raw().getAsJsonObject("sky");
        JsonElement list = sky.get("celestial_bodies");
        if (list == null || !list.isJsonArray()) {
            return List.of();
        }
        List<Body> out = new ArrayList<>();
        for (JsonElement e : list.getAsJsonArray()) {
            JsonObject b = e.getAsJsonObject();
            out.add(new Body(sprite(Identifier.parse(b.get("texture").getAsString())), b.get("size").getAsFloat(),
                    b.get("azimuth").getAsFloat(), b.get("elevation").getAsFloat()));
        }
        return List.copyOf(out);
    }

    /**
     * Sprite-ID im Atlas {@code celestials} zu einer Texturangabe aus 25.1: Namespace + Dateiname ohne Endung
     * ({@code infinite_horizon:textures/sky/earth.png} → {@code infinite_horizon:earth}). Die Datei selbst liegt unter
     * {@code textures/environment/celestial/}, dem Ordner des Atlas. SPEC-GAP #52.
     */
    static Identifier sprite(Identifier texture) {
        String path = texture.getPath();
        String file = path.substring(path.lastIndexOf('/') + 1);
        if (file.endsWith(".png")) {
            file = file.substring(0, file.length() - ".png".length());
        }
        return Identifier.fromNamespaceAndPath(texture.getNamespace(), file);
    }

    /** Monster tragen Atemmasken in jeder Dimension mit Planeten-Eintrag außer der Overworld (13.5). */
    public static boolean masksActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && !mc.level.dimension().identifier().equals(PlanetResolver.EARTH) && currentPlanet() != null;
    }
}
