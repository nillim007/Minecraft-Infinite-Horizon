package de.sternenwerft.compat.rei;

import de.sternenwerft.compat.ClientRecipes;
import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeKinds;
import de.sternenwerft.compat.RecipeView;
import de.sternenwerft.compat.RecipeViewerHooks;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.recipe.ProcessingRecipe;
import de.sternenwerft.registry.ModBlocks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * REI-Plugin für den Client (3.9): Kategorien, Werkbänke (Maschinen) und Anzeigen aus {@link ClientRecipes}.
 * Fabric: Entry-Point {@code rei_client}; NeoForge: Unterklasse mit {@code @REIPluginClient}. Wird nur geladen, wenn REI
 * installiert ist.
 */
public class SwReiClientPlugin implements REIClientPlugin {

    /** REI-Kategorie der Vanilla-Schmelzrezepte (Standard-Plugin von REI). */
    private static final CategoryIdentifier<?> VANILLA_SMELTING = CategoryIdentifier.of("minecraft", "plugins/smelting");

    private static List<RecipeCategories.Category> categories() {
        return RecipeCategories.build(BalanceManager.getOrBundled(), RecipeKinds.all().stream().map(ProcessingRecipe.Kind::name).toList());
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (RecipeCategories.Category c : categories()) {
            registry.add(new MachineDisplayCategory(c));
            if (!c.catalysts().isEmpty()) {
                registry.addWorkstations(MachineRecipeDisplay.categoryOf(c.type()), EntryStacks.of(RecipeView.catalystItem(c)));
            }
        }
        try {
            // Vanilla-Schmelzrezepte laufen über die Vanilla-Kategorie (3.9); fehlt das REI-Standard-Plugin, entfällt das
            registry.addWorkstations(VANILLA_SMELTING, EntryStacks.of(ModBlocks.ELECTRIC_FURNACE.get()));
        } catch (RuntimeException e) {
            // keine Vanilla-Kategorie vorhanden
        }
        RecipeViewerHooks.active = "rei";
        RecipeViewerHooks.showCategory = name -> ViewSearchBuilder.builder().addCategory(MachineRecipeDisplay.categoryOf(name)).open();
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        int count = 0;
        for (ProcessingRecipe.Kind kind : RecipeKinds.all()) {
            for (RecipeHolder<ProcessingRecipe> h : ClientRecipes.of(kind)) {
                registry.add(MachineRecipeDisplay.of(h));
                count++;
            }
        }
        RecipeViewerHooks.recipeCount = count;
    }
}
