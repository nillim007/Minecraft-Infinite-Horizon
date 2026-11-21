package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Inhalt im Tooltip (Spieltest Phase 1, Ä31): Flüssigkeit oder Gas in Tank, Druckflasche, Gastank und Tankdepot,
 * Ladung in Akku und Akkubank. Kapazitäten aus {@code storage.json} (mitgelieferte Daten, der Client kennt die des
 * Servers nicht).
 */
public final class ContentTooltips {

    private static final String KEY = "tooltip." + InfiniteHorizon.MOD_ID + ".";

    private ContentTooltips() {
    }

    public static void append(ItemStack stack, List<Component> lines) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var storage = BalanceManager.bundled().entry("storage", id.toString());
        if (storage.isEmpty()) {
            return;
        }
        long capacity = storage.get().has("capacity") ? storage.get().get("capacity").getAsLong() : 0;
        String accepts = storage.get().has("accepts") ? storage.get().get("accepts").getAsString() : "";
        if ("energy".equals(accepts)) {
            Long energy = stack.get(ModDataComponents.ENERGY.get());
            lines.add(Component.translatable(KEY + "energy_content", energy == null ? 0 : energy, capacity)
                    .withStyle(ChatFormatting.AQUA));
            return;
        }
        IhFluidStack fluid = stack.get(ModDataComponents.FLUID.get());
        if (fluid == null || fluid.isEmpty()) {
            lines.add(Component.translatable(KEY + "fluid_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Identifier f = fluid.fluid();
        Component name = Component.translatable("block." + f.getNamespace() + "." + f.getPath());
        lines.add(Component.translatable(KEY + "fluid_content", name, fluid.amount(), capacity).withStyle(ChatFormatting.AQUA));
    }
}
