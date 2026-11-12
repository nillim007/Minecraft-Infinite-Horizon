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

/**
 * Lädt {@code data/<ns>/infinite_horizon/research/*.json} und {@code research_tier/*.json} bei Serverstart und
 * {@code /reload} (11.4, 25.3). Reihenfolge wie bei den Planeten: zuerst {@code infinite_horizon}, dann alle anderen
 * Namespaces alphabetisch ({@link ResearchRegistry#mergeNode}).
 */
public final class ResearchReloadListener implements ResourceManagerReloadListener {

    public static final Identifier ID = InfiniteHorizon.id("research");

    private static final Comparator<Identifier> ORDER = Comparator
            .comparing((Identifier id) -> !id.getNamespace().equals(InfiniteHorizon.MOD_ID))
            .thenComparing(Identifier::getNamespace)
            .thenComparing(Identifier::getPath);

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Integer, JsonObject> tiers = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> e : list(manager, ResearchRegistry.TIER_DIRECTORY).entrySet()) {
            JsonObject o = read(e.getKey(), e.getValue());
            if (o != null) {
                ResearchRegistry.mergeTier(tiers, o);
            }
        }
        Map<Identifier, JsonObject> nodes = new LinkedHashMap<>();
        String prefix = ResearchRegistry.NODE_DIRECTORY + "/";
        for (Map.Entry<Identifier, Resource> e : list(manager, ResearchRegistry.NODE_DIRECTORY).entrySet()) {
            JsonObject o = read(e.getKey(), e.getValue());
            if (o != null) {
                String path = e.getKey().getPath();
                path = path.substring(prefix.length(), path.length() - ".json".length());
                ResearchRegistry.mergeNode(nodes, Identifier.fromNamespaceAndPath(e.getKey().getNamespace(), path), o);
            }
        }
        ResearchRegistry.set(new ResearchRegistry(nodes, tiers));
        InfiniteHorizon.LOG.info("Forschung geladen: {} Stufen, {} Knoten", tiers.size(), nodes.size());
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
            InfiniteHorizon.LOG.error("Forschungs-Datei {} nicht lesbar: {}", file, ex.getMessage());
            return null;
        }
    }
}
