package de.infinitehorizon.logistics;

import com.google.gson.JsonObject;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * {@code cargo_rocket} und {@code heavy_freighter} (20.3): Item im Hangar-Slot eines Frachtterminals. Der Tooltip zeigt
 * Treibstoff bzw. restliche Flüge und den nachgerüsteten Antrieb (Komponenten, der Client kennt keine Balance).
 */
public class CargoRocketItem extends Item {

    public CargoRocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines,
                                TooltipFlag flag) {
        var fuel = stack.get(ModDataComponents.FLUID.get());
        if (fuel != null) {
            lines.accept(Component.translatable("gui.infinite_horizon.cargo_rocket.fuel", fuel.amount())
                    .withStyle(ChatFormatting.GRAY));
        }
        Integer flights = stack.get(ModDataComponents.FLIGHTS_LEFT.get());
        if (flights != null) {
            lines.accept(Component.translatable("gui.infinite_horizon.cargo_rocket.flights", flights).withStyle(ChatFormatting.GRAY));
        }
        Identifier drive = stack.get(ModDataComponents.DRIVE.get());
        if (drive != null) {
            Item item = BuiltInRegistries.ITEM.getValue(drive);
            lines.accept(Component.translatable("gui.infinite_horizon.cargo_rocket.drive", Component.translatable(item.getDescriptionId()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** Für Tests und Werkzeuge: Werte der Rakete aus der Balance. */
    public static JsonObject rules(ItemStack stack) {
        return CargoRockets.rules(stack);
    }
}
