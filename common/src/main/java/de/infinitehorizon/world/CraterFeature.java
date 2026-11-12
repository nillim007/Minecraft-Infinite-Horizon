package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Kleiner Krater (13.4, {@code small_crater}): Radius aus {@code radius}, Tiefe = Radius / {@code depth_divisor},
 * Rand {@code rim_height} Blöcke aus dem Oberflächenblock. JSON:
 * {@code {"type": "infinite_horizon:crater", "radius": <int provider>, "depth_divisor": 3, "rim_height": 1}}.
 */
public record CraterFeature(IntProvider radius, int depthDivisor, int rimHeight) implements Feature {

    public static final MapCodec<CraterFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IntProviders.CODEC.fieldOf("radius").forGetter(CraterFeature::radius),
            Codec.INT.fieldOf("depth_divisor").forGetter(CraterFeature::depthDivisor),
            Codec.INT.fieldOf("rim_height").forGetter(CraterFeature::rimHeight)
    ).apply(i, CraterFeature::new));

    @Override
    public MapCodec<CraterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int r = radius.sample(random);
        int depth = Math.max(1, r / Math.max(1, depthDivisor));
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                CraterShape.column(level, origin.getX() + dx, origin.getZ() + dz, origin.getX(), origin.getZ(), r, depth,
                        rimHeight, 0, 0, null);
            }
        }
        return true;
    }
}
