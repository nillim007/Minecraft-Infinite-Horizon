package de.sternenwerft.compat;

import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.ServerConfig;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gemeinsame Aufbereitung eines Maschinenrezepts für JEI und REI (3.9): Zutaten (beim Montagetisch mit
 * {@code recipe_amount_multiplier}), Pressform, Fluide in mB, Ergebnisse mit Chance, Dauer und Energie. Die Plugins
 * zeichnen nur noch. Lage: Eingaben links im Raster mit 3 Spalten, Pfeil in der Mitte, Ausgaben rechts.
 */
public record RecipeView(Identifier id, List<List<ItemStack>> inputs, @Nullable ItemStack mold, List<SwFluidStack> fluidsIn,
                         List<ItemStack> outputs, List<Float> chances, List<SwFluidStack> fluidsOut, int ticks, long energy) {

    /** Breite und Höhe einer Rezeptanzeige in Pixeln. */
    public static final int WIDTH = 150;
    public static final int HEIGHT = 64;
    public static final int SLOT = 18;
    public static final int COLUMNS = 3;
    public static final int ARROW_X = 60;
    public static final int ARROW_Y = 18;
    public static final int OUTPUT_X = 94;
    public static final int TEXT_Y = 56;
    /** Zahl der Eingabeslots des Montagetischs (Grenze des Multiplikators, 22.3). */
    public static final int ASSEMBLY_SLOTS = 9;

    public static RecipeView of(RecipeHolder<ProcessingRecipe> holder, List<String> catalysts) {
        ProcessingRecipe r = holder.value();
        double multiplier = ServerConfig.client().getDouble("recipe_amount_multiplier");
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (ProcessingRecipe.CountedIngredient ci : r.scaledIngredients(multiplier, ASSEMBLY_SLOTS)) {
            List<ItemStack> alts = ci.ingredient().items().map(h -> new ItemStack(h, ci.count())).toList();
            inputs.add(alts);
        }
        ItemStack mold = r.mold().map(ItemStack::new).orElse(null);
        List<ItemStack> outputs = new ArrayList<>();
        List<Float> chances = new ArrayList<>();
        for (ProcessingRecipe.ChanceResult c : r.results()) {
            outputs.add(c.toStack());
            chances.add(c.chance());
        }
        int ticks = r.ticks().orElse(0);
        long ept = r.energyPerTick().orElse(0L);
        if ((ticks == 0 || ept == 0) && !catalysts.isEmpty()) {
            BalanceManager b = BalanceManager.getOrBundled();
            try {
                BalanceManager.MachineStats s = b.machine(catalysts.getFirst());
                ticks = ticks == 0 ? s.ticks() : ticks;
                ept = ept == 0 ? s.energyPerTick() : ept;
            } catch (RuntimeException e) {
                // Katalysator ohne Balance-Eintrag: keine Angabe
            }
        }
        return new RecipeView(holder.id().identifier(), inputs, mold, r.fluid().map(List::of).orElse(List.of()),
                outputs, chances, r.fluidResults(), ticks, ept * ticks);
    }

    /** Alle Ansichten einer Rezeptart. */
    public static List<RecipeView> all(ProcessingRecipe.Kind kind, List<String> catalysts) {
        List<RecipeView> out = new ArrayList<>();
        for (RecipeHolder<ProcessingRecipe> h : ClientRecipes.of(kind)) {
            out.add(of(h, catalysts));
        }
        return out;
    }

    /** Zeile unter dem Rezept: Dauer und Energie (3.9). */
    public Component info() {
        return Component.translatable("gui.sternenwerft.recipe.info", ticks, energy);
    }

    /** Position des n-ten Eingabeplatzes (Items, Pressform, Fluide in dieser Reihenfolge). */
    public static int inputX(int n) {
        return (n % COLUMNS) * SLOT;
    }

    public static int inputY(int n) {
        return (n / COLUMNS) * SLOT;
    }

    public static int outputX(int n) {
        return OUTPUT_X + (n % COLUMNS) * SLOT;
    }

    public static int outputY(int n) {
        return (n / COLUMNS) * SLOT;
    }

    public static Fluid fluid(Identifier id) {
        Fluid f = BuiltInRegistries.FLUID.getValue(id);
        return f == null ? Fluids.EMPTY : f;
    }

    /** Erster Katalysator einer Kategorie als Item (Symbol der Kategorie); Barriere, wenn keiner da ist. */
    public static net.minecraft.world.item.Item catalystItem(RecipeCategories.Category c) {
        if (c.catalysts().isEmpty()) {
            return net.minecraft.world.item.Items.BARRIER;
        }
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(c.catalysts().getFirst()));
        return item == null ? net.minecraft.world.item.Items.BARRIER : item;
    }

    /** Hinweistext zur Pressform. */
    public static Component moldNote() {
        return Component.translatable("gui.sternenwerft.handbook.mold");
    }

    /** Hinweistext zur Chance eines Ergebnisses; leer bei 100 %. */
    public static Optional<Component> chanceNote(float chance) {
        return chance >= 1.0f ? Optional.empty()
                : Optional.of(Component.translatable("gui.sternenwerft.recipe.chance", Math.round(chance * 100)));
    }

    /** Rezeptart zum Namen einer Kategorie (für Plugins). */
    public static ProcessingRecipe.Kind kind(String type) {
        return RecipeKinds.byName(type);
    }
}
