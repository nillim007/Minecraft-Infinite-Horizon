package de.sternenwerft.registry;

import de.sternenwerft.platform.Services;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

/** Serializer der eigenen Rezeptarten (gleiche ID wie der Typ). */
public final class ModRecipeSerializers {

    public static final Supplier<RecipeSerializer<ProcessingRecipe>> CRUSHING =
            Services.REGISTRY.register(BuiltInRegistries.RECIPE_SERIALIZER, ModRecipeTypes.CRUSHING.name(),
                    ModRecipeTypes.CRUSHING::serializer);

    private ModRecipeSerializers() {
    }

    public static void init() {
    }
}
