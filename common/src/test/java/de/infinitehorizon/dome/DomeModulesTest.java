package de.infinitehorizon.dome;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Kuppelmodule (10.1): Faktoren für Radius, Strom und O₂. */
class DomeModulesTest {

    private static final Identifier RANGE = Identifier.fromNamespaceAndPath("infinite_horizon", "dome_range_module");
    private static final Identifier EFFICIENCY = Identifier.fromNamespaceAndPath("infinite_horizon", "dome_efficiency_module");

    private static JsonObject obj(String key, double v, String key2, double v2) {
        JsonObject o = new JsonObject();
        o.addProperty(key, v);
        if (key2 != null) {
            o.addProperty(key2, v2);
        }
        return o;
    }

    @Test
    void rangeAndEfficiencyCombine() {
        JsonObject range = obj("radius_multiplier", 1.5, "oxygen_multiplier", 2.0);
        JsonObject eff = obj("oxygen_multiplier", 0.75, null, 0);
        var data = DomeModules.table(Map.of(RANGE, range, EFFICIENCY, eff));
        assertEquals(1.5, DomeModules.factor(List.of(RANGE, EFFICIENCY), "radius_multiplier", data));
        assertEquals(1.5, DomeModules.factor(List.of(RANGE, EFFICIENCY), "oxygen_multiplier", data), "2 × 0,75");
        assertEquals(1.0, DomeModules.factor(List.of(), "radius_multiplier", data));
        // T1 (Radius 8) mit Reichweitenmodul: 12
        assertEquals(12, (int) Math.floor(8 * DomeModules.factor(List.of(RANGE), "radius_multiplier", data)));
    }
}
