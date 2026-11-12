package de.infinitehorizon.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Liest Listen, die {@code tools/gen_resources.py} fuer den Code erzeugt ({@code infinite_horizon/*.json} im Mod-Jar),
 * z. B. einfache Items, einfache Bloecke, Biom-Ergaenzungen. So steht keine ID-Liste doppelt im Java-Code.
 */
public final class GeneratedData {

    private GeneratedData() {
    }

    /** Laedt {@code infinite_horizon/<name>.json}; leeres Objekt, wenn die Datei fehlt. */
    public static JsonObject read(String name) {
        String path = "/" + InfiniteHorizon.MOD_ID + "/" + name + ".json";
        try (InputStream in = GeneratedData.class.getResourceAsStream(path)) {
            if (in == null) {
                InfiniteHorizon.LOG.error("{} fehlt - gen_resources.py ausfuehren", path);
                return new JsonObject();
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r).getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            InfiniteHorizon.LOG.error("{} nicht lesbar: {}", path, e.getMessage());
            return new JsonObject();
        }
    }

    /** Wie {@link #read(String)}, liefert aber das Array {@code key} (leer, wenn es fehlt). */
    public static com.google.gson.JsonArray array(String name, String key) {
        JsonObject o = read(name);
        return o.has(key) ? o.getAsJsonArray(key) : new com.google.gson.JsonArray();
    }
}
