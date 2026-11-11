package de.sternenwerft.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sternenwerft.Sternenwerft;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Liest Listen, die {@code tools/gen_resources.py} fuer den Code erzeugt ({@code sternenwerft/*.json} im Mod-Jar),
 * z. B. einfache Items, einfache Bloecke, Biom-Ergaenzungen. So steht keine ID-Liste doppelt im Java-Code.
 */
public final class GeneratedData {

    private GeneratedData() {
    }

    /** Laedt {@code sternenwerft/<name>.json}; leeres Objekt, wenn die Datei fehlt. */
    public static JsonObject read(String name) {
        String path = "/" + Sternenwerft.MOD_ID + "/" + name + ".json";
        try (InputStream in = GeneratedData.class.getResourceAsStream(path)) {
            if (in == null) {
                Sternenwerft.LOG.error("{} fehlt - gen_resources.py ausfuehren", path);
                return new JsonObject();
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(r).getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            Sternenwerft.LOG.error("{} nicht lesbar: {}", path, e.getMessage());
            return new JsonObject();
        }
    }

    /** Wie {@link #read(String)}, liefert aber das Array {@code key} (leer, wenn es fehlt). */
    public static com.google.gson.JsonArray array(String name, String key) {
        JsonObject o = read(name);
        return o.has(key) ? o.getAsJsonArray(key) : new com.google.gson.JsonArray();
    }
}
