package de.infinitehorizon.registry;

import de.infinitehorizon.menu.MachineMenu;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public final class ModMenus {

    public static final Supplier<MenuType<MachineMenu>> MACHINE = Services.REGISTRY.register(BuiltInRegistries.MENU, "machine",
            () -> Services.REGISTRY.menuType(MachineMenu::new, MachineMenu.OpenData.STREAM_CODEC));
    /** Raketen-Computer (12.4); Daten = Position von launch_control. */
    public static final Supplier<MenuType<de.infinitehorizon.space.LaunchControlMenu>> LAUNCH_CONTROL =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "launch_control",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.space.LaunchControlMenu::new,
                            net.minecraft.core.BlockPos.STREAM_CODEC));
    /** Forschungsterminal (11.3); Daten = Position des Blocks. */
    public static final Supplier<MenuType<de.infinitehorizon.research.ResearchTerminalMenu>> RESEARCH_TERMINAL =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "research_terminal",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.research.ResearchTerminalMenu::new,
                            net.minecraft.core.BlockPos.STREAM_CODEC));

    private ModMenus() {
    }

    public static void init() {
    }
}
