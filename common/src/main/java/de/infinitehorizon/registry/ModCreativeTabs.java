package de.infinitehorizon.registry;

import de.infinitehorizon.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class ModCreativeTabs {

    public static final Supplier<CreativeModeTab> MAIN = Services.REGISTRY.register(BuiltInRegistries.CREATIVE_MODE_TAB, "main",
            () -> Services.REGISTRY.creativeTab(Component.translatable("itemGroup.infinite_horizon"),
                    () -> new ItemStack(ModItems.CRUSHER.get()),
                    () -> {
                        java.util.List<ItemStack> items = new java.util.ArrayList<>(
                                ModItems.all().stream().map(item -> new ItemStack(item.get())).toList());
                        items.addAll(de.infinitehorizon.machine.PrefilledTanks.stacks());
                        return items;
                    }));

    private ModCreativeTabs() {
    }

    public static void init() {
    }
}
