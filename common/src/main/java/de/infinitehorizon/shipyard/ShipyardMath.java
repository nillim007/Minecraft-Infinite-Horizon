package de.infinitehorizon.shipyard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orbitalwerft (21.1, 21.2), reine Funktionen: Bedarf je Item/Fluid über alle offenen Abschnitte, Verteilung einer
 * Lieferung (früheste offene Abschnitte zuerst) und Bau in Reihenfolge 1 → 8, je {@code build_ticks} Overworld-Zeit ab
 * dem späteren Zeitpunkt von „voll beliefert“ und „Vorgänger fertig“ (läuft auch offline).
 */
public final class ShipyardMath {

    /** Material eines Abschnitts: Item- oder Fluid-ID und Menge (Stück bzw. mB). */
    public record Material(String id, long amount, boolean fluid) {
    }

    /** Bauabschnitt. */
    public record Section(List<Material> materials, long buildTicks) {
    }

    /**
     * Fortschritt der Werft.
     *
     * @param built       fertige Abschnitte (0–8)
     * @param buildEnd    Ende des laufenden Baus (0 = keiner)
     * @param lastEnd     Ende des zuletzt fertigen Abschnitts
     * @param delivered   geliefert je Abschnitt (Index ab 0) und ID
     * @param completedAt Zeitpunkt, zu dem ein Abschnitt voll beliefert war
     */
    public record State(int built, long buildEnd, long lastEnd, Map<Integer, Map<String, Long>> delivered,
                        Map<Integer, Long> completedAt) {

        public static State empty() {
            return new State(0, 0, 0, Map.of(), Map.of());
        }

        public long delivered(int section, String id) {
            return delivered.getOrDefault(section, Map.of()).getOrDefault(id, 0L);
        }
    }

    private ShipyardMath() {
    }

    public static long missing(Section s, int index, State st, String id) {
        long need = 0;
        for (Material m : s.materials()) {
            if (m.id().equals(id)) {
                need += m.amount();
            }
        }
        return Math.max(0, need - st.delivered(index, id));
    }

    public static boolean complete(Section s, int index, State st) {
        for (Material m : s.materials()) {
            if (missing(s, index, st, m.id()) > 0) {
                return false;
            }
        }
        return true;
    }

    /** Offener Bedarf für eine ID über alle noch nicht gebauten Abschnitte. */
    public static long demand(List<Section> sections, State st, String id) {
        long d = 0;
        for (int i = st.built(); i < sections.size(); i++) {
            d += missing(sections.get(i), i, st, id);
        }
        return d;
    }

    /** Gesamter offener Bedarf (alle IDs). */
    public static long totalDemand(List<Section> sections, State st) {
        long d = 0;
        for (int i = st.built(); i < sections.size(); i++) {
            for (Material m : sections.get(i).materials()) {
                d += missing(sections.get(i), i, st, m.id());
            }
        }
        return d;
    }

    /** Ergebnis einer Lieferung: neuer Zustand und angenommene Menge. */
    public record Delivery(State state, long accepted) {
    }

    /** Liefert bis zu {@code amount} von {@code id}; die frühesten offenen Abschnitte zuerst. */
    public static Delivery deliver(List<Section> sections, State st, String id, long amount, long now) {
        Map<Integer, Map<String, Long>> delivered = copy(st.delivered());
        Map<Integer, Long> completedAt = new HashMap<>(st.completedAt());
        State cur = st;
        long left = amount;
        for (int i = st.built(); i < sections.size() && left > 0; i++) {
            long miss = missing(sections.get(i), i, cur, id);
            if (miss <= 0) {
                continue;
            }
            long take = Math.min(miss, left);
            delivered.computeIfAbsent(i, k -> new HashMap<>()).merge(id, take, Long::sum);
            left -= take;
            cur = new State(st.built(), st.buildEnd(), st.lastEnd(), delivered, completedAt);
            if (complete(sections.get(i), i, cur)) {
                completedAt.putIfAbsent(i, now);
            }
        }
        return new Delivery(new State(st.built(), st.buildEnd(), st.lastEnd(), delivered, completedAt), amount - left);
    }

    /** Baufortschritt bis {@code now} (auch über mehrere Abschnitte, wenn lange niemand nachgesehen hat). */
    public static State advance(List<Section> sections, State st, long now) {
        int built = st.built();
        long buildEnd = st.buildEnd();
        long lastEnd = st.lastEnd();
        while (built < sections.size()) {
            if (buildEnd > 0) {
                if (now < buildEnd) {
                    break;
                }
                built++;
                lastEnd = buildEnd;
                buildEnd = 0;
                continue;
            }
            Long done = st.completedAt().get(built);
            if (done == null) {
                break;
            }
            buildEnd = Math.max(lastEnd, done) + sections.get(built).buildTicks();
        }
        return new State(built, buildEnd, lastEnd, st.delivered(), st.completedAt());
    }

    private static Map<Integer, Map<String, Long>> copy(Map<Integer, Map<String, Long>> m) {
        Map<Integer, Map<String, Long>> out = new HashMap<>();
        m.forEach((k, v) -> out.put(k, new HashMap<>(v)));
        return out;
    }
}
