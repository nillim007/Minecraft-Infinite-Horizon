package de.infinitehorizon.api;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.api.energy.EnergyNode;
import de.infinitehorizon.core.grid.EnergyGrid;
import de.infinitehorizon.core.grid.GridManager;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Öffentlicher Einstieg für Addons (siehe docs/dev/addon-api.md). Alles hier bleibt zwischen kleinen Versionen stabil.
 *
 * <ul>
 *   <li>Werte: eigene Einträge in {@code data/<addon>/infinite_horizon/balance/*.json} ergänzen oder überschreiben unsere.</li>
 *   <li>Maschinen: {@code MachineBlock} + {@code ProcessingMachineBlockEntity} + {@link #newRecipeKind(String)}.</li>
 *   <li>Kabel: {@code new CableBlock(properties)} + Eintrag in {@code transport.json}.</li>
 *   <li>Energie: {@link EnergyNode} implementieren oder die Energie-API des Loaders nutzen.</li>
 * </ul>
 */
public final class InfiniteHorizonApi {

    public static final String MOD_ID = InfiniteHorizon.MOD_ID;

    private InfiniteHorizonApi() {
    }

    /** Aktuelle Balance-Werte (nach jedem /reload neu). */
    public static BalanceManager balance() {
        return BalanceManager.get();
    }

    /** Serverkonfiguration (24.4); auf dem Client die vom Server gesendeten Werte über {@link ServerConfig#client()}. */
    public static ServerConfig config() {
        return ServerConfig.get();
    }

    /** Netz des Kabels an {@code pos} oder null. */
    public static @Nullable EnergyGrid energyGrid(Level level, BlockPos pos) {
        return GridManager.get(level).gridAt(level, pos);
    }

    /**
     * Neue Rezeptart für eine eigene Verarbeitungsmaschine; {@code type()} und {@code serializer()} selbst registrieren.
     * Name mit Namespace ({@code meinaddon:mill}), damit Kategorien und Übersetzungen ({@code jei.meinaddon.mill}) passen.
     */
    public static ProcessingRecipe.Kind newRecipeKind(String name) {
        return new ProcessingRecipe.Kind(name);
    }

    /** Wie {@link #newRecipeKind(String)}, mit exaktem Abgleich wie im Montagetisch (22.3). */
    public static ProcessingRecipe.Kind newExactRecipeKind(String name) {
        return new ProcessingRecipe.Kind(name, true);
    }

    /**
     * Zeigt die Rezeptart in JEI/REI (eigene Kategorie; Katalysator = alle Maschinen, die sie in machines.json unter
     * {@code recipe_types} führen) und schickt ihre Rezepte zum Client. Beim Mod-Start aufrufen.
     */
    public static void showInRecipeViewers(ProcessingRecipe.Kind kind) {
        de.infinitehorizon.compat.RecipeKinds.register(kind);
    }
}
