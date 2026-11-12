package de.infinitehorizon.core;

import de.infinitehorizon.handbook.HandbookMarkup;
import de.infinitehorizon.handbook.HandbookMarkup.ItemRef;
import de.infinitehorizon.handbook.HandbookMarkup.Link;
import de.infinitehorizon.handbook.HandbookMarkup.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Inline-Markup des Handbuchs (25.6, 27.3 M2f): Links, Items, Fettdruck. */
class HandbookMarkupTest {

    @Test
    void parsesLinksItemsAndBold() {
        List<HandbookMarkup.Segment> s = HandbookMarkup.parse(
                "Fang mit [[infinite_horizon:phase_1/erste_schritte|Erste Schritte]] an. Der <item:infinite_horizon:crusher> ist **wichtig**.");
        assertEquals(List.of(
                new Text("Fang mit ", false),
                new Link("infinite_horizon:phase_1/erste_schritte", "Erste Schritte", false),
                new Text(" an. Der ", false),
                new ItemRef("infinite_horizon:crusher", false),
                new Text(" ist ", false),
                new Text("wichtig", true),
                new Text(".", false)), s);
    }

    @Test
    void boldAppliesToLinksAndUnclosedMarkupStaysText() {
        assertEquals(List.of(new Link("a:b/c", "c", true)), HandbookMarkup.parse("**[[a:b/c]]**"));
        assertEquals(List.of(new Text("[[offen und <item:ohne Ende", false)), HandbookMarkup.parse("[[offen und <item:ohne Ende"));
    }
}
