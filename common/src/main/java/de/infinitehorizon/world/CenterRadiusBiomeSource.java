package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * Biomquelle {@code center_radius} (13.1, Pluto): im Kreis mit {@code radius} um {@code center} (X/Z) liegt
 * {@code inner_biome} (Herzgletscher), außerhalb entscheidet die Biomquelle {@code outside} (multi_noise).
 */
public class CenterRadiusBiomeSource extends BiomeSource {

    public static final MapCodec<CenterRadiusBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.listOf(2, 2).fieldOf("center").forGetter(s -> List.of(s.cx, s.cz)),
            Codec.INT.fieldOf("radius").forGetter(s -> s.radius),
            Biome.CODEC.fieldOf("inner_biome").forGetter(s -> s.inner),
            BiomeSource.CODEC.fieldOf("outside").forGetter(s -> s.outside)
    ).apply(i, (c, r, in, out) -> new CenterRadiusBiomeSource(c.get(0), c.get(1), r, in, out)));

    private final int cx;
    private final int cz;
    private final int radius;
    private final Holder<Biome> inner;
    private final BiomeSource outside;

    public CenterRadiusBiomeSource(int cx, int cz, int radius, Holder<Biome> inner, BiomeSource outside) {
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
        this.inner = inner;
        this.outside = outside;
    }

    /** Liegt die Blockspalte im Kreis? (reine Rechnung) */
    public static boolean inside(int x, int z, int cx, int cz, int radius) {
        long dx = x - cx;
        long dz = z - cz;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(Stream.of(inner), outside.possibleBiomes().stream()).distinct();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        BiomeResolver out = outside.createResolver(sampler);
        return (qx, qy, qz) -> inside(qx << 2, qz << 2, cx, cz, radius) ? inner : out.getNoiseBiome(qx, qy, qz);
    }
}
