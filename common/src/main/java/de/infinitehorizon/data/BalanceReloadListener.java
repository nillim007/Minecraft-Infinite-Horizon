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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lädt {@code data/<ns>/infinite_horizon/balance/*.json} bei Serverstart und {@code /reload} (25.7).
 * Zuerst die Dateien von {@code infinite_horizon}, danach alle anderen Namespaces alphabetisch; deren Einträge
 * ergänzen oder überschreiben unsere ({@link BalanceManager#merge}). So liefern Addons eigene Werte, ohne
 * unsere Dateien zu ersetzen.
 */
public final class BalanceReloadListener implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(InfiniteHorizon.MOD_ID, "balance");
    public static final String DIRECTORY = InfiniteHorizon.MOD_ID + "/balance";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Comparator<Identifier> order = Comparator
                .comparing((Identifier id) -> !id.getNamespace().equals(InfiniteHorizon.MOD_ID))
                .thenComparing(Identifier::getNamespace)
                .thenComparing(Identifier::getPath);
        Map<Identifier, Resource> found = new TreeMap<>(order);
        found.putAll(manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")));

        Map<String, JsonObject> files = new HashMap<>();
        for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
            String path = e.getKey().getPath();
            String name = path.substring(DIRECTORY.length() + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                BalanceManager.merge(files, name, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException ex) {
                InfiniteHorizon.LOG.error("Balance-Datei {} nicht lesbar: {}", e.getKey(), ex.getMessage());
            }
        }
        BalanceManager.set(new BalanceManager(files));
        InfiniteHorizon.LOG.info("Balance geladen: {} Dateien aus {} Quellen", files.size(), found.size());
    }
}
