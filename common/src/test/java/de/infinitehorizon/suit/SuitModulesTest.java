package de.infinitehorizon.suit;

import de.infinitehorizon.agency.ProgressLog;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regeln für den Moduleinbau (9.2) und die Zeile des Erfolgsprotokolls (24.3). */
class SuitModulesTest {

    private static final Identifier LAMP = Identifier.fromNamespaceAndPath("infinite_horizon", "helmet_lamp");
    private static final Identifier SHIELD = Identifier.fromNamespaceAndPath("infinite_horizon", "radiation_shield");
    private static final Identifier COOLING = Identifier.fromNamespaceAndPath("infinite_horizon", "cooling_module");

    @Test
    void installNeedsMatchingPieceFreeSlotAndNewModule() {
        List<String> helmet = List.of("helmet");
        assertTrue(SuitModules.canInstall(List.of(), LAMP, helmet, "helmet", 2));
        assertFalse(SuitModules.canInstall(List.of(), LAMP, helmet, "chestplate", 2), "falsches Teil");
        assertFalse(SuitModules.canInstall(List.of(), LAMP, helmet, null, 2), "kein Anzugteil");
        assertFalse(SuitModules.canInstall(List.of(LAMP), LAMP, helmet, "helmet", 2), "schon eingebaut");
        assertTrue(SuitModules.canInstall(List.of(SHIELD), LAMP, helmet, "helmet", 2));
        assertFalse(SuitModules.canInstall(List.of(SHIELD, COOLING), LAMP, helmet, "helmet", 2), "kein freier Slot");
        assertFalse(SuitModules.canInstall(List.of(), LAMP, List.of(), "helmet", 2), "kein Modul");
    }

    @Test
    void progressLogLine() {
        OffsetDateTime t = OffsetDateTime.of(2026, 11, 13, 12, 30, 5, 123_000_000, ZoneOffset.UTC);
        assertEquals("2026-11-13T12:30:05Z Alex infinite_horizon:small_step" + System.lineSeparator(),
                ProgressLog.line(t, "Alex", Identifier.fromNamespaceAndPath("infinite_horizon", "small_step")));
    }
}
