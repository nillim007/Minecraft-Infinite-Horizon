package de.sternenwerft.registry;

import de.sternenwerft.platform.Services;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeBookCategory;

import java.util.function.Supplier;

/** Eigene Rezeptarten (25.4). Jede Art ist eine {@link ProcessingRecipe.Kind}. */
public final class ModRecipeTypes {

    public static final ProcessingRecipe.Kind CRUSHING = new ProcessingRecipe.Kind("crushing");

    static {
        Services.REGISTRY.register(BuiltInRegistries.RECIPE_TYPE, CRUSHING.name(), CRUSHING::type);
    }

    /** Gemeinsame Rezeptbuch-Kategorie aller Maschinenrezepte (sie erscheinen nicht im Rezeptbuch). */
    public static final Supplier<RecipeBookCategory> BOOK_CATEGORY =
            Services.REGISTRY.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, "machine", RecipeBookCategory::new);

    private ModRecipeTypes() {
    }

    public static void init() {
    }
}
