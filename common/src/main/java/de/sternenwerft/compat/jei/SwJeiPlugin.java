package de.sternenwerft.compat.jei;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.compat.ClientRecipes;
import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeView;
import de.sternenwerft.compat.RecipeViewerHooks;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.recipe.ProcessingRecipe;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI-Plugin (3.9): eine Kategorie je eigener Rezeptart, Katalysator = Maschine aus machines.json; der Elektroofen
 * erscheint zusätzlich bei den Vanilla-Schmelzrezepten. NeoForge findet es über {@link JeiPlugin}, Fabric über den
 * Entry-Point {@code jei_mod_plugin}. Wird nur geladen, wenn JEI installiert ist.
 */
@JeiPlugin
public class SwJeiPlugin implements IModPlugin {

    private final Map<String, IRecipeHolderType<ProcessingRecipe>> types = new LinkedHashMap<>();

    @Override
    public Identifier getPluginUid() {
        return Sternenwerft.id("jei");
    }

    private List<RecipeCategories.Category> categories() {
        return RecipeCategories.build(BalanceManager.getOrBundled(), ModRecipeTypes.ALL.stream().map(ProcessingRecipe.Kind::name).toList());
    }

    private IRecipeHolderType<ProcessingRecipe> type(String name) {
        return types.computeIfAbsent(name, n -> IRecipeType.create(RecipeView.kind(n).type()));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        long bucket = registration.getJeiHelpers().getPlatformFluidHelper().bucketVolume();
        for (RecipeCategories.Category c : categories()) {
            registration.addRecipeCategories(new MachineRecipeCategory(c, type(c.type()),
                    registration.getJeiHelpers().getGuiHelper(), bucket));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        int count = 0;
        for (RecipeCategories.Category c : categories()) {
            var recipes = ClientRecipes.of(RecipeView.kind(c.type()));
            registration.addRecipes(type(c.type()), recipes);
            count += recipes.size();
        }
        RecipeViewerHooks.recipeCount = count;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RecipeCategories.Category c : categories()) {
            if (!c.catalysts().isEmpty()) {
                registration.addCraftingStation(type(c.type()), RecipeView.catalystItem(c));
            }
        }
        // Vanilla-Schmelzrezepte laufen über die Vanilla-Kategorie (3.9)
        registration.addCraftingStation(RecipeTypes.SMELTING, ModBlocks.ELECTRIC_FURNACE.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RecipeViewerHooks.active = "jei";
        RecipeViewerHooks.showCategory = name -> runtime.getRecipesGui().showTypes(List.of(type(name)));
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeViewerHooks.showCategory = null;
    }
}
