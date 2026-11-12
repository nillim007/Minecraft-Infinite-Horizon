package de.infinitehorizon.handbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Inline-Markup der Handbuchtexte (25.6) als reine Funktion: {@code [[<seiten-id>|Text]]} (Link),
 * {@code <item:<id>>} (Item-Name, anklickbar) und {@code **fett**}. Unvollständiges Markup bleibt normaler Text.
 */
public final class HandbookMarkup {

    /** Ein Abschnitt des Texts. */
    public sealed interface Segment permits Text, Link, ItemRef {
        boolean bold();
    }

    /** Normaler Text. */
    public record Text(String text, boolean bold) implements Segment {
    }

    /** Link auf eine Seite; {@code label} ist der sichtbare Text. */
    public record Link(String target, String label, boolean bold) implements Segment {
    }

    /** Verweis auf ein Item; angezeigt wird der übersetzte Name. */
    public record ItemRef(String id, boolean bold) implements Segment {
    }

    private HandbookMarkup() {
    }

    public static List<Segment> parse(String s) {
        List<Segment> out = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean bold = false;
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("**", i)) {
                flush(out, text, bold);
                bold = !bold;
                i += 2;
                continue;
            }
            if (s.startsWith("[[", i)) {
                int end = s.indexOf("]]", i + 2);
                if (end > 0) {
                    flush(out, text, bold);
                    String inner = s.substring(i + 2, end);
                    int bar = inner.indexOf('|');
                    String target = bar >= 0 ? inner.substring(0, bar) : inner;
                    String label = bar >= 0 ? inner.substring(bar + 1) : target.substring(target.lastIndexOf('/') + 1);
                    out.add(new Link(target.trim(), label.replace("**", ""), bold));
                    i = end + 2;
                    continue;
                }
            }
            if (s.startsWith("<item:", i)) {
                int end = s.indexOf('>', i);
                if (end > 0) {
                    flush(out, text, bold);
                    out.add(new ItemRef(s.substring(i + "<item:".length(), end).trim(), bold));
                    i = end + 1;
                    continue;
                }
            }
            text.append(s.charAt(i));
            i++;
        }
        flush(out, text, bold);
        return out;
    }

    private static void flush(List<Segment> out, StringBuilder text, boolean bold) {
        if (!text.isEmpty()) {
            out.add(new Text(text.toString(), bold));
            text.setLength(0);
        }
    }
}
