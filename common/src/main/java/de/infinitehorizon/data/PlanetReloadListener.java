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
 * Lädt {@code data/<ns>/infinite_horizon/planet/*.json} bei Serverstart und {@code /reload} (13.1). Reihenfolge wie
 * bei der Balance: zuerst {@code infinite_horizon}, dann alle anderen Namespaces alphabetisch (Addons ergänzen oder
 * überschreiben, {@link PlanetRegistry#merge}).
 */
public final class PlanetReloadListener implements ResourceManagerReloadListener {

    public static final Identifier ID = InfiniteHorizon.id("planets");

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Comparator<Identifier> order = Comparator
                .comparing((Identifier id) -> !id.getNamespace().equals(InfiniteHorizon.MOD_ID))
                .thenComparing(Identifier::getNamespace)
                .thenComparing(Identifier::getPath);
        Map<Identifier, Resource> found = new TreeMap<>(order);
        found.putAll(manager.listResources(PlanetRegistry.DIRECTORY, id -> id.getPath().endsWith(".json")));

        Map<Identifier, JsonObject> entries = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                PlanetRegistry.merge(entries, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException ex) {
                InfiniteHorizon.LOG.error("Planet-Datei {} nicht lesbar: {}", e.getKey(), ex.getMessage());
            }
        }
        PlanetRegistry.set(new PlanetRegistry(entries));
        InfiniteHorizon.LOG.info("Planeten geladen: {}", entries.keySet());
    }
}
