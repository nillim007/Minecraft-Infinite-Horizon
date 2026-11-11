package de.sternenwerft.machine;

import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Elektroofen (6.1): eigene Rezepte ({@code electric_smelting}) und alle Vanilla-Rezepte {@code minecraft:smelting}.
 * Bei Vanilla-Rezepten gilt immer die Maschinendauer; {@code cookingtime} und Erfahrung entfallen.
 */
public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {

    public ElectricFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      Supplier<List<ProcessingRecipe.Kind>> kinds) {
        super(type, pos, state, kinds);
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        Optional<Plan> own = super.findPlan(level);
        if (own.isPresent()) {
            return own;
        }
        MachineLayout l = layout();
        for (int i = 0; i < l.inputs(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (found.isPresent()) {
                ItemStack out = found.get().value().assemble(input);
                if (!out.isEmpty()) {
                    return Optional.of(new Plan(new int[]{i}, new int[]{1},
                            List.of(new ProcessingRecipe.ChanceResult(out.getItem(), out.getCount(), 1.0f)),
                            Optional.empty(), Optional.empty()));
                }
            }
        }
        return Optional.empty();
    }
}
