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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Biomquelle nach X-Koordinate (13.1, 13.4 Merkur): Bänder mit {@code x_min}/{@code x_max} (Blöcke), dazu seltene
 * Einsprengsel ({@code rare_inclusions}) in einem festen Zellraster. Die Regeln stehen als reine Funktion in
 * {@link #bandIndex} und {@link #inclusionAt}.
 */
public class XBandsBiomeSource extends BiomeSource implements BiomeResolver {

    public record Band(Holder<Biome> biome, Optional<Integer> xMin, Optional<Integer> xMax) {
        public static final Codec<Band> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("biome").forGetter(Band::biome),
                Codec.INT.optionalFieldOf("x_min").forGetter(Band::xMin),
                Codec.INT.optionalFieldOf("x_max").forGetter(Band::xMax)).apply(i, Band::new));
    }

    /** Seltenes Einsprengsel: in {@code chance} der Zellen (Kantenlänge {@code cell} Blöcke) eine Scheibe mit Radius {@code radius}. */
    public record Inclusion(Holder<Biome> biome, int cell, int radius, double chance) {
        public static final Codec<Inclusion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("biome").forGetter(Inclusion::biome),
                Codec.INT.fieldOf("cell").forGetter(Inclusion::cell),
                Codec.INT.fieldOf("radius").forGetter(Inclusion::radius),
                Codec.DOUBLE.fieldOf("chance").forGetter(Inclusion::chance)).apply(i, Inclusion::new));
    }

    public static final MapCodec<XBandsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Band.CODEC.listOf().fieldOf("bands").forGetter(s -> s.bands),
            Inclusion.CODEC.listOf().optionalFieldOf("rare_inclusions", List.of()).forGetter(s -> s.inclusions)
    ).apply(i, XBandsBiomeSource::new));

    private final List<Band> bands;
    private final List<Inclusion> inclusions;

    public XBandsBiomeSource(List<Band> bands, List<Inclusion> inclusions) {
        this.bands = List.copyOf(bands);
        this.inclusions = List.copyOf(inclusions);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(bands.stream().map(Band::biome), inclusions.stream().map(Inclusion::biome)).distinct();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return this;
    }

    /** Biom an Quart-Koordinaten (je 4 Blöcke, wie Vanilla). */
    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz) {
        int x = qx << 2;
        int z = qz << 2;
        for (Inclusion inc : inclusions) {
            if (inclusionAt(x, z, inc.cell(), inc.radius(), inc.chance())) {
                return inc.biome();
            }
        }
        int[] mins = new int[bands.size()];
        int[] maxs = new int[bands.size()];
        for (int k = 0; k < bands.size(); k++) {
            mins[k] = bands.get(k).xMin().orElse(Integer.MIN_VALUE);
            maxs[k] = bands.get(k).xMax().orElse(Integer.MAX_VALUE);
        }
        return bands.get(bandIndex(x, mins, maxs)).biome();
    }

    public static int bandIndex(int x, int[] mins, int[] maxs) {
        return XBands.bandIndex(x, mins, maxs);
    }

    public static boolean inclusionAt(int x, int z, int cell, int radius, double chance) {
        return XBands.inclusionAt(x, z, cell, radius, chance);
    }
}
