package de.sternenwerft.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Fluidkugel (13.4, Ölblase): füllt eine Kugel mit Fluid-Quellblöcken. Ersetzt alles außer Grundgestein, Fluiden und
 * Luft (Luft bleibt, damit Höhlen nicht fluten). JSON: {@code {"type": "sternenwerft:fluid_sphere", "fluid": <block>,
 * "radius": <int provider>}}.
 */
public record FluidSphereFeature(BlockState fluid, IntProvider radius) implements Feature {

    public static final MapCodec<FluidSphereFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("fluid").forGetter(FluidSphereFeature::fluid),
            IntProviders.CODEC.fieldOf("radius").forGetter(FluidSphereFeature::radius)
    ).apply(i, FluidSphereFeature::new));

    @Override
    public MapCodec<FluidSphereFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int r = radius.sample(random);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) {
                        continue;
                    }
                    pos.setWithOffset(origin, dx, dy, dz);
                    BlockState current = level.getBlockState(pos);
                    if (current.isAir() || current.is(Blocks.BEDROCK) || !current.getFluidState().isEmpty()) {
                        continue;
                    }
                    level.setBlock(pos, fluid, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
