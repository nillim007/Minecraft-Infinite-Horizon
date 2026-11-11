package de.sternenwerft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sternenwerft.Sternenwerft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Serverkonfiguration {@code <welt>/serverconfig/sternenwerft.json} (24.4). Schlüssel, Standardwerte und Grenzen
 * kommen aus {@code misc.json → config_defaults}; neue Schlüssel brauchen deshalb keinen Code.
 * Ungültige Werte werden auf den Bereich begrenzt bzw. durch den Standard ersetzt.
 */
public final class ServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> META = Set.of("file", "spec_ref", "notes");

    private static volatile ServerConfig server = new ServerConfig(new JsonObject());
    private static volatile ServerConfig client = new ServerConfig(new JsonObject());

    private final JsonObject values;

    private ServerConfig(JsonObject values) {
        this.values = values;
    }

    /** Wirksame Werte auf dem Server. */
    public static ServerConfig get() {
        return server;
    }

    /** Vom Server empfangene Werte (nur Anzeige auf dem Client). */
    public static ServerConfig client() {
        return client;
    }

    public static void setClient(String json) {
        client = new ServerConfig(JsonParser.parseString(json).getAsJsonObject());
    }

    public String toJson() {
        return values.toString();
    }

    public boolean getBoolean(String key) {
        return values.has(key) && values.get(key).getAsBoolean();
    }

    public double getDouble(String key) {
        return values.has(key) ? values.get(key).getAsDouble() : 0.0;
    }

    public String getString(String key) {
        return values.has(key) ? values.get(key).getAsString() : "";
    }

    /** Lädt bzw. legt die Datei an; schreibt sie mit allen bekannten Schlüsseln zurück. */
    public static ServerConfig load(Path file) {
        JsonObject defaults = BalanceManager.get().entry("misc", "config_defaults").orElse(new JsonObject());
        JsonObject stored = new JsonObject();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                stored = JsonParser.parseReader(r).getAsJsonObject();
            } catch (IOException | RuntimeException e) {
                Sternenwerft.LOG.error("Config {} nicht lesbar, nutze Standardwerte: {}", file, e.getMessage());
            }
        }
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> e : defaults.entrySet()) {
            if (META.contains(e.getKey()) || !e.getValue().isJsonObject()) {
                continue;
            }
            result.add(e.getKey(), validate(e.getValue().getAsJsonObject(), stored.get(e.getKey())));
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(result, w);
            }
        } catch (IOException e) {
            Sternenwerft.LOG.error("Config {} nicht schreibbar: {}", file, e.getMessage());
        }
        server = new ServerConfig(result);
        return server;
    }

    /** Reine Funktion: prüft einen Wert gegen {@code {default, min?, max?, values?}}. */
    public static JsonElement validate(JsonObject rule, JsonElement value) {
        JsonElement def = rule.get("default");
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return def;
        }
        var prim = value.getAsJsonPrimitive();
        var defPrim = def.getAsJsonPrimitive();
        if (defPrim.isBoolean()) {
            return prim.isBoolean() ? prim : def;
        }
        if (defPrim.isNumber()) {
            if (!prim.isNumber()) {
                return def;
            }
            double v = prim.getAsDouble();
            if (rule.has("min")) {
                v = Math.max(v, rule.get("min").getAsDouble());
            }
            if (rule.has("max")) {
                v = Math.min(v, rule.get("max").getAsDouble());
            }
            return new com.google.gson.JsonPrimitive(v);
        }
        if (rule.has("values")) {
            for (JsonElement allowed : rule.getAsJsonArray("values")) {
                if (allowed.getAsString().equals(prim.getAsString())) {
                    return prim;
                }
            }
            return def;
        }
        return prim;
    }
}
