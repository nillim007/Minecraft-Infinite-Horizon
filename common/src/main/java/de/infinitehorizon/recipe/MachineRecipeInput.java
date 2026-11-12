package de.infinitehorizon.recipe;

import de.infinitehorizon.core.fluid.IhFluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * Eingabe einer Maschine: alle Eingabeslots, der Formslot (leer, wenn die Maschine keinen hat) und der Inhalt der
 * Eingangstanks.
 */
public record MachineRecipeInput(List<ItemStack> inputs, ItemStack mold, List<IhFluidStack> fluids) implements RecipeInput {

    public MachineRecipeInput(List<ItemStack> inputs, ItemStack mold) {
        this(inputs, mold, List.of());
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < inputs.size() ? inputs.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return inputs.size();
    }

    /** Leer nur ohne Items und ohne Fluid (sonst fände der Rezeptmanager reine Fluid-Rezepte nicht). */
    @Override
    public boolean isEmpty() {
        return inputs.stream().allMatch(ItemStack::isEmpty) && fluids.stream().allMatch(IhFluidStack::isEmpty);
    }
}
