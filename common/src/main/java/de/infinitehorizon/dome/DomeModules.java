package de.infinitehorizon.dome;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Kuppelmodule (10.1): Faktoren aus domes.json, als reine Rechnung testbar. */
public final class DomeModules {

    private DomeModules() {
    }

    /** Ist {@code id} ein Kuppelmodul (Eintrag in domes.json mit Namen {@code dome_*_module})? */
    public static boolean isModule(Identifier id) {
        return id.getPath().startsWith("dome_") && id.getPath().endsWith("_module")
                && BalanceManager.get().entry("domes", id.toString()).isPresent();
    }

    /** Produkt des Felds {@code key} aller Module (ohne Feld: 1). */
    public static double factor(List<Identifier> modules, String key) {
        return factor(modules, key, id -> BalanceManager.get().entry("domes", id.toString()).orElse(null));
    }

    /** Reine Rechnung mit einer Nachschlagefunktion für die Moduldaten. */
    public static double factor(List<Identifier> modules, String key, Function<Identifier, JsonObject> data) {
        double f = 1.0;
        for (Identifier m : modules) {
            JsonObject o = data.apply(m);
            if (o != null && o.has(key)) {
                f *= o.get(key).getAsDouble();
            }
        }
        return f;
    }

    /** Für Tests: Nachschlagen aus einer festen Tabelle. */
    public static Function<Identifier, JsonObject> table(Map<Identifier, JsonObject> map) {
        return map::get;
    }
}
