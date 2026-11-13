package de.infinitehorizon.machine;

import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.recipe.ProcessingRecipe;
import de.infinitehorizon.suit.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Montagetisch (6.1): Rezepte {@code assembly}; passt keines, gelten die festen Rezepttypen {@code suit_module_install}
 * und {@code suit_module_remove} (9.2, 25.4) mit Dauer und Verbrauch der Maschine.
 */
public class AssemblyTableBlockEntity extends ProcessingMachineBlockEntity {

    public AssemblyTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                    Supplier<List<ProcessingRecipe.Kind>> kinds) {
        super(type, pos, state, kinds);
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        Optional<Plan> own = super.findPlan(level);
        if (own.isPresent()) {
            return own;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < layout().inputs(); i++) {
            inputs.add(items.get(i));
        }
        return SuitModules.plan(inputs).map(c -> new Plan(c.consumeSlots(), c.consumeCounts(), List.of(), Optional.empty(),
                Optional.empty(), IhFluidStack.EMPTY, List.of(), c.results()));
    }
}
