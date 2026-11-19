package de.infinitehorizon.environment;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Erfolg „Drei Minuten Hölle“ (24.3): reine Regel in {@link VenusHeat#descent}. */
class VenusDescentTest {

    private static final Identifier VENUS = Identifier.fromNamespaceAndPath("infinite_horizon", "venus");
    private static final Identifier MARS = Identifier.fromNamespaceAndPath("infinite_horizon", "mars");
    private final VenusHeat heat = new VenusHeat(VENUS, 180, 3600, 180, 1200);

    @Test
    void belowThenAbove() {
        assertEquals(VenusHeat.Descent.NONE, heat.descent(false, VENUS, 200));
        assertEquals(VenusHeat.Descent.BELOW, heat.descent(false, VENUS, 120));
        assertEquals(VenusHeat.Descent.BELOW, heat.descent(true, VENUS, 150));
        assertEquals(VenusHeat.Descent.RETURNED, heat.descent(true, VENUS, 180));
    }

    @Test
    void otherPlanetsKeepTheMark() {
        assertEquals(VenusHeat.Descent.NONE, heat.descent(false, MARS, 50));
        assertEquals(VenusHeat.Descent.BELOW, heat.descent(true, MARS, 250));
        assertEquals(VenusHeat.Descent.NONE, heat.descent(false, null, 250));
    }
}
