package de.infinitehorizon.research;

import com.google.gson.JsonObject;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * FP-Buchhaltung (11.1): Werte in Hundertsteln, Abnahme je Probenart bzw. Strukturtyp. Alle Zahlen kommen aus
 * {@code balance/research.json} ({@link Rules#from}); die Rechnung ist eine reine Funktion (JUnit).
 * <p>
 * Wert = {@code fp × scale}, ab der {@code full_count + 1}. Probe derselben Art bzw. Instanz desselben Strukturtyps
 * {@code × factor_after}, abgerundet.
 */
public final class FpLedger {

    /** Regeln aus research.json. {@code ringFp}: Ring → FP je Probe ({@code sample_ring_<n>}). */
    public record Rules(int scale, int fullCount, double factorAfter, boolean deepslateSameType, int overworldRing,
                        Map<Integer, Integer> ringFp, int explorationFp) {

        public static Rules from(BalanceManager balance) {
            JsonObject storage = balance.entry("research", "fp_storage").orElse(new JsonObject());
            JsonObject dim = balance.entry("research", "diminishing_returns").orElse(new JsonObject());
            JsonObject ring = balance.entry("research", "sample_ring_rules").orElse(new JsonObject());
            Map<Integer, Integer> fp = new HashMap<>();
            for (int r = 0; ; r++) {
                var e = balance.entry("research", "sample_ring_" + r);
                if (e.isEmpty()) {
                    break;
                }
                fp.put(r, e.get().has("fp") ? e.get().get("fp").getAsInt() : 0);
            }
            int exploration = balance.entry("research", "exploration_target")
                    .filter(o -> o.has("fp")).map(o -> o.get("fp").getAsInt()).orElse(0);
            return new Rules(
                    storage.has("scale") ? storage.get("scale").getAsInt() : 1,
                    dim.has("full_count") ? dim.get("full_count").getAsInt() : Integer.MAX_VALUE,
                    dim.has("factor_after") ? dim.get("factor_after").getAsDouble() : 1.0,
                    dim.has("deepslate_variants_same_type") && dim.get("deepslate_variants_same_type").getAsBoolean(),
                    ring.has("overworld_ring") ? ring.get("overworld_ring").getAsInt() : 0,
                    Map.copyOf(fp), exploration);
        }
    }

    /** Präfix der Tiefenschiefer-Varianten (11.2: zählen als dieselbe Probenart). */
    private static final String DEEPSLATE = "deepslate_";

    private FpLedger() {
    }

    // ---------------------------------------------------------------- reine Rechnung

    /** Hundertstel für {@code fp} ganze FP, wenn vorher schon {@code countBefore} derselben Art gezählt wurden. */
    public static int value(int fp, int countBefore, Rules r) {
        double v = (double) fp * r.scale() * (countBefore < r.fullCount() ? 1.0 : r.factorAfter());
        // kleine Toleranz gegen Rundungsfehler (5 × 100 × 0,1 = 50,000…01 bzw. 49,999…)
        return (int) Math.floor(v + 1e-9);
    }

    /** Probenart = Block-ID + Dimension; {@code deepslate_}-Varianten zählen wie die normale Variante (11.2). */
    public static String sampleKey(Identifier block, Identifier dimension, Rules r) {
        String path = block.getPath();
        if (r.deepslateSameType() && path.startsWith(DEEPSLATE)) {
            path = path.substring(DEEPSLATE.length());
        }
        return block.getNamespace() + ":" + path + "@" + dimension;
    }

    /** FP einer Probe aus diesem Ring (0 = keine FP). */
    public static int ringFp(int ring, Rules r) {
        return r.ringFp().getOrDefault(ring, 0);
    }

    /** Schreibt eine analysierte Probe gut (Zähler + FP); gibt die Hundertstel zurück. */
    public static int creditSample(AgencyProgress p, String sampleKey, int ring, Rules r) {
        int fp = ringFp(ring, r);
        if (fp <= 0) {
            return 0;
        }
        int value = value(fp, p.sampleCount(sampleKey), r);
        p.countSample(sampleKey);
        p.addFp(value);
        return value;
    }

    /** Schreibt ein neu gefundenes Erkundungsziel gut (Zähler je Strukturtyp + FP); gibt die Hundertstel zurück. */
    public static int creditStructure(AgencyProgress p, Identifier structureType, Rules r) {
        String key = structureType.toString();
        int value = value(r.explorationFp(), p.structureCount(key), r);
        p.countStructure(key);
        p.addFp(value);
        return value;
    }

    /** FP aus Hundertsteln mit 2 Nachkommastellen (11.3), z. B. {@code 12.34}. */
    public static String format(int hundredths) {
        return String.format(java.util.Locale.ROOT, "%d.%02d", hundredths / 100, Math.abs(hundredths % 100));
    }

    // ---------------------------------------------------------------- Ring einer Dimension

    /**
     * Ring einer Probe nach ihrer Dimension (11.1): Overworld = {@code overworld_ring}, sonst {@code ring} aus dem
     * Planeten-Eintrag; Nether, End und Dimensionen ohne Eintrag: leer (keine FP).
     */
    public static OptionalInt ringOf(Identifier dimension, PlanetRegistry planets, Rules r) {
        if (dimension.equals(PlanetResolver.EARTH)) {
            return OptionalInt.of(r.overworldRing());
        }
        Planet p = planets.planet(dimension).orElse(null);
        return p == null || p.ring() < 0 ? OptionalInt.empty() : OptionalInt.of(p.ring());
    }

    /** Gibt diese Probe FP (Eingangsslot des Analysators, 6.1)? */
    public static boolean givesFp(SampleData sample, PlanetRegistry planets, Rules r) {
        OptionalInt ring = ringOf(sample.dimension(), planets, r);
        return ring.isPresent() && ringFp(ring.getAsInt(), r) > 0;
    }

    private static volatile BalanceManager rulesSource;
    private static volatile Rules rules;

    /** Regeln der laufenden Balance (nach {@code /reload} neu). */
    public static Rules rules() {
        BalanceManager b = BalanceManager.get();
        if (rules == null || rulesSource != b) {
            rules = Rules.from(b);
            rulesSource = b;
        }
        return rules;
    }
}
