package de.infinitehorizon.logistics;

import com.google.gson.JsonObject;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Frachtraketen (20.3): Werte je Raketen-Item aus {@code logistics.json} (Laderäume, Tank oder Brennstab, Antrieb),
 * Treibstoff im Item ({@code infinite_horizon:fluid}), Flüge des Frachters ({@code flights_left}) und Antrieb
 * ({@code infinite_horizon:drive}; es zählt nur der beste).
 */
public final class CargoRockets {

    private CargoRockets() {
    }

    private static JsonObject entry(String id) {
        return BalanceManager.get().entry("logistics", id).orElse(new JsonObject());
    }

    /** Werte der Rakete oder ein leeres Objekt, wenn das Item keine Frachtrakete ist. */
    public static JsonObject rules(ItemStack stack) {
        if (stack.isEmpty()) {
            return new JsonObject();
        }
        JsonObject e = entry(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return e.has("item_holds") ? e : new JsonObject();
    }

    public static boolean isRocket(ItemStack stack) {
        return rules(stack).has("item_holds");
    }

    private static int integer(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : 0;
    }

    /** Item-Slots im Laderaum (Laderäume × Slots je Laderaum). */
    public static int itemSlots(ItemStack rocket) {
        JsonObject r = rules(rocket);
        return integer(r, "item_holds") * integer(r, "slots_per_hold");
    }

    public static int fluidHolds(ItemStack rocket) {
        return integer(rules(rocket), "fluid_holds");
    }

    public static long fluidHoldCapacity(ItemStack rocket) {
        JsonObject r = rules(rocket);
        return r.has("fluid_hold_capacity") ? r.get("fluid_hold_capacity").getAsLong() : 0;
    }

    /** Tank der Rakete ({@code fuel_tank}) oder null (Frachter mit Brennstab). */
    public static @Nullable JsonObject fuelTank(ItemStack rocket) {
        JsonObject r = rules(rocket);
        return r.has("fuel_tank") && r.get("fuel_tank").isJsonObject() ? r.getAsJsonObject("fuel_tank") : null;
    }

    /** Brennstab-Angabe ({@code fuel}: Item, Anzahl, Flüge) oder null. */
    public static @Nullable JsonObject fuelItem(ItemStack rocket) {
        JsonObject r = rules(rocket);
        return r.has("fuel") && r.get("fuel").isJsonObject() ? r.getAsJsonObject("fuel") : null;
    }

    /** Treibstoff im Tank der Rakete in mB. */
    public static long fuel(ItemStack rocket) {
        IhFluidStack f = rocket.get(ModDataComponents.FLUID.get());
        return f == null ? 0 : f.amount();
    }

    public static void setFuel(ItemStack rocket, long amount) {
        JsonObject tank = fuelTank(rocket);
        if (tank == null) {
            return;
        }
        if (amount <= 0) {
            rocket.remove(ModDataComponents.FLUID.get());
        } else {
            rocket.set(ModDataComponents.FLUID.get(), IhFluidStack.of(Identifier.parse(tank.get("fluid").getAsString()), amount));
        }
    }

    public static int flightsLeft(ItemStack rocket) {
        Integer v = rocket.get(ModDataComponents.FLIGHTS_LEFT.get());
        return v == null ? 0 : v;
    }

    public static void setFlightsLeft(ItemStack rocket, int flights) {
        rocket.set(ModDataComponents.FLIGHTS_LEFT.get(), Math.max(0, flights));
    }

    /** Treibstoff für eine Strecke in mB (20.3: {@code base + per_delta_dp × |ΔDP|}); 0 beim Frachter. */
    public static long fuelPerLeg(ItemStack rocket, double dpFrom, double dpTo) {
        JsonObject r = rules(rocket);
        if (!r.has("fuel_per_flight") || !r.get("fuel_per_flight").isJsonObject()) {
            return 0;
        }
        JsonObject f = r.getAsJsonObject("fuel_per_flight");
        return CargoFlightMath.fuelPerLeg(f.get("base").getAsLong(), f.get("per_delta_dp").getAsLong(), dpFrom, dpTo);
    }

    /** Flugzeit-Divisor eines Antriebs ({@code flight_time_divisor}), 1 ohne Antrieb. */
    public static double divisor(@Nullable Identifier drive) {
        if (drive == null) {
            return 1.0;
        }
        JsonObject d = entry(drive.toString());
        return d.has("flight_time_divisor") ? d.get("flight_time_divisor").getAsDouble() : 1.0;
    }

    /** Bester Antrieb: eingebauter ({@code drive} der Rakete) oder nachgerüsteter (Komponente). */
    public static @Nullable Identifier bestDrive(ItemStack rocket) {
        JsonObject r = rules(rocket);
        Identifier builtin = r.has("drive") && r.get("drive").isJsonPrimitive() ? Identifier.parse(r.get("drive").getAsString()) : null;
        Identifier fitted = rocket.get(ModDataComponents.DRIVE.get());
        if (builtin == null) {
            return fitted;
        }
        if (fitted == null) {
            return builtin;
        }
        return divisor(fitted) >= divisor(builtin) ? fitted : builtin;
    }

    /** Flugzeit einer Strecke in Ticks (20.3), geteilt durch den Divisor des besten Antriebs. */
    public static long flightTicks(ItemStack rocket, double dpFrom, double dpTo) {
        JsonObject t = entry("flight_time");
        return CargoFlightMath.flightTicks(t.get("base_ticks").getAsLong(), t.get("per_delta_dp_ticks").getAsLong(),
                dpFrom, dpTo, divisor(bestDrive(rocket)));
    }

    /** Nachrüsten (20.3): ein schwächerer Antrieb ersetzt keinen besseren aus {@code source}. */
    public static void keepBestDrive(ItemStack result, ItemStack source) {
        if (source.isEmpty()) {
            return;
        }
        Identifier old = source.get(ModDataComponents.DRIVE.get());
        Identifier now = result.get(ModDataComponents.DRIVE.get());
        if (old != null && now != null && divisor(old) > divisor(now)) {
            result.set(ModDataComponents.DRIVE.get(), old);
        }
    }
}
