package de.infinitehorizon.machine;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Vorgefüllte Flüssigkeitstanks im Kreativ-Tab (Spieltest Phase 1, Ä31): Erdöl, Kerosin und Wasser, jeweils voll
 * ({@code capacity} aus storage.json). Per Befehl genauso:
 * {@code /give @s infinite_horizon:fluid_tank[infinite_horizon:fluid={fluid:"infinite_horizon:crude_oil",amount:16000}]}.
 */
public final class PrefilledTanks {

    private PrefilledTanks() {
    }

    public static List<ItemStack> stacks() {
        long capacity = BalanceManager.bundled().entry("storage", InfiniteHorizon.id("fluid_tank").toString())
                .map(o -> o.has("capacity") ? o.get("capacity").getAsLong() : 0L).orElse(0L);
        if (capacity <= 0) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>();
        for (Identifier fluid : List.of(InfiniteHorizon.id("crude_oil"), InfiniteHorizon.id("kerosene"),
                Identifier.withDefaultNamespace("water"))) {
            ItemStack stack = new ItemStack(ModBlocks.FLUID_TANK.get());
            stack.set(ModDataComponents.FLUID.get(), IhFluidStack.of(fluid, capacity));
            out.add(stack);
        }
        return out;
    }
}
