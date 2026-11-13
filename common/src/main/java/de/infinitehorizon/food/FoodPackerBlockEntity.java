package de.infinitehorizon.food;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Nahrungspacker (6.1, 17): fester Rezepttyp {@code food_packing} (misc.json {@code code_recipes}). 3 beliebige
 * Nahrungsmittel → 1 {@code space_ration} mit der Summe von Nahrung und Sättigung (je höchstens 20).
 */
public class FoodPackerBlockEntity extends ProcessingMachineBlockEntity {

    public FoodPackerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        int n = layout().inputs();
        List<FoodRules.Food> foods = new ArrayList<>();
        int[] slots = new int[n];
        int[] counts = new int[n];
        for (int i = 0; i < n; i++) {
            FoodProperties f = items.get(i).get(DataComponents.FOOD);
            if (f == null) {
                return Optional.empty();
            }
            foods.add(new FoodRules.Food(f.nutrition(), f.saturation()));
            slots[i] = i;
            counts[i] = 1;
        }
        return Optional.of(new Plan(slots, counts, List.of(), Optional.empty(), Optional.empty(), IhFluidStack.EMPTY,
                List.of(), List.of(ration(foods))));
    }

    /** Ration aus den Eingaben; Grenzen aus agriculture.json. */
    public static ItemStack ration(List<FoodRules.Food> foods) {
        JsonObject cfg = BalanceManager.get().entry("agriculture", InfiniteHorizon.id("space_ration").toString()).orElseThrow();
        Nutrition nut = FoodRules.pack(foods, cfg.get("nutrition_max").getAsInt(), cfg.get("saturation_max").getAsFloat());
        ItemStack out = new ItemStack(ModItems.SPACE_RATION.get());
        out.set(ModDataComponents.NUTRITION.get(), nut);
        out.set(DataComponents.FOOD, new FoodProperties(nut.nutrition(), nut.saturation(), false));
        return out;
    }
}
