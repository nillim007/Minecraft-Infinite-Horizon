package de.sternenwerft.compat;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Brücke zu einer geladenen Rezeptanzeige, ohne deren Klassen anzufassen (die Plugins setzen die Felder, wenn JEI oder
 * REI läuft). Für den Client-Test: Kategorie öffnen und Anzahl der angezeigten Rezepte abfragen.
 */
public final class RecipeViewerHooks {

    /** Öffnet die Kategorie zur Rezeptart (z. B. {@code refining}); null ohne Rezeptanzeige. */
    public static volatile @Nullable Consumer<String> showCategory;
    /** Name der Rezeptanzeige, die die Kategorien registriert hat ({@code jei} oder {@code rei}). */
    public static volatile @Nullable String active;
    /** Zahl der registrierten eigenen Rezepte. */
    public static volatile int recipeCount;

    private RecipeViewerHooks() {
    }
}
