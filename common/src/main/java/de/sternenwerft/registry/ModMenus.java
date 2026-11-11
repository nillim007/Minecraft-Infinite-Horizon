package de.sternenwerft.registry;

import de.sternenwerft.menu.MachineMenu;
import de.sternenwerft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public final class ModMenus {

    public static final Supplier<MenuType<MachineMenu>> MACHINE = Services.REGISTRY.register(BuiltInRegistries.MENU, "machine",
            () -> Services.REGISTRY.menuType(MachineMenu::new, MachineMenu.OpenData.STREAM_CODEC));

    private ModMenus() {
    }

    public static void init() {
    }
}
