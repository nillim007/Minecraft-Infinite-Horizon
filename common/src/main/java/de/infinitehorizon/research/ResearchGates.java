package de.infinitehorizon.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.ResearchRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Stufen-Tore (11.4, 25.3) als reine Funktion: Agentur-Fakten + Tor-Definition → offen/zu, für alle 5 Tor-Typen
 * ({@code has_block}, {@code enter_dimension}, {@code crafted_items}, {@code machine_completed}, {@code all}).
 * Ein Tor ohne {@code type} gilt als erfüllt (Addon-Stufen ohne Bedingung); unbekannte Typen nie.
 */
public final class ResearchGates {

    /**
     * Was die Agentur erreicht hat.
     *
     * @param blocks            Blockarten mit {@code owner_agency} = Agentur (beim Setzen gezählt)
     * @param dimensions        von Mitgliedern betretene Dimensionen ({@code visited_dimensions})
     * @param craftedItems      mindestens einmal hergestellte Items (Zähler ab M5)
     * @param craftedFluids     mindestens einmal hergestellte Fluide (Zähler ab M5)
     * @param completedMachines abgeschlossene Vorgänge: {@code <block>} und {@code <block>|<rezept>} (Zähler ab M5)
     */
    public record Facts(Set<Identifier> blocks, Set<Identifier> dimensions, Set<Identifier> craftedItems,
                        Set<Identifier> craftedFluids, Set<String> completedMachines) {

        public static Facts of(Set<Identifier> blocks, Set<Identifier> dimensions) {
            return new Facts(blocks, dimensions, Set.of(), Set.of(), Set.of());
        }
    }

    private ResearchGates() {
    }

    /** Ist das Tor mit diesen Fakten erfüllt? */
    public static boolean isMet(JsonObject gate, Facts facts) {
        if (gate == null || !gate.has("type")) {
            return true;
        }
        return switch (gate.get("type").getAsString()) {
            case "has_block" -> facts.blocks().contains(id(gate, "block"));
            case "enter_dimension" -> facts.dimensions().contains(id(gate, "dimension"));
            case "crafted_items" -> allIn(gate, "items", facts.craftedItems()) && allIn(gate, "fluids", facts.craftedFluids());
            case "machine_completed" -> {
                String block = id(gate, "block").toString();
                JsonElement recipe = gate.get("recipe");
                yield recipe == null || recipe.isJsonNull() ? facts.completedMachines().contains(block)
                        : facts.completedMachines().contains(block + "|" + Identifier.parse(recipe.getAsString()));
            }
            case "all" -> {
                for (JsonElement e : gate.getAsJsonArray("gates")) {
                    if (!isMet(e.getAsJsonObject(), facts)) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Offene Stufen nach der Prüfung: Die bisher offenen bleiben offen (Tore schließen sich nie). Stufe n öffnet sich,
     * wenn die vorige Stufe offen ist (die niedrigste Stufe hat keine) und ihr Tor erfüllt ist; geprüft wird in
     * aufsteigender Reihenfolge, sodass mehrere Stufen auf einmal aufgehen können.
     */
    public static SortedSet<Integer> openTiers(Collection<ResearchRegistry.Tier> tiers, Set<Integer> open, Facts facts) {
        SortedSet<Integer> result = new TreeSet<>(open);
        TreeSet<ResearchRegistry.Tier> sorted = new TreeSet<>(java.util.Comparator.comparingInt(ResearchRegistry.Tier::tier));
        sorted.addAll(tiers);
        Integer previous = null;
        for (ResearchRegistry.Tier t : sorted) {
            boolean previousOpen = previous == null || result.contains(previous);
            if (!result.contains(t.tier()) && previousOpen && isMet(t.gate(), facts)) {
                result.add(t.tier());
            }
            previous = t.tier();
        }
        return result;
    }

    private static Identifier id(JsonObject o, String key) {
        return Identifier.parse(o.get(key).getAsString());
    }

    private static boolean allIn(JsonObject gate, String key, Set<Identifier> have) {
        if (!gate.has(key) || gate.get(key).isJsonNull()) {
            return true;
        }
        for (JsonElement e : gate.getAsJsonArray(key)) {
            if (!have.contains(Identifier.parse(e.getAsString()))) {
                return false;
            }
        }
        return true;
    }
}
