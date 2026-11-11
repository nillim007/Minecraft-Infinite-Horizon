package de.sternenwerft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.sternenwerft.registry.ModRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Rezept einer Verarbeitungsmaschine mit einem Eingang (Format 25.4):
 * {@code {"type": "<ns>:<art>", "ingredients": [{"ingredient": …, "count": n}], "results": [{"id", "count", "chance"}],
 * "ticks"?: n, "energy_per_tick"?: n}}. Fehlen {@code ticks}/{@code energy_per_tick}, gelten die Werte der Maschine.
 *
 * <p>Eine neue Rezeptart braucht nur eine {@link Kind}; Addons registrieren deren {@code type()} und
 * {@code serializer()} unter eigener ID.
 */
public final class ProcessingRecipe implements Recipe<MachineRecipeInput> {

    public record CountedIngredient(Ingredient ingredient, int count) {
        public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(i, CountedIngredient::new));
    }

    public record ChanceResult(Item item, int count, float chance) {
        public static final Codec<ChanceResult> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ChanceResult::item),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ChanceResult::count),
                Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(ChanceResult::chance)
        ).apply(i, ChanceResult::new));

        public ItemStack toStack() {
            return new ItemStack(item, count);
        }
    }

    /** Eine Rezeptart (z. B. {@code crushing}) mit eigenem RecipeType und Serializer. */
    public static final class Kind {
        private final String name;
        private final RecipeType<ProcessingRecipe> type;
        private final RecipeSerializer<ProcessingRecipe> serializer;

        public Kind(String name) {
            this.name = name;
            this.type = new RecipeType<>() {
                @Override
                public String toString() {
                    return name;
                }
            };
            MapCodec<ProcessingRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(ProcessingRecipe::ingredients),
                    ChanceResult.CODEC.listOf().fieldOf("results").forGetter(ProcessingRecipe::results),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("ticks").forGetter(ProcessingRecipe::ticks),
                    Codec.LONG.optionalFieldOf("energy_per_tick").forGetter(ProcessingRecipe::energyPerTick),
                    BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("mold").forGetter(ProcessingRecipe::mold)
            ).apply(i, (ing, res, t, e, m) -> new ProcessingRecipe(this, ing, res, t, e, m)));
            this.serializer = new RecipeSerializer<>(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
        }

        public String name() {
            return name;
        }

        public RecipeType<ProcessingRecipe> type() {
            return type;
        }

        public RecipeSerializer<ProcessingRecipe> serializer() {
            return serializer;
        }
    }

    private final Kind kind;
    private final List<CountedIngredient> ingredients;
    private final List<ChanceResult> results;
    private final Optional<Integer> ticks;
    private final Optional<Long> energyPerTick;
    private final Optional<Item> mold;

    public ProcessingRecipe(Kind kind, List<CountedIngredient> ingredients, List<ChanceResult> results,
                            Optional<Integer> ticks, Optional<Long> energyPerTick, Optional<Item> mold) {
        this.kind = kind;
        this.ingredients = List.copyOf(ingredients);
        this.results = List.copyOf(results);
        this.ticks = ticks;
        this.energyPerTick = energyPerTick;
        this.mold = mold;
    }

    /** Pressform, die im Formslot liegen muss und nicht verbraucht wird (5.2). */
    public Optional<Item> mold() {
        return mold;
    }

    public Kind kind() {
        return kind;
    }

    public List<CountedIngredient> ingredients() {
        return ingredients;
    }

    public List<ChanceResult> results() {
        return results;
    }

    public Optional<Integer> ticks() {
        return ticks;
    }

    public Optional<Long> energyPerTick() {
        return energyPerTick;
    }

    /** Benötigte Menge der ersten Zutat. */
    public int inputCount() {
        return ingredients.isEmpty() ? 0 : ingredients.getFirst().count();
    }

    /**
     * Ordnet jeder Zutat einen eigenen Eingabeslot zu, der genug davon enthält (Reihenfolge egal).
     * Ergebnis: Slotindex je Zutat, oder null, wenn das Rezept nicht passt.
     */
    public int[] assign(MachineRecipeInput input) {
        if (ingredients.isEmpty()) {
            return null;
        }
        if (mold.isPresent() && !input.mold().is(mold.get())) {
            return null;
        }
        int[] slots = new int[ingredients.size()];
        boolean[] used = new boolean[input.size()];
        for (int k = 0; k < ingredients.size(); k++) {
            CountedIngredient ci = ingredients.get(k);
            int found = -1;
            for (int s = 0; s < input.size() && found < 0; s++) {
                ItemStack stack = input.getItem(s);
                if (!used[s] && !stack.isEmpty() && ci.ingredient().test(stack) && stack.getCount() >= ci.count()) {
                    found = s;
                }
            }
            if (found < 0) {
                return null;
            }
            used[found] = true;
            slots[k] = found;
        }
        return slots;
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        return assign(input) != null;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().toStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipeTypes.BOOK_CATEGORY.get();
    }

    @Override
    public RecipeSerializer<ProcessingRecipe> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<ProcessingRecipe> getType() {
        return kind.type();
    }
}
