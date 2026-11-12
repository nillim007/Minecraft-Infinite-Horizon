package de.infinitehorizon.environment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spieler-Attachment {@code hazard_state} (3A.4): Belastung je Gefahr in Ticks (8.2, 0 bis {@code buildup_ticks}).
 * Wird mit dem Spieler gespeichert und beim Tod nicht übernommen (also 0). Spätere Felder (O₂-Zähler 9.1,
 * Venus-Hitzezeit) kommen optional dazu.
 */
public final class HazardState {

    public static final MapCodec<HazardState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("buildup", Map.of()).forGetter(HazardState::encode),
            Codec.LONG.optionalFieldOf("suit_oxygen_counter", 0L).forGetter(HazardState::suitOxygenCounter)
    ).apply(i, (map, counter) -> {
        HazardState s = decode(map);
        s.suitOxygenCounter = counter;
        return s;
    }));
    public static final Codec<HazardState> CODEC = MAP_CODEC.codec();

    private final EnumMap<Hazard, Integer> buildup = new EnumMap<>(Hazard.class);
    /** Zähler für den O₂-Verbrauch des Anzugs in 1/1.000 mB (9.1). */
    private long suitOxygenCounter;

    public long suitOxygenCounter() {
        return suitOxygenCounter;
    }

    public void setSuitOxygenCounter(long value) {
        suitOxygenCounter = value;
    }

    public int buildup(Hazard h) {
        return buildup.getOrDefault(h, 0);
    }

    public void setBuildup(Hazard h, int ticks) {
        if (ticks <= 0) {
            buildup.remove(h);
        } else {
            buildup.put(h, ticks);
        }
    }

    /** Belastung 0–1 (Wert / {@code buildup_ticks}). */
    public double fraction(Hazard h, int buildupTicks) {
        return buildupTicks <= 0 ? 0 : Math.min(1.0, buildup(h) / (double) buildupTicks);
    }

    public void reset() {
        buildup.clear();
        suitOxygenCounter = 0;
    }

    private Map<String, Integer> encode() {
        Map<String, Integer> out = new LinkedHashMap<>();
        buildup.forEach((h, v) -> out.put(h.id(), v));
        return out;
    }

    private static HazardState decode(Map<String, Integer> map) {
        HazardState s = new HazardState();
        map.forEach((k, v) -> {
            try {
                s.setBuildup(Hazard.byId(k), v);
            } catch (IllegalArgumentException unknown) {
                // Gefahr aus einer neueren Version: ignorieren
            }
        });
        return s;
    }

    /**
     * Ein Prüftakt (8.2): mit Gefahr {@code +interval} bis {@code buildupTicks}, ohne Gefahr
     * {@code −interval × buildupTicks / decayTicks} bis 0. Reine Funktion.
     */
    public static int step(int current, boolean active, int interval, int buildupTicks, int decayTicks) {
        if (active) {
            return Math.min(buildupTicks, current + interval);
        }
        int remove = decayTicks <= 0 ? buildupTicks : Math.ceilDiv(interval * buildupTicks, decayTicks);
        return Math.max(0, current - remove);
    }
}
