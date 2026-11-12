package de.infinitehorizon.compat.jei;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.compat.ClientRecipes;
import de.infinitehorizon.compat.RecipeCategories;
import de.infinitehorizon.compat.RecipeKinds;
import de.infinitehorizon.compat.RecipeView;
import de.infinitehorizon.compat.RecipeViewerHooks;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.recipe.ProcessingRecipe;
import de.infinitehorizon.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI-Plugin (3.9): eine Kategorie je eigener Rezeptart, Katalysator = Maschine aus machines.json; der Elektroofen
 * erscheint zusätzlich bei den Vanilla-Schmelzrezepten. NeoForge findet es über {@link JeiPlugin}, Fabric über den
 * Entry-Point {@code jei_mod_plugin}. Wird nur geladen, wenn JEI installiert ist.
 */
@JeiPlugin
public class IhJeiPlugin implements IModPlugin {

    private final Map<String, IRecipeHolderType<ProcessingRecipe>> types = new LinkedHashMap<>();

    @Override
    public Identifier getPluginUid() {
        return InfiniteHorizon.id("jei");
    }

    private List<RecipeCategories.Category> categories() {
        return RecipeCategories.build(BalanceManager.getOrBundled(), RecipeKinds.all().stream().map(ProcessingRecipe.Kind::name).toList());
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
        // Taste H über einem Item in der Liste, den Lesezeichen oder einem Rezept (24.1)
        RecipeViewerHooks.hoveredStack = () -> {
            ItemStack s = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            if (s == null) {
                s = runtime.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
            }
            return s != null ? s : runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        };
        RecipeViewerHooks.typing = () -> runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeViewerHooks.showCategory = null;
        RecipeViewerHooks.hoveredStack = null;
        RecipeViewerHooks.typing = null;
    }
}
