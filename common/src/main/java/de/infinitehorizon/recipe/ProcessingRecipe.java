package de.infinitehorizon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.registry.ModRecipeTypes;
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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Rezept einer Verarbeitungsmaschine (Format 25.4):
 * {@code {"type": "<ns>:<art>", "ingredients": [{"ingredient": …, "count": n}], "results": [{"id", "count", "chance"}],
 * "fluid"?: {"fluid", "amount"}, "fluid_results"?: [{"fluid", "amount"}], "ticks"?: n, "energy_per_tick"?: n, "mold"?: id}}.
 * Fehlen {@code ticks}/{@code energy_per_tick}, gelten die Werte der Maschine.
 *
 * <p>Eine neue Rezeptart braucht nur eine {@link Kind}; Addons registrieren deren {@code type()} und
 * {@code serializer()} unter eigener ID. {@link Kind#exact()} schaltet den exakten Abgleich des Montagetischs ein (22.3).
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

    /** Zuordnung der Zutaten zu Slots: welche Slots wie viel verbrauchen. */
    public record Match(int[] slots, int[] counts) {
    }

    /** Eine Rezeptart (z. B. {@code crushing}) mit eigenem RecipeType und Serializer. */
    public static final class Kind {
        private final String name;
        private final boolean exact;
        private final RecipeType<ProcessingRecipe> type;
        private final RecipeSerializer<ProcessingRecipe> serializer;

        public Kind(String name) {
            this(name, false);
        }

        /** @param exact exakter Abgleich (22.3): belegte Slots enthalten genau die Zutatenmengen, nichts anderes */
        public Kind(String name, boolean exact) {
            this.name = name;
            this.exact = exact;
            this.type = new RecipeType<>() {
                @Override
                public String toString() {
                    return name;
                }
            };
            MapCodec<ProcessingRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    CountedIngredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(ProcessingRecipe::ingredients),
                    ChanceResult.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(ProcessingRecipe::results),
                    IhFluidStack.CODEC.optionalFieldOf("fluid").forGetter(ProcessingRecipe::fluid),
                    IhFluidStack.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(ProcessingRecipe::fluidResults),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("ticks").forGetter(ProcessingRecipe::ticks),
                    Codec.LONG.optionalFieldOf("energy_per_tick").forGetter(ProcessingRecipe::energyPerTick),
                    BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("mold").forGetter(ProcessingRecipe::mold)
            ).apply(i, (ing, res, f, fr, t, e, m) -> new ProcessingRecipe(this, ing, res, f, fr, t, e, m)));
            this.serializer = new RecipeSerializer<>(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()));
        }

        public String name() {
            return name;
        }

        public boolean exact() {
            return exact;
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
    private final Optional<IhFluidStack> fluid;
    private final List<IhFluidStack> fluidResults;
    private final Optional<Integer> ticks;
    private final Optional<Long> energyPerTick;
    private final Optional<Item> mold;

    public ProcessingRecipe(Kind kind, List<CountedIngredient> ingredients, List<ChanceResult> results,
                            Optional<Integer> ticks, Optional<Long> energyPerTick, Optional<Item> mold) {
        this(kind, ingredients, results, Optional.empty(), List.of(), ticks, energyPerTick, mold);
    }

    public ProcessingRecipe(Kind kind, List<CountedIngredient> ingredients, List<ChanceResult> results,
                            Optional<IhFluidStack> fluid, List<IhFluidStack> fluidResults,
                            Optional<Integer> ticks, Optional<Long> energyPerTick, Optional<Item> mold) {
        this.kind = kind;
        this.ingredients = List.copyOf(ingredients);
        this.results = List.copyOf(results);
        this.fluid = fluid;
        this.fluidResults = List.copyOf(fluidResults);
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

    /** Fluid-Zutat aus einem Eingangstank (Raffinerie, Elektrolyseur, Montagetisch). */
    public Optional<IhFluidStack> fluid() {
        return fluid;
    }

    /** Fluid-Ergebnisse, je eines in einen Ausgangstank. */
    public List<IhFluidStack> fluidResults() {
        return fluidResults;
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
     * Prüft das Rezept gegen die Eingabe und liefert, welche Slots wie viel verbrauchen; null, wenn es nicht passt.
     * Normal: jede Zutat in einem eigenen Slot mit mindestens der Menge (Reihenfolge egal). Exakt (22.3): alle belegten
     * Slots zusammen enthalten genau die Zutaten. Eine Fluid-Zutat braucht einen Eingangstank mit genug davon.
     */
    public @Nullable Match match(MachineRecipeInput input) {
        if (mold.isPresent() && !input.mold().is(mold.get())) {
            return null;
        }
        // SPEC-GAP: Fluid-Zutat „mindestens“, auch beim exakten Abgleich (spec-gaps #26)
        if (fluid.isPresent() && input.fluids().stream()
                .noneMatch(f -> f.is(fluid.get().fluid()) && f.amount() >= fluid.get().amount())) {
            return null;
        }
        if (kind.exact()) {
            List<CountedIngredient> need = scaledIngredients(
                    de.infinitehorizon.data.ServerConfig.get().getDouble("recipe_amount_multiplier"), input.size());
            int[] owner = ExactMatch.assign(input.inputs(), need, ItemStack::isEmpty,
                    (ci, stack) -> ci.ingredient().test(stack), ItemStack::getCount, CountedIngredient::count);
            if (owner == null) {
                return null;
            }
            int n = 0;
            for (int o : owner) {
                n += o >= 0 ? 1 : 0;
            }
            int[] slots = new int[n];
            int[] counts = new int[n];
            int k = 0;
            for (int s = 0; s < owner.length; s++) {
                if (owner[s] >= 0) {
                    slots[k] = s;
                    counts[k++] = input.getItem(s).getCount();
                }
            }
            return new Match(slots, counts);
        }
        if (ingredients.isEmpty()) {
            return fluid.isPresent() ? new Match(new int[0], new int[0]) : null;
        }
        int[] slots = assign(input);
        return slots == null ? null
                : new Match(slots, ingredients.stream().mapToInt(CountedIngredient::count).toArray());
    }

    /**
     * Zutaten mit dem Mengen-Multiplikator (22.3): nur bei exaktem Abgleich (Montagetisch), nur stapelbare Zutaten,
     * höchstens so groß, dass alles in {@code slots} Slots passt. Auch für die Anzeige in JEI/REI.
     */
    public List<CountedIngredient> scaledIngredients(double multiplier, int slots) {
        if (!kind.exact() || multiplier <= 0 || multiplier == 1.0 || ingredients.isEmpty()) {
            return ingredients;
        }
        int[] counts = new int[ingredients.size()];
        int[] stacks = new int[ingredients.size()];
        for (int i = 0; i < counts.length; i++) {
            CountedIngredient ci = ingredients.get(i);
            counts[i] = ci.count();
            stacks[i] = ci.ingredient().items().findFirst().map(h -> h.value().getDefaultMaxStackSize()).orElse(64);
        }
        int[] scaled = AmountMultiplier.scale(counts, stacks, multiplier, slots);
        List<CountedIngredient> out = new java.util.ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            out.add(new CountedIngredient(ingredients.get(i).ingredient(), scaled[i]));
        }
        return out;
    }

    /**
     * Ordnet jeder Zutat einen eigenen Eingabeslot zu, der genug davon enthält (Reihenfolge egal).
     * Ergebnis: Slotindex je Zutat, oder null, wenn das Rezept nicht passt.
     */
    public int @Nullable [] assign(MachineRecipeInput input) {
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
        return match(input) != null;
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
