package de.infinitehorizon.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.List;

/**
 * Chunk-Attachment {@code sampled_positions} (11.2, 3A.4): Mengen gepackter Positionen ({@code BlockPos#asLong}).
 * Einträge bleiben nach dem Abbau des Blocks bestehen.
 * <p>
 * SPEC-GAP #67: Beprobte und von Spielern gesetzte Positionen stehen getrennt ({@code positions}, {@code placed}),
 * damit die Probenzange „schon beprobt“ und „keine verwertbare Probe“ unterscheiden kann (27.7 M4b).
 */
public final class SampledPositions {

    public static final MapCodec<SampledPositions> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(s -> List.copyOf(s.sampled)),
            Codec.LONG.listOf().optionalFieldOf("placed", List.of()).forGetter(s -> List.copyOf(s.placed))
    ).apply(i, SampledPositions::new));
    public static final Codec<SampledPositions> CODEC = MAP_CODEC.codec();

    private final LongOpenHashSet sampled;
    private final LongOpenHashSet placed;

    public SampledPositions() {
        this(List.of(), List.of());
    }

    private SampledPositions(List<Long> sampled, List<Long> placed) {
        this.sampled = new LongOpenHashSet(sampled);
        this.placed = new LongOpenHashSet(placed);
    }

    /** Schon mit der Probenzange beprobt? */
    public boolean isSampled(long pos) {
        return sampled.contains(pos);
    }

    /** Von einem Spieler gesetzt (nie beprobbar)? */
    public boolean isPlaced(long pos) {
        return placed.contains(pos);
    }

    /** Merkt eine beprobte Position; true, wenn sie neu war. */
    public boolean markSampled(long pos) {
        return sampled.add(pos);
    }

    /** Merkt eine von einem Spieler gesetzte Position; true, wenn sie neu war. */
    public boolean markPlaced(long pos) {
        return placed.add(pos);
    }

    public boolean isEmpty() {
        return sampled.isEmpty() && placed.isEmpty();
    }
}
