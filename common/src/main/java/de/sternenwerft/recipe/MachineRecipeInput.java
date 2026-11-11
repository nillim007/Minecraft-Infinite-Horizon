package de.sternenwerft.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** Eingabe einer Maschine: alle Eingabeslots und der Formslot (leer, wenn die Maschine keinen hat). */
public record MachineRecipeInput(List<ItemStack> inputs, ItemStack mold) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < inputs.size() ? inputs.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return inputs.size();
    }
}
