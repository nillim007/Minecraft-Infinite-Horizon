package de.sternenwerft.compat.rei;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import de.sternenwerft.Sternenwerft;
import de.sternenwerft.compat.ClientRecipes;
import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeKinds;
import de.sternenwerft.compat.RecipeView;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.recipe.ProcessingRecipe;
import dev.architectury.fluid.FluidStack;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REI-Anzeige eines Maschinenrezepts (3.9). Gespeichert wird nur die Rezept-ID; der Inhalt kommt aus
 * {@link ClientRecipes} und {@link RecipeView}.
 */
public class MachineRecipeDisplay extends BasicDisplay {

    public static final DisplaySerializer<MachineRecipeDisplay> SERIALIZER = DisplaySerializer.of(
            Identifier.CODEC.fieldOf("recipe").flatXmap(MachineRecipeDisplay::byId, d -> DataResult.success(d.view.id())),
            StreamCodec.<RegistryFriendlyByteBuf, Identifier>of((buf, id) -> buf.writeIdentifier(id), buf -> buf.readIdentifier())
                    .map(id -> byId(id).getOrThrow(), d -> d.view.id()),
            false);

    private final CategoryIdentifier<MachineRecipeDisplay> category;
    private final RecipeView view;

    public MachineRecipeDisplay(CategoryIdentifier<MachineRecipeDisplay> category, RecipeView view, List<EntryIngredient> inputs,
                                List<EntryIngredient> outputs) {
        super(inputs, outputs, Optional.of(view.id()));
        this.category = category;
        this.view = view;
    }

    public RecipeView view() {
        return view;
    }

    public static CategoryIdentifier<MachineRecipeDisplay> categoryOf(String type) {
        return CategoryIdentifier.of(RecipeKinds.id(type));
    }

    private static DataResult<MachineRecipeDisplay> byId(Identifier id) {
        RecipeHolder<ProcessingRecipe> h = ClientRecipes.byId(id);
        return h == null ? DataResult.error(() -> "Rezept unbekannt: " + id) : DataResult.success(of(h));
    }

    /** Baut die Anzeige; Katalysatoren laut machines.json (für Dauer und Energie aus der Maschine). */
    public static MachineRecipeDisplay of(RecipeHolder<ProcessingRecipe> holder) {
        String type = holder.value().kind().name();
        List<String> catalysts = RecipeCategories.build(BalanceManager.getOrBundled(), List.of(type)).getFirst().catalysts();
        RecipeView v = RecipeView.of(holder, catalysts);
        List<EntryIngredient> in = new ArrayList<>();
        for (List<ItemStack> alts : v.inputs()) {
            in.add(EntryIngredients.ofItemStacks(alts));
        }
        if (v.mold() != null) {
            in.add(EntryIngredient.of(EntryStacks.of(v.mold()).tooltip(RecipeView.moldNote())));
        }
        for (SwFluidStack f : v.fluidsIn()) {
            in.add(fluid(f));
        }
        List<EntryIngredient> out = new ArrayList<>();
        for (int i = 0; i < v.outputs().size(); i++) {
            var stack = EntryStacks.of(v.outputs().get(i));
            RecipeView.chanceNote(v.chances().get(i)).ifPresent(stack::tooltip);
            out.add(EntryIngredient.of(stack));
        }
        for (SwFluidStack f : v.fluidsOut()) {
            out.add(fluid(f));
        }
        return new MachineRecipeDisplay(categoryOf(type), v, in, out);
    }

    private static EntryIngredient fluid(SwFluidStack f) {
        return EntryIngredients.of(RecipeView.fluid(f.fluid()), f.amount() * FluidStack.bucketAmount() / 1000L);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public DisplaySerializer<? extends MachineRecipeDisplay> getSerializer() {
        return SERIALIZER;
    }
}
