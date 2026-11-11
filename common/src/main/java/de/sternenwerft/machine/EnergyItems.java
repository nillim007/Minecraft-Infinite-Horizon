package de.sternenwerft.machine;

import com.google.gson.JsonObject;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.platform.Services;
import de.sternenwerft.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Laden von Items im Ladeslot (6.3 {@code battery}): Items mit Energie-Schnittstelle des Loaders (NeoForge
 * {@code Capabilities.Energy.ITEM}, Team Reborn {@code EnergyStorage.ITEM}) oder mit der Komponente
 * {@code sternenwerft:energy}. Für die Komponente gilt als Kapazität die {@code capacity} des Item-IDs in storage.json
 * (z. B. ein Akku als Item).
 */
public final class EnergyItems {

    private EnergyItems() {
    }

    /** Kapazität laut storage.json für Items mit {@code sternenwerft:energy}; 0, wenn das Item keine hat. */
    public static long componentCapacity(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
