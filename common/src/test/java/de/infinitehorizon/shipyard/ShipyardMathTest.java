package de.infinitehorizon.shipyard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Orbitalwerft (21.1, 21.2): Bedarf, Verteilung der Lieferungen, Bau in Reihenfolge, auch offline. */
class ShipyardMathTest {

    private static final long BUILD = 48000;
    private static final List<ShipyardMath.Section> SECTIONS = List.of(
            new ShipyardMath.Section(List.of(new ShipyardMath.Material("a:plate", 10, false)), BUILD),
            new ShipyardMath.Section(List.of(new ShipyardMath.Material("a:plate", 5, false),
                    new ShipyardMath.Material("a:methane", 1000, true)), BUILD),
            new ShipyardMath.Section(List.of(new ShipyardMath.Material("a:chip", 2, false)), BUILD));

    @Test
    void deliveryFillsEarliestOpenSectionFirst() {
        ShipyardMath.State st = ShipyardMath.State.empty();
        assertEquals(15, ShipyardMath.demand(SECTIONS, st, "a:plate"));
        ShipyardMath.Delivery d = ShipyardMath.deliver(SECTIONS, st, "a:plate", 12, 100);
        assertEquals(12, d.accepted());
        assertEquals(10, d.state().delivered(0, "a:plate"));
        assertEquals(2, d.state().delivered(1, "a:plate"));
        assertEquals(3, ShipyardMath.demand(SECTIONS, d.state(), "a:plate"));
        assertEquals(0, ShipyardMath.deliver(SECTIONS, d.state(), "a:dirt", 5, 100).accepted(), "nur Benötigtes");
        ShipyardMath.Delivery over = ShipyardMath.deliver(SECTIONS, d.state(), "a:plate", 64, 100);
        assertEquals(3, over.accepted(), "nicht mehr als der Bedarf");
    }

    @Test
    void buildsInOrderAfterDelivery() {
        ShipyardMath.State st = ShipyardMath.deliver(SECTIONS, ShipyardMath.State.empty(), "a:plate", 10, 100).state();
        st = ShipyardMath.advance(SECTIONS, st, 100);
        assertEquals(0, st.built());
        assertEquals(100 + BUILD, st.buildEnd());
        st = ShipyardMath.advance(SECTIONS, st, 100 + BUILD - 1);
        assertEquals(0, st.built());
        st = ShipyardMath.advance(SECTIONS, st, 100 + BUILD);
        assertEquals(1, st.built());
        assertEquals(0, st.buildEnd(), "Abschnitt 2 ist noch nicht beliefert");
    }

    @Test
    void laterSectionWaitsForEarlierOne() {
        ShipyardMath.State st = ShipyardMath.State.empty();
        st = ShipyardMath.deliver(SECTIONS, st, "a:chip", 2, 0).state();   // Abschnitt 3 voll
        st = ShipyardMath.advance(SECTIONS, st, 1000);
        assertEquals(0, st.built());
        assertEquals(0, st.buildEnd(), "Abschnitt 3 baut nicht vor Abschnitt 1");
        st = ShipyardMath.deliver(SECTIONS, st, "a:plate", 15, 1000).state();
        st = ShipyardMath.deliver(SECTIONS, st, "a:methane", 1000, 2000).state();
        // offline lange nicht nachgesehen: alle drei hintereinander
        st = ShipyardMath.advance(SECTIONS, st, 1_000_000);
        assertEquals(3, st.built());
        assertEquals(1000 + BUILD, ShipyardMath.advance(SECTIONS,
                ShipyardMath.deliver(SECTIONS, ShipyardMath.State.empty(), "a:plate", 10, 1000).state(), 1000).buildEnd());
        assertTrue(ShipyardMath.totalDemand(SECTIONS, st) == 0);
    }

    @Test
    void secondSectionStartsWhenFirstEnds() {
        ShipyardMath.State st = ShipyardMath.State.empty();
        st = ShipyardMath.deliver(SECTIONS, st, "a:plate", 15, 0).state();
        st = ShipyardMath.deliver(SECTIONS, st, "a:methane", 1000, 0).state();
        st = ShipyardMath.advance(SECTIONS, st, BUILD + 10);
        assertEquals(1, st.built());
        assertEquals(2 * BUILD, st.buildEnd(), "Abschnitt 2 beginnt mit dem Ende von Abschnitt 1");
    }
}
