package de.infinitehorizon.space;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ziele der Landefähre (12.7): nur zwischen einem Orbit und den Körpern darunter ({@code lander_connects_to} in
 * planets.json); Start von den Dimensionen in {@code launch_forbidden_from} (Erde) verboten; Kosten ohne Transfer.
 */
public final class LanderRoutes {

    private LanderRoutes() {
    }

    /** Verbindet die Fähre {@code from} und {@code to}? Reine Regel über die Felder der Planeten-Einträge. */
    public static boolean connected(Identifier from, @Nullable String fromConnects, Identifier to, @Nullable String toConnects) {
        return to.toString().equals(fromConnects) || from.toString().equals(toConnects);
    }

    private static @Nullable String connects(Planet p) {
        JsonElement e = p.raw().get("lander_connects_to");
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    public static JsonObject rules() {
        return BalanceManager.get().entry("spaceflight", "lander").orElse(new JsonObject());
    }

    /** Ist der Start von hier verboten ({@code launch_forbidden_from})? */
    public static boolean launchForbidden(Identifier here) {
        JsonObject r = rules();
        if (r.has("launch_forbidden_from")) {
            for (JsonElement e : r.getAsJsonArray("launch_forbidden_from")) {
                if (e.getAsString().equals(here.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<LaunchChecklist.Destination> destinations(ServerLevel level, long fuel, @Nullable Planet here,
                                                                 @Nullable Agency agency) {
        if (here == null || launchForbidden(here.dimension())) {
            return List.of();
        }
        FuelCalculator.Constants c = FuelCalculator.Constants.from(BalanceManager.get());
        FuelCalculator.Body from = FuelCalculator.Body.of(here);
        Set<Identifier> reached = agency == null ? Set.of() : agency.progress().probeReached();
        List<LaunchChecklist.Destination> out = new ArrayList<>();
        for (Planet p : PlanetRegistry.get().all()) {
            if (p.dimension().equals(here.dimension()) || !connected(here.dimension(), connects(here), p.dimension(), connects(p))
                    || level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, p.dimension())) == null) {
                continue;
            }
            boolean known = p.dimension().equals(PlanetResolver.EARTH) || p.dimension().getPath().equals("earth_orbit")
                    || reached.contains(p.dimension());
            FuelCalculator.Body to = FuelCalculator.Body.of(p);
            out.add(new LaunchChecklist.Destination(p.dimension(), FuelCalculator.landerCost(from, to, c),
                    FuelCalculator.landerCost(to, from, c), FuelCalculator.landerReach(fuel, from, to, c), known));
        }
        return List.copyOf(out);
    }
}
