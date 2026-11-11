package de.sternenwerft.compat;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    /** Item unter der Maus in der Liste, den Lesezeichen oder einem Rezept der Anzeige; leer, wenn keins (Taste H, 24.1). */
    public static volatile @Nullable Supplier<ItemStack> hoveredStack;
    /** Tippt der Spieler gerade in ein Suchfeld der Anzeige? Dann öffnet H nicht das Handbuch. */
    public static volatile @Nullable BooleanSupplier typing;

    /** Item unter der Maus in der Rezeptanzeige oder {@link ItemStack#EMPTY}; Fehler der Anzeige-Mod werden geschluckt. */
    public static ItemStack hovered() {
        Supplier<ItemStack> s = hoveredStack;
        try {
            ItemStack stack = s == null ? null : s.get();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean isTyping() {
        BooleanSupplier t = typing;
        try {
            return t != null && t.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private RecipeViewerHooks() {
    }
}
