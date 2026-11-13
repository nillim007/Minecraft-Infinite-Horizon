package de.infinitehorizon.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Lädt {@code rocket_part/*.json} und {@code rocket_rules/*.json} bei Serverstart und {@code /reload} (12.4, 25.5). */
public final class RocketPartReloadListener implements ResourceManagerReloadListener {

    public static final Identifier ID = InfiniteHorizon.id("rocket_parts");

    private static final Comparator<Identifier> ORDER = Comparator
            .comparing((Identifier id) -> !id.getNamespace().equals(InfiniteHorizon.MOD_ID))
            .thenComparing(Identifier::getNamespace)
            .thenComparing(Identifier::getPath);

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, JsonObject> parts = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> e : list(manager, RocketPartRegistry.PART_DIRECTORY).entrySet()) {
            JsonObject o = read(e.getKey(), e.getValue());
            if (o != null) {
                RocketPartRegistry.mergePart(parts, o);
            }
        }
        Map<Integer, JsonObject> rules = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> e : list(manager, RocketPartRegistry.RULE_DIRECTORY).entrySet()) {
            JsonObject o = read(e.getKey(), e.getValue());
            if (o != null) {
                RocketPartRegistry.mergeRule(rules, o);
            }
        }
        RocketPartRegistry.set(new RocketPartRegistry(parts, rules));
        InfiniteHorizon.LOG.info("Raketenteile geladen: {} Teile, {} Stufen", parts.size(), rules.size());
    }

    private static Map<Identifier, Resource> list(ResourceManager manager, String directory) {
        Map<Identifier, Resource> found = new TreeMap<>(ORDER);
        found.putAll(manager.listResources(directory, id -> id.getPath().endsWith(".json")));
        return found;
    }

    private static JsonObject read(Identifier file, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | RuntimeException ex) {
            InfiniteHorizon.LOG.error("Raketen-Datei {} nicht lesbar: {}", file, ex.getMessage());
            return null;
        }
    }
}
