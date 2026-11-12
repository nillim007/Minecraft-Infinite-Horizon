package de.infinitehorizon.recipe;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.data.ResearchRegistry;
import de.infinitehorizon.data.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Rezeptsperre (11.3): Ein Rezept ist gesperrt, wenn eines seiner Ergebnisse in {@code unlocks} eines Knotens steht,
 * den die Agentur {@code OwnershipResolver.current(owner)} nicht erforscht hat. Ohne Besitzer oder mit
 * {@code research_required = false} ist es frei. Werkbank- und Ofenrezepte haben keine gesperrten Ergebnisse.
 */
public final class ResearchRecipeLock {

    /** Config-Schlüssel (24.4). */
    public static final String CONFIG_KEY = "research_required";

    /** Je Rezeptverwaltung (neu nach {@code /reload}) die Rezepte einer Art, sortiert nach ID. */
    private static final Map<Object, Map<RecipeType<?>, List<RecipeHolder<ProcessingRecipe>>>> SORTED = new WeakHashMap<>();

    private ResearchRecipeLock() {
    }

    /**
     * Reine Funktion: Sind alle Ergebnisse frei, wenn die Agentur {@code researched} erforscht hat?
     * SPEC-GAP #65: Ein Item in mehreren Knoten ist erst frei, wenn alle erforscht sind.
     */
    public static boolean isUnlocked(Collection<Identifier> results, ResearchRegistry registry, Set<Identifier> researched) {
        for (Identifier result : results) {
            for (ResearchRegistry.Node node : registry.nodesUnlocking(result)) {
                if (!researched.contains(node.id())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Ist die Sperre auf diesem Server aktiv ({@code research_required}, Standard true)? */
    public static boolean required() {
        return ServerConfig.get().getBoolean(CONFIG_KEY);
    }

    /** Server: Darf die Agentur {@code owner} (gespeicherter Besitzer) dieses Rezept ausführen? */
    public static boolean isUnlocked(ProcessingRecipe recipe, @Nullable UUID owner, MinecraftServer server) {
        if (owner == null || !required()) {
            return true;
        }
        ResearchRegistry registry = ResearchRegistry.get();
        List<Identifier> results = new ArrayList<>(recipe.results().size());
        boolean any = false;
        for (ProcessingRecipe.ChanceResult r : recipe.results()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(r.item());
            results.add(id);
            any |= !registry.nodesUnlocking(id).isEmpty();
        }
        if (!any) {
            return true;
        }
        AgencyData data = AgencyData.get(server);
        Agency agency = data.byId(data.resolve(owner)).orElse(null);
        return agency == null || isUnlocked(results, registry, agency.progress().researched());
    }

    /** Rezepte einer Art in der Reihenfolge ihrer ID (11.3: „das erste freie“). */
    public static List<RecipeHolder<ProcessingRecipe>> sorted(ServerLevel level, RecipeType<ProcessingRecipe> type) {
        Object manager = level.recipeAccess();
        synchronized (SORTED) {
            Map<RecipeType<?>, List<RecipeHolder<ProcessingRecipe>>> byType = SORTED.computeIfAbsent(manager, m -> new IdentityHashMap<>());
            return byType.computeIfAbsent(type, t -> {
                List<RecipeHolder<ProcessingRecipe>> list = new ArrayList<>();
                for (RecipeHolder<?> h : level.recipeAccess().getRecipes()) {
                    if (h.value().getType() == t && h.value() instanceof ProcessingRecipe) {
                        @SuppressWarnings("unchecked")
                        RecipeHolder<ProcessingRecipe> cast = (RecipeHolder<ProcessingRecipe>) h;
                        list.add(cast);
                    }
                }
                list.sort(Comparator.comparing(h -> h.id().identifier().toString()));
                return List.copyOf(list);
            });
        }
    }
}
