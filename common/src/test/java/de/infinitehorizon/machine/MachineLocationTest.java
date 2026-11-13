package de.infinitehorizon.machine;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ortsbindung von Maschinen (6.1). */
class MachineLocationTest {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier EARTH = Identifier.withDefaultNamespace("overworld");

    @Test
    void onlyListedDimensions() {
        assertTrue(MachineLocation.allowed(Set.of(MOON), Set.of(), MOON));
        assertFalse(MachineLocation.allowed(Set.of(MOON), Set.of(), EARTH));
        assertTrue(MachineLocation.allowed(Set.of(), Set.of(), EARTH), "leere Liste = überall");
        assertFalse(MachineLocation.allowed(Set.of(), Set.of(EARTH), EARTH), "ausgeschlossen");
    }
}
