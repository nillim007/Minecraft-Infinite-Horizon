package de.infinitehorizon.machine;

import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** {@code antimatter_cell} (5): leer oder gefüllt über die Komponente {@code charge} (0/1); gefüllt schimmert sie. */
public class AntimatterCellItem extends Item {

    public AntimatterCellItem(Properties properties) {
        super(properties);
    }

    public static boolean charged(ItemStack stack) {
        Integer c = stack.get(ModDataComponents.CHARGE.get());
        return c != null && c > 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return charged(stack) || super.isFoil(stack);
    }
}
