package de.infinitehorizon.suit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Anzugmodule (9.2): Komponente {@code suit_modules} und die festen Rezepttypen {@code suit_module_install} und
 * {@code suit_module_remove} des Montagetischs (25.4). Die Regeln stehen als reine Funktionen hier, die Werte in
 * {@code suits.json}.
 */
public final class SuitModules {

    /** Teilnamen in der Reihenfolge von {@code pieces} der Anzüge (Spalte „Teil“ in 9.2). */
    public static final List<String> PIECES = List.of("helmet", "chestplate", "leggings", "boots");

    /** Ein fester Vorgang: verbrauchte Slots und Mengen, Ergebnisse (Hauptergebnis zuerst). */
    public record CodePlan(int[] consumeSlots, int[] consumeCounts, List<ItemStack> results) {
    }

    private SuitModules() {
    }

    public static List<Identifier> modules(ItemStack stack) {
        List<Identifier> l = stack.get(ModDataComponents.SUIT_MODULES.get());
        return l == null ? List.of() : l;
    }

    /** Modulslots je Teil ({@code module_slots_per_piece}). */
    public static int slots() {
        return BalanceManager.get().entry("suits", "suit_rules")
                .filter(o -> o.has("module_slots_per_piece")).map(o -> o.get("module_slots_per_piece").getAsInt()).orElse(0);
    }

    /** Teil eines Anzugs ({@code helmet} …) oder null. */
    public static @Nullable String pieceKind(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (Suits.Tier t : Suits.tiers()) {
            int i = t.pieces().indexOf(id.toString());
            if (i >= 0 && i < PIECES.size()) {
                return PIECES.get(i);
            }
        }
        return null;
    }

    /** Teile, auf die das Modul passt (leer = kein Modul). */
    public static List<String> fits(Identifier module) {
        Optional<JsonObject> e = BalanceManager.get().entry("suits", module.toString());
        List<String> out = new ArrayList<>();
        e.filter(o -> o.has("piece") && o.get("piece").isJsonArray())
                .ifPresent(o -> o.getAsJsonArray("piece").forEach((JsonElement p) -> out.add(p.getAsString())));
        return out;
    }

    /** Regel für den Einbau (9.2): Modul passt zum Teil, freier Slot, noch nicht eingebaut. */
    public static boolean canInstall(List<Identifier> installed, Identifier module, List<String> fits, @Nullable String piece,
                                     int slots) {
        return piece != null && fits.contains(piece) && installed.size() < slots && !installed.contains(module);
    }

    /**
     * Fester Vorgang für die belegten Eingabeslots: genau 1 Anzugteil + 1 passendes Modul → Einbau; genau 1 Anzugteil
     * mit Modulen → Ausbau. Sonst nichts.
     */
    public static Optional<CodePlan> plan(List<ItemStack> inputs) {
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).isEmpty()) {
                used.add(i);
            }
        }
        if (used.size() == 2) {
            for (int k = 0; k < 2; k++) {
                int pieceSlot = used.get(k);
                int moduleSlot = used.get(1 - k);
                ItemStack piece = inputs.get(pieceSlot);
                ItemStack module = inputs.get(moduleSlot);
                Identifier moduleId = BuiltInRegistries.ITEM.getKey(module.getItem());
                if (piece.getCount() != 1 || module.getCount() != 1
                        || !canInstall(modules(piece), moduleId, fits(moduleId), pieceKind(piece), slots())) {
                    continue;
                }
                ItemStack result = piece.copyWithCount(1);
                List<Identifier> next = new ArrayList<>(modules(piece));
                next.add(moduleId);
                result.set(ModDataComponents.SUIT_MODULES.get(), List.copyOf(next));
                return Optional.of(new CodePlan(new int[]{pieceSlot, moduleSlot}, new int[]{1, 1}, List.of(result)));
            }
            return Optional.empty();
        }
        if (used.size() == 1) {
            int slot = used.getFirst();
            ItemStack piece = inputs.get(slot);
            List<Identifier> installed = modules(piece);
            if (piece.getCount() != 1 || pieceKind(piece) == null || installed.isEmpty()) {
                return Optional.empty();
            }
            List<ItemStack> results = new ArrayList<>();
            ItemStack bare = piece.copyWithCount(1);
            bare.remove(ModDataComponents.SUIT_MODULES.get());
            results.add(bare);
            for (Identifier id : installed) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                results.add(new ItemStack(item));
            }
            return Optional.of(new CodePlan(new int[]{slot}, new int[]{1}, results));
        }
        return Optional.empty();
    }
}
