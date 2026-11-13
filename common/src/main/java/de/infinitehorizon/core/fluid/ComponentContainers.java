package de.infinitehorizon.core.fluid;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Behälter mit Inhalt in der Komponente {@code infinite_horizon:fluid} (4.1, ab M3: Sauerstoffflaschen). Sie bleiben
 * dasselbe Item; die Übergabe geschieht sofort, so viel wie passt bzw. vorhanden ist (Teilfüllung erlaubt). „Voll“ heißt
 * Kapazität erreicht, „leer“ heißt Komponente fehlt. Werte aus der Balance: Einträge mit {@code capacity} und
 * {@code fluids} in {@link #FILES} (Addons ergänzen eigene Behälter dort).
 */
public final class ComponentContainers {

    /** Balance-Dateien, in denen Behälter-Einträge stehen (Item-ID → capacity, fluids). */
    public static final String[] FILES = {"suits", "items", "storage"};

    /** Fassungsvermögen und erlaubte Fluide eines Behälters. */
    public record Spec(long capacity, Set<Identifier> fluids) {
        public boolean accepts(@Nullable Identifier fluid) {
            return fluid != null && (fluids.isEmpty() || fluids.contains(fluid));
        }
    }

    private ComponentContainers() {
    }

    /** Behälterdaten des Items, sonst null. */
    public static @Nullable Spec spec(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) {
            return null;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        BalanceManager balance = BalanceManager.getOrBundled();
        for (String file : FILES) {
            Optional<JsonObject> e = balance.entry(file, id);
            if (e.isPresent() && e.get().has("capacity") && e.get().has("fluids")) {
                Set<Identifier> fluids = new LinkedHashSet<>();
                e.get().getAsJsonArray("fluids").forEach(f -> fluids.add(Identifier.parse(f.getAsString())));
                return new Spec(e.get().get("capacity").getAsLong(), Set.copyOf(fluids));
            }
            // Behälter-Items aus storage.json (transport_tank, 4.1): kind = item, accepts = all → jedes Fluid
            if (e.isPresent() && e.get().has("capacity") && e.get().has("kind")
                    && "item".equals(e.get().get("kind").getAsString())) {
                return new Spec(e.get().get("capacity").getAsLong(), Set.of());
            }
        }
        return null;
    }

    public static IhFluidStack content(ItemStack stack) {
        IhFluidStack s = stack.get(ModDataComponents.FLUID.get());
        return s == null ? IhFluidStack.EMPTY : s;
    }

    /** Lässt sich der Behälter (noch) mit {@code fluid} füllen? {@code fluid == null}: mit irgendeinem erlaubten. */
    public static boolean canFill(ItemStack stack, @Nullable Identifier fluid) {
        Spec spec = spec(stack);
        if (spec == null) {
            return false;
        }
        IhFluidStack c = content(stack);
        if (c.amount() >= spec.capacity()) {
            return false;
        }
        return fluid == null || (spec.accepts(fluid) && (c.isEmpty() || c.is(fluid)));
    }

    public static boolean isFull(ItemStack stack) {
        Spec spec = spec(stack);
        return spec != null && content(stack).amount() >= spec.capacity();
    }

    public static boolean hasContent(ItemStack stack) {
        return spec(stack) != null && !content(stack).isEmpty();
    }

    /** Füllt bis zu {@code max} mB {@code fluid} ein; liefert die eingefüllte Menge. */
    public static long fill(ItemStack stack, Identifier fluid, long max) {
        Spec spec = spec(stack);
        if (spec == null || !canFill(stack, fluid) || max <= 0) {
            return 0;
        }
        IhFluidStack c = content(stack);
        long moved = Math.min(max, spec.capacity() - c.amount());
        stack.set(ModDataComponents.FLUID.get(), IhFluidStack.of(fluid, c.amount() + moved));
        return moved;
    }

    /** Entnimmt bis zu {@code max} mB; ohne Rest verliert der Behälter die Komponente. */
    public static IhFluidStack drain(ItemStack stack, long max) {
        IhFluidStack c = content(stack);
        if (c.isEmpty() || max <= 0) {
            return IhFluidStack.EMPTY;
        }
        long moved = Math.min(max, c.amount());
        long rest = c.amount() - moved;
        if (rest <= 0) {
            stack.remove(ModDataComponents.FLUID.get());
        } else {
            stack.set(ModDataComponents.FLUID.get(), c.withAmount(rest));
        }
        return IhFluidStack.of(c.fluid(), moved);
    }
}
