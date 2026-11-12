package de.infinitehorizon.environment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Raumanzüge (9.1) aus suits.json: Einträge mit {@code pieces} und {@code tier}. Ein Anzug schützt nur, wenn alle
 * 4 Teile dieselbe oder eine höhere Stufe haben; es gilt die niedrigste Stufe. Die Brustplatte trägt den O₂-Vorrat
 * (Komponente {@code infinite_horizon:oxygen}).
 */
public final class Suits {

    private static final String FILE = "suits";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Eine Anzugstufe: Teile in Slot-Reihenfolge (Helm, Brust, Hose, Stiefel), Schutzwerte, O₂-Verbrauch. */
    public record Tier(String id, int tier, List<String> pieces, double tempMin, double tempMax, Set<Pressure> pressures,
                       int radiationMax, int consumeAmount, int consumePerTicks) {
    }

    /** O₂-Vorrat (suits.json → {@code oxygen_supply}). */
    public record Supply(int capacity, List<String> refillItems) {
    }

    private static @Nullable BalanceManager source;
    private static List<Tier> tiers = List.of();
    private static @Nullable Supply supply;

    private Suits() {
    }

    private static synchronized void load() {
        BalanceManager balance = BalanceManager.getOrBundled();
        if (balance == source) {
            return;
        }
        source = balance;
        List<Tier> out = new ArrayList<>();
        JsonObject entries = balance.entries(FILE).orElse(new JsonObject());
        for (Map.Entry<String, JsonElement> e : entries.entrySet()) {
            if (!e.getValue().isJsonObject() || !e.getValue().getAsJsonObject().has("pieces")) {
                continue;
            }
            JsonObject o = e.getValue().getAsJsonObject();
            List<String> pieces = new ArrayList<>();
            o.getAsJsonArray("pieces").forEach(p -> pieces.add(p.getAsString()));
            Set<Pressure> pressures = EnumSet.noneOf(Pressure.class);
            o.getAsJsonArray("pressure").forEach(p -> pressures.add(Pressure.byId(p.getAsString())));
            JsonObject c = o.getAsJsonObject("oxygen_consumption");
            out.add(new Tier(e.getKey(), o.get("tier").getAsInt(), List.copyOf(pieces), o.get("temp_min").getAsDouble(),
                    o.get("temp_max").getAsDouble(), pressures, o.get("radiation_max").getAsInt(),
                    c.get("amount").getAsInt(), c.get("per_ticks").getAsInt()));
        }
        tiers = List.copyOf(out);
        JsonObject s = entries.has("oxygen_supply") ? entries.getAsJsonObject("oxygen_supply") : new JsonObject();
        List<String> refill = new ArrayList<>();
        if (s.has("auto_refill_from_inventory")) {
            s.getAsJsonArray("auto_refill_from_inventory").forEach(x -> refill.add(x.getAsString()));
        }
        supply = new Supply(s.has("chestplate_capacity") ? s.get("chestplate_capacity").getAsInt() : 0, List.copyOf(refill));
    }

    public static List<Tier> tiers() {
        load();
        return tiers;
    }

    public static Supply supply() {
        load();
        return supply;
    }

    /** Stufe eines Anzugteils (Helm, Brust, Hose oder Stiefel), sonst null. */
    public static @Nullable Tier tierOf(ItemStack stack, int pieceIndex) {
        if (stack.isEmpty()) {
            return null;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (Tier t : tiers()) {
            if (t.pieces().size() > pieceIndex && t.pieces().get(pieceIndex).equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** Wirksame Stufe des getragenen Anzugs (niedrigstes Teil), null ohne vollständigen Anzug (9.1). */
    public static @Nullable Tier worn(Player player) {
        Tier lowest = null;
        for (int i = 0; i < SLOTS.length; i++) {
            Tier t = tierOf(player.getItemBySlot(SLOTS[i]), i);
            if (t == null) {
                return null;
            }
            if (lowest == null || t.tier() < lowest.tier()) {
                lowest = t;
            }
        }
        return lowest;
    }

    public static ItemStack chestplate(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    /** O₂ in der Brustplatte in mB (0 ohne Komponente). */
    public static int oxygen(ItemStack chestplate) {
        Integer v = chestplate.get(ModDataComponents.OXYGEN.get());
        return v == null ? 0 : v;
    }

    public static void setOxygen(ItemStack chestplate, int mb) {
        if (mb <= 0) {
            chestplate.remove(ModDataComponents.OXYGEN.get());
        } else {
            chestplate.set(ModDataComponents.OXYGEN.get(), mb);
        }
    }

    /** Schutz eines vollständigen Anzugs; liefert Sauerstoff, solange O₂ &gt; 0 (9.1). */
    public static Protection protection(Tier tier, int oxygen) {
        return new Protection(tier.tempMin(), tier.tempMax(), tier.pressures(), tier.radiationMax(), oxygen > 0, false);
    }

    /**
     * O₂-Verbrauch in 1/1.000 mB für {@code ticks} Ticks (9.1): {@code amount / per_ticks × oxygen_consumption}.
     * Reine Funktion.
     */
    public static long consumptionMilli(Tier tier, int ticks, double configFactor) {
        return Math.round(1000.0 * tier.consumeAmount() / tier.consumePerTicks() * ticks * configFactor);
    }
}
