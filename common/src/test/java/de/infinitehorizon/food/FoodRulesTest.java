package de.infinitehorizon.food;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nahrung (17): Ration packen, Wachstumsregeln, Stamm zum Setzling. */
class FoodRulesTest {

    @Test
    void rationSumsAndCaps() {
        // Brot 5/6,0 + Steak 8/12,8 + Ofenkartoffel 5/6,0 → 18 Nahrung, Sättigung 24,8 → 20
        Nutrition n = FoodRules.pack(List.of(new FoodRules.Food(5, 6.0f), new FoodRules.Food(8, 12.8f),
                new FoodRules.Food(5, 6.0f)), 20, 20f);
        assertEquals(18, n.nutrition());
        assertEquals(20f, n.saturation());
        assertEquals(20, FoodRules.pack(List.of(new FoodRules.Food(8, 1f), new FoodRules.Food(8, 1f),
                new FoodRules.Food(8, 1f)), 20, 20f).nutrition());
    }

    @Test
    void growthNeedsLightAndDome() {
        assertTrue(FoodRules.canGrow(9, 9, false, true));
        assertFalse(FoodRules.canGrow(8, 9, false, true), "zu dunkel");
        assertFalse(FoodRules.canGrow(15, 9, true, true), "ohne Kuppel");
        assertTrue(FoodRules.canGrow(15, 9, true, false), "Kuppel nicht verlangt");
        assertEquals(1333, FoodRules.ticksPerStage(2000, 1333, true));
        assertEquals(2000, FoodRules.ticksPerStage(2000, 1333, false));
    }

    @Test
    void logForSapling() {
        assertEquals("birch_log", FoodRules.logFor("birch_sapling"));
        assertEquals("mangrove_log", FoodRules.logFor("mangrove_propagule"));
        assertEquals("oak_log", FoodRules.logFor("azalea"));
    }
}
