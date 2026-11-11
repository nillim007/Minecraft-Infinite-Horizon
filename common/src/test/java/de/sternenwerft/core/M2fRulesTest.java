package de.sternenwerft.core;

import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.recipe.AmountMultiplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reine Regeln aus M2f: Kategorien der Rezeptanzeige (3.9) und Mengen-Multiplikator (22.3). */
class M2fRulesTest {

    private static BalanceManager balance;

    @BeforeAll
    static void load() throws Exception {
        balance = BalanceManager.loadDirectory(Path.of(System.getProperty("sternenwerft.specData")).resolve("balance"));
    }

    @Test
    void categoriesComeFromRecipeTypes() {
        List<String> types = List.of("crushing", "pressing", "electric_smelting", "refining", "electrolysis", "assembly");
        List<RecipeCategories.Category> cats = RecipeCategories.build(balance, types);
        assertEquals(types.size(), cats.size());
        String[] machines = {"crusher", "press", "electric_furnace", "refinery_controller", "electrolyzer", "assembly_table"};
        for (int i = 0; i < types.size(); i++) {
            RecipeCategories.Category c = cats.get(i);
            assertEquals(types.get(i), c.type());
            assertEquals("jei.sternenwerft." + types.get(i), c.titleKey());
            assertTrue(c.catalysts().contains("sternenwerft:" + machines[i]), c + " ohne Katalysator " + machines[i]);
        }
    }

    @Test
    void addonKindGetsOwnNamespace() {
        com.google.gson.JsonArray types = new com.google.gson.JsonArray();
        types.add("meinaddon:milling");
        BalanceManager withAddon = balance.withValue("machines", "meinaddon:mill", "recipe_types", types);
        RecipeCategories.Category c = RecipeCategories.build(withAddon, List.of("meinaddon:milling")).getFirst();
        assertEquals("jei.meinaddon.milling", c.titleKey());
        assertEquals(List.of("meinaddon:mill"), c.catalysts());
    }

    @Test
    void multiplierDoublesStackableIngredients() {
        // JUnit-Beispiel aus 22.3: Multiplikator 2,0 bei 4 iron_plate → 8
        assertArrayEquals(new int[]{8, 2}, AmountMultiplier.scale(new int[]{4, 1}, new int[]{64, 64}, 2.0, 9));
        // unstapelbare Zutaten bleiben, kleine Faktoren runden auf (min. 1)
        assertArrayEquals(new int[]{1, 1}, AmountMultiplier.scale(new int[]{1, 3}, new int[]{1, 64}, 0.25, 9));
        // nur so weit vergrößern, dass es noch in 9 Slots passt: 4 × 128 Platten bräuchten 8 Slots je Zutat
        int[] big = AmountMultiplier.scale(new int[]{128, 64}, new int[]{64, 64}, 4.0, 9);
        assertTrue(AmountMultiplier.slotsNeeded(big, new int[]{64, 64}) <= 9, "passt nicht: " + big[0] + ", " + big[1]);
        assertTrue(big[0] >= 128 && big[1] >= 64, "kleiner als das Original");
    }
}
