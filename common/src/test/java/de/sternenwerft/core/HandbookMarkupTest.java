package de.sternenwerft.core;

import de.sternenwerft.handbook.HandbookMarkup;
import de.sternenwerft.handbook.HandbookMarkup.ItemRef;
import de.sternenwerft.handbook.HandbookMarkup.Link;
import de.sternenwerft.handbook.HandbookMarkup.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Inline-Markup des Handbuchs (25.6, 27.3 M2f): Links, Items, Fettdruck. */
class HandbookMarkupTest {

    @Test
    void parsesLinksItemsAndBold() {
        List<HandbookMarkup.Segment> s = HandbookMarkup.parse(
                "Fang mit [[sternenwerft:phase_1/erste_schritte|Erste Schritte]] an. Der <item:sternenwerft:crusher> ist **wichtig**.");
        assertEquals(List.of(
                new Text("Fang mit ", false),
                new Link("sternenwerft:phase_1/erste_schritte", "Erste Schritte", false),
                new Text(" an. Der ", false),
                new ItemRef("sternenwerft:crusher", false),
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
