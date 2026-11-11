package de.sternenwerft.compat;

import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Maschinenrezepte auf dem Client (für JEI/REI, 3.9). Seit 1.21.2 kennt der Client nicht alle Rezepte; die eigenen
 * Rezeptarten schickt der Server deshalb gezielt (NeoForge {@code OnDatapackSyncEvent#sendRecipes}, Fabric
 * {@code RecipeSynchronization}). Der Loader setzt hier die Quelle, die Anzeige-Plugins lesen nur.
 */
public final class ClientRecipes {

    private static volatile Function<RecipeType<ProcessingRecipe>, Collection<RecipeHolder<ProcessingRecipe>>> source =
            t -> List.of();

    private ClientRecipes() {
    }

    public static void setSource(Function<RecipeType<ProcessingRecipe>, Collection<RecipeHolder<ProcessingRecipe>>> s) {
        source = s;
    }

    /** Alle Rezepte einer Art, wie der Server sie geschickt hat. */
    public static List<RecipeHolder<ProcessingRecipe>> of(ProcessingRecipe.Kind kind) {
        try {
            return List.copyOf(source.apply(kind.type()));
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** Rezept nach ID über alle eigenen Arten (für gespeicherte Anzeigen). */
    public static @Nullable RecipeHolder<ProcessingRecipe> byId(Identifier id) {
        for (ProcessingRecipe.Kind kind : RecipeKinds.all()) {
            for (RecipeHolder<ProcessingRecipe> h : of(kind)) {
                if (h.id().identifier().equals(id)) {
                    return h;
                }
            }
        }
        return null;
    }

    /** Alle eigenen Rezeptarten als Vanilla-Typen (zum Synchronisieren). */
    public static List<RecipeType<?>> types() {
        List<RecipeType<?>> out = new ArrayList<>();
        for (ProcessingRecipe.Kind k : RecipeKinds.all()) {
            out.add(k.type());
        }
        return out;
    }
}
