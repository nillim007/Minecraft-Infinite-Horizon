package de.sternenwerft.registry;

import de.sternenwerft.platform.Services;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeBookCategory;

import java.util.function.Supplier;

/** Eigene Rezeptarten (25.4). Jede Art ist eine {@link ProcessingRecipe.Kind}. */
public final class ModRecipeTypes {

    public static final ProcessingRecipe.Kind CRUSHING = kind("crushing");
    public static final ProcessingRecipe.Kind PRESSING = kind("pressing");
    public static final ProcessingRecipe.Kind ELECTRIC_SMELTING = kind("electric_smelting");
    public static final ProcessingRecipe.Kind REFINING = kind("refining");
    public static final ProcessingRecipe.Kind ELECTROLYSIS = kind("electrolysis");
    /** Montagetisch: exakter Abgleich (22.3). */
    public static final ProcessingRecipe.Kind ASSEMBLY = kind("assembly", true);

    /** Alle eigenen Rezeptarten in Registrierreihenfolge (Kategorien der Rezeptanzeige, 3.9). */
    public static final java.util.List<ProcessingRecipe.Kind> ALL =
            java.util.List.of(CRUSHING, PRESSING, ELECTRIC_SMELTING, REFINING, ELECTROLYSIS, ASSEMBLY);

    /** Legt eine Rezeptart an und registriert Typ und Serializer unter {@code sternenwerft:<name>}. */
    public static ProcessingRecipe.Kind kind(String name) {
        return kind(name, false);
    }

    /** Wie {@link #kind(String)}; {@code exact} = exakter Abgleich wie im Montagetisch (22.3). */
    public static ProcessingRecipe.Kind kind(String name, boolean exact) {
        ProcessingRecipe.Kind k = new ProcessingRecipe.Kind(name, exact);
        Services.REGISTRY.register(BuiltInRegistries.RECIPE_TYPE, name, k::type);
        Services.REGISTRY.register(BuiltInRegistries.RECIPE_SERIALIZER, name, k::serializer);
        return k;
    }

    /** Gemeinsame Rezeptbuch-Kategorie aller Maschinenrezepte (sie erscheinen nicht im Rezeptbuch). */
    public static final Supplier<RecipeBookCategory> BOOK_CATEGORY =
            Services.REGISTRY.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, "machine", RecipeBookCategory::new);

    private ModRecipeTypes() {
    }

    public static void init() {
    }
}
