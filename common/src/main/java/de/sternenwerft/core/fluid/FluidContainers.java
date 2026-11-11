package de.sternenwerft.core.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.sternenwerft.data.GeneratedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Behälter-Items für Füll- und Leerslots (4.1): voller Behälter ↔ Fluid + Menge ↔ leerer Behälter.
 * Die Liste ({@code sternenwerft/fluid_containers.json}) erzeugt gen_resources; Addons können eigene Einträge
 * über {@link #register(Entry)} ergänzen.
 */
public final class FluidContainers {

    /** Ein voller Behälter {@code full} enthält {@code amount} mB {@code fluid}; geleert wird er zu {@code empty}. */
    public record Entry(Identifier full, Identifier fluid, long amount, Identifier empty) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean loaded;

    private FluidContainers() {
    }

    private static synchronized List<Entry> entries() {
        if (!loaded) {
            loaded = true;
            for (JsonElement e : GeneratedData.array("fluid_containers", "containers")) {
                JsonObject o = e.getAsJsonObject();
                ENTRIES.add(new Entry(Identifier.parse(o.get("full").getAsString()), Identifier.parse(o.get("fluid").getAsString()),
                        o.get("amount").getAsLong(), Identifier.parse(o.get("empty").getAsString())));
            }
        }
        return ENTRIES;
    }

    public static synchronized void register(Entry entry) {
        entries().add(entry);
    }

    private static Identifier id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    /** Eintrag für einen vollen Behälter, sonst null. */
    public static @Nullable Entry ofFull(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Identifier item = id(stack);
        for (Entry e : entries()) {
            if (e.full().equals(item)) {
                return e;
            }
        }
        return null;
    }

    /** Ist das ein leerer Behälter, der sich mit irgendeinem Fluid füllen lässt? */
    public static boolean isEmptyContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier item = id(stack);
        for (Entry e : entries()) {
            if (e.empty().equals(item)) {
                return true;
            }
        }
        return false;
    }

    /** Eintrag, um den leeren Behälter {@code empty} mit {@code fluid} zu füllen, sonst null. */
    public static @Nullable Entry fillable(ItemStack empty, @Nullable Identifier fluid) {
        if (empty.isEmpty() || fluid == null) {
            return null;
        }
        Identifier item = id(empty);
        for (Entry e : entries()) {
            if (e.empty().equals(item) && e.fluid().equals(fluid)) {
                return e;
            }
        }
        return null;
    }

    public static ItemStack stackOf(Identifier item) {
        Item it = BuiltInRegistries.ITEM.getValue(item);
        return new ItemStack(it);
    }
}
