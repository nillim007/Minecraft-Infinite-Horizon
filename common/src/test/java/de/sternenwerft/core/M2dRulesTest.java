package de.sternenwerft.core;

import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.multiblock.MultiblockPattern;
import de.sternenwerft.multiblock.MultiblockRegistry;
import de.sternenwerft.recipe.ExactMatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reine Regeln aus M2d: exakter Abgleich (22.3) und Multiblock-Muster (4.3). */
class M2dRulesTest {

    private static BalanceManager balance;

    @BeforeAll
    static void load() throws Exception {
        balance = BalanceManager.loadDirectory(Path.of(System.getProperty("sternenwerft.specData")).resolve("balance"));
    }

    /** Slot-Inhalt: Item-Name und Menge. */
    private record Stack(String item, int count) {
    }

    /** Zutat: Item-Name und geforderte Menge. */
    private record Need(String item, int count) {
    }

    private static int[] match(List<Stack> slots, List<Need> needs) {
        return ExactMatch.assign(slots, needs, s -> s == null || s.count() == 0,
                (n, s) -> n.item().equals(s.item()), Stack::count, Need::count);
    }

    private static final List<Need> FLUID_PORT = List.of(new Need("iron_plate", 4), new Need("copper_pipe", 1));
    private static final List<Need> CASING = List.of(new Need("iron_plate", 2), new Need("copper_pipe", 1));

    @Test
    void exactMatchSeparatesRecipesWithSameIngredients() {
        List<Stack> four = java.util.Arrays.asList(new Stack("iron_plate", 4), null, new Stack("copper_pipe", 1));
        assertArrayEquals(new int[]{0, -1, 1}, match(four, FLUID_PORT));
        assertNull(match(four, CASING), "4 + 1 darf nicht refinery_casing (2 + 1) ergeben");
    }

    @Test
    void exactMatchSumsSlotsAndRejectsSurplus() {
        // 96 Titanplatten = 64 + 32 (22.3)
        List<Need> plates = List.of(new Need("titanium_plate", 96));
        assertNotNull(match(List.of(new Stack("titanium_plate", 64), new Stack("titanium_plate", 32)), plates));
        assertNull(match(List.of(new Stack("titanium_plate", 64), new Stack("titanium_plate", 33)), plates), "Überschuss");
        assertNull(match(List.of(new Stack("iron_plate", 4), new Stack("copper_pipe", 1), new Stack("stone", 1)), FLUID_PORT),
                "fremdes Item");
        assertNull(match(List.of(new Stack("iron_plate", 3), new Stack("copper_pipe", 1)), FLUID_PORT), "zu wenig");
    }

    @Test
    void refineryPatternHas44PartsWithinNotifyRadius() {
        MultiblockPattern p = MultiblockPattern.forMachine(balance, "sternenwerft:refinery_controller").orElseThrow();
        List<MultiblockPattern.Slot> slots = p.slots();
        assertEquals(44, slots.size(), "3 × 3 × 5 ohne Controller");
        assertTrue(slots.stream().allMatch(s -> s.kind() == MultiblockPattern.SlotKind.PART));
        for (MultiblockPattern.Slot s : slots) {
            assertTrue(Math.abs(s.right()) <= 1 && s.up() >= 0 && s.up() <= 4 && s.back() >= 0 && s.back() <= 2, s.toString());
            assertTrue(Math.max(Math.abs(s.right()), Math.max(s.up(), s.back())) <= MultiblockRegistry.NOTIFY_RADIUS);
        }
    }

    @Test
    void hollowPatternHasAirInside() {
        // Fusionsreaktor (M6) als Beispiel für „hollow“: 98 Hüllenblöcke inkl. Controller, 27 Luft
        MultiblockPattern p = MultiblockPattern.forMachine(balance.withValue("machines", "test:fusion", "multiblock",
                new com.google.gson.JsonPrimitive("fusion_reactor_multiblock")), "test:fusion").orElseThrow();
        long parts = p.slots().stream().filter(s -> s.kind() == MultiblockPattern.SlotKind.PART).count();
        long air = p.slots().stream().filter(s -> s.kind() == MultiblockPattern.SlotKind.AIR).count();
        assertEquals(97, parts);
        assertEquals(27, air);
    }
}
