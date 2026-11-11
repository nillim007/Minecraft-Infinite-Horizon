package de.sternenwerft.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.sternenwerft.Sternenwerft;
import de.sternenwerft.data.BalanceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Kategorien der Rezeptanzeige (JEI/REI, 3.9) als reine Rechnung: je eigener Rezeptart eine Kategorie mit Titel
 * {@code jei.sternenwerft.<art>} und den Maschinen als Katalysator, die die Art in machines.json unter
 * {@code recipe_types} führen. Die Plugins der Anzeige-Mods bauen nur noch die Darstellung darum.
 */
public final class RecipeCategories {

    /** Eine Kategorie: Rezeptart (ohne Namespace), Übersetzungsschlüssel, Katalysator-Blöcke (IDs). */
    public record Category(String type, String titleKey, List<String> catalysts) {
    }

    private RecipeCategories() {
    }

    /** Baut die Kategorien für die Rezeptarten {@code types} (z. B. {@code crushing}) in deren Reihenfolge. */
    public static List<Category> build(BalanceManager balance, Collection<String> types) {
        List<Category> out = new ArrayList<>();
        JsonObject machines = balance.entries("machines").orElse(new JsonObject());
        for (String type : types) {
            String full = Sternenwerft.MOD_ID + ":" + type;
            List<String> catalysts = new ArrayList<>();
            for (Map.Entry<String, JsonElement> e : machines.entrySet()) {
                if (!e.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject m = e.getValue().getAsJsonObject();
                if (m.has("recipe_types") && m.get("recipe_types").isJsonArray()) {
                    for (JsonElement t : m.getAsJsonArray("recipe_types")) {
                        if (full.equals(t.getAsString())) {
                            catalysts.add(e.getKey());
                        }
                    }
                }
            }
            out.add(new Category(type, "jei." + Sternenwerft.MOD_ID + "." + type, List.copyOf(catalysts)));
        }
        return out;
    }
}
