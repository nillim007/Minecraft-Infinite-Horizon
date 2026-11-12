package de.infinitehorizon.compat;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.recipe.ProcessingRecipe;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Rezeptarten, die in JEI/REI erscheinen und deren Rezepte zum Client geschickt werden (3.9). Unsere Arten trägt
 * {@code ModRecipeTypes} ein, Addons rufen {@code InfiniteHorizonApi.showInRecipeViewers(kind)}. Name einer Addon-Art mit
 * Namespace ({@code meinaddon:mill}); ohne Namespace gilt {@code infinite_horizon}.
 */
public final class RecipeKinds {

    private static final List<ProcessingRecipe.Kind> KINDS = new CopyOnWriteArrayList<>();

    private RecipeKinds() {
    }

    public static void register(ProcessingRecipe.Kind kind) {
        if (!KINDS.contains(kind)) {
            KINDS.add(kind);
            Services.REGISTRY.syncRecipesToClient(kind.serializer());
        }
    }

    public static List<ProcessingRecipe.Kind> all() {
        return List.copyOf(KINDS);
    }

    /** Volle ID einer Rezeptart aus ihrem Namen ({@code crushing} → {@code infinite_horizon:crushing}). */
    public static Identifier id(String name) {
        return name.contains(":") ? Identifier.parse(name) : InfiniteHorizon.id(name);
    }

    /** Art nach Namen; IllegalArgumentException, wenn sie nicht eingetragen ist. */
    public static ProcessingRecipe.Kind byName(String name) {
        for (ProcessingRecipe.Kind k : KINDS) {
            if (k.name().equals(name)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Rezeptart nicht eingetragen: " + name);
    }
}
