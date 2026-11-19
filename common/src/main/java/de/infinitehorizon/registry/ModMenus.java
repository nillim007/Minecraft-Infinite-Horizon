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

    /** Navigationskarte (12.5); Daten = Ziele, Landefeuer, Treibstoff. */
    public static final Supplier<MenuType<de.infinitehorizon.space.StarMapMenu>> STAR_MAP =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "star_map",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.space.StarMapMenu::new,
                            de.infinitehorizon.space.StarMapMenu.Data.CODEC));
    /** Raketen-GUI einer gelandeten Rakete (12.5). */
    public static final Supplier<MenuType<de.infinitehorizon.space.RocketMenu>> ROCKET =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "rocket",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.space.RocketMenu::new,
                            de.infinitehorizon.space.RocketMenu.Data.CODEC));

    /** Frachtterminal (20.1); Daten = Position, Slot-Aufbau, Tanks. */
    public static final Supplier<MenuType<de.infinitehorizon.logistics.CargoTerminalMenu>> CARGO_TERMINAL =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "cargo_terminal",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.logistics.CargoTerminalMenu::new,
                            de.infinitehorizon.logistics.CargoTerminalMenu.Data.STREAM_CODEC));

    /** Handelsposten (23.3); Daten = Position, Lagerslots, Angebote. */
    public static final Supplier<MenuType<de.infinitehorizon.logistics.TradingPostMenu>> TRADING_POST =
            Services.REGISTRY.register(BuiltInRegistries.MENU, "trading_post",
                    () -> Services.REGISTRY.menuType(de.infinitehorizon.logistics.TradingPostMenu::new,
                            de.infinitehorizon.logistics.TradingPostMenu.Data.STREAM_CODEC));

    private ModMenus() {
    }

    public static void init() {
    }
}
