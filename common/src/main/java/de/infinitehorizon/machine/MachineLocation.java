package de.infinitehorizon.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Ortsbindung einer Maschine ({@code location} in machines.json, 6.1): {@code dimensions} (leer = überall) und
 * {@code excluded_dimensions}. Der Ort kommt aus {@link PlanetResolver}, damit der GameTest-Override gilt.
 */
public final class MachineLocation {

    private MachineLocation() {
    }

    /** Reine Regel: Dimension in der Liste (oder Liste leer) und nicht ausgeschlossen. */
    public static boolean allowed(Set<Identifier> dimensions, Set<Identifier> excluded, Identifier here) {
        return (dimensions.isEmpty() || dimensions.contains(here)) && !excluded.contains(here);
    }

    public static boolean allowed(@Nullable JsonObject machine, ServerLevel level, BlockPos pos) {
        if (machine == null || !machine.has("location") || !machine.get("location").isJsonObject()) {
            return true;
        }
        JsonObject loc = machine.getAsJsonObject("location");
        PlanetResolver.Resolved r = PlanetResolver.at(level, pos);
        Identifier here = r.planet() != null ? r.planet().dimension() : level.dimension().identifier();
        String type = loc.has("dimension_type") && loc.get("dimension_type").isJsonPrimitive()
                ? loc.get("dimension_type").getAsString() : null;
        return typeAllowed(type, r.planet() == null ? null : r.planet().type())
                && allowed(ids(loc, "dimensions"), ids(loc, "excluded_dimensions"), here);
    }

    /** Art der Dimension (z. B. {@code orbit}, 14.2): ohne Vorgabe überall, sonst nur bei gleicher Planetenart. */
    public static boolean typeAllowed(@Nullable String required, @Nullable String planetType) {
        return required == null || required.equals(planetType);
    }

    private static Set<Identifier> ids(JsonObject o, String key) {
        Set<Identifier> out = new HashSet<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                out.add(Identifier.parse(e.getAsString()));
            }
        }
        return out;
    }
}
