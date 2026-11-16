package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Laden von Items im Ladeslot (6.3 {@code battery}): Items mit Energie-Schnittstelle des Loaders (NeoForge
 * {@code Capabilities.Energy.ITEM}, Team Reborn {@code EnergyStorage.ITEM}) oder mit der Komponente
 * {@code infinite_horizon:energy}. Für die Komponente gilt als Kapazität die {@code capacity} des Item-IDs in storage.json
 * (z. B. ein Akku als Item).
 */
public final class EnergyItems {

    private EnergyItems() {
    }

    /** Kapazität laut storage.json für Items mit {@code infinite_horizon:energy}; 0, wenn das Item keine hat. */
    public static long componentCapacity(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // Anzugmodule mit Energie (9.2: jetpack) und Brustplatten mit eingebautem Jetpack
        java.util.List<net.minecraft.resources.Identifier> mods = de.infinitehorizon.suit.SuitModules.modules(stack);
        String module = mods.contains(de.infinitehorizon.suit.Jetpack.ID) ? de.infinitehorizon.suit.Jetpack.ID.toString() : id;
        JsonObject suit = BalanceManager.get().entry("suits", module).orElse(null);
        if (suit != null && suit.has("energy_capacity")) {
            return suit.get("energy_capacity").getAsLong();
        }
        // Werkzeuge mit Akku (items.json, z. B. ice_drill)
        JsonObject tool = BalanceManager.get().entry("items", id).orElse(null);
        if (tool != null && tool.has("energy_capacity")) {
            return tool.get("energy_capacity").getAsLong();
        }
        JsonObject e = BalanceManager.get().entry("storage", id).orElse(null);
        if (e == null || !e.has("capacity") || !"J".equals(e.has("unit") ? e.get("unit").getAsString() : "")) {
            return 0;
        }
        return e.get("capacity").getAsLong();
    }

    /** Darf das Item in einen Ladeslot? */
    public static boolean isChargeable(ItemStack stack) {
        return !stack.isEmpty() && (componentCapacity(stack) > 0 || Services.ENERGY.isPlatformChargeable(stack));
    }

    /** Gespeicherte Energie laut Komponente. */
    public static long stored(ItemStack stack) {
        Long v = stack.get(ModDataComponents.ENERGY.get());
        return v == null ? 0 : v;
    }

    /**
     * Lädt das Item in Slot {@code slot} mit höchstens {@code max} J und gibt die angenommene Menge zurück. Erst über die
     * Komponente, sonst über die Loader-API.
     */
    public static long charge(Container container, int slot, long max) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || max <= 0) {
            return 0;
        }
        long cap = componentCapacity(stack);
        if (cap > 0) {
            if (stack.getCount() != 1) {
                return 0;
            }
            long now = stored(stack);
            long add = Math.min(max, cap - now);
            if (add > 0) {
                stack.set(ModDataComponents.ENERGY.get(), now + add);
                container.setChanged();
            }
            return Math.max(0, add);
        }
        return Services.ENERGY.chargeItem(container, slot, max);
    }
}
