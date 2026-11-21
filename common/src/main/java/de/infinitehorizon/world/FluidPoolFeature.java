package de.infinitehorizon.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluidpfütze an der Oberfläche (13.4, Ä31: Ölpfütze statt Ölblase). {@code origin} ist der erste Luftblock über dem
 * Boden (Platzierung über die Heightmap). Eine runde Mulde mit Radius {@code radius}, innen {@code depth} Blöcke tief,
 * am Rand 1 Block, wird mit Quellblöcken gefüllt. Hat die Mulde keinen festen Rand oder Boden (Abhang, Höhle, Wasser),
 * entsteht nichts, damit kein Fluid ausläuft. Pflanzen über der Pfütze verschwinden.
 * JSON: {@code {"type": "infinite_horizon:fluid_pool", "fluid": <block>, "radius": <int provider>, "depth": <int provider>}}.
 */
public record FluidPoolFeature(BlockState fluid, IntProvider radius, IntProvider depth) implements Feature {

    public static final MapCodec<FluidPoolFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("fluid").forGetter(FluidPoolFeature::fluid),
            IntProviders.CODEC.fieldOf("radius").forGetter(FluidPoolFeature::radius),
            IntProviders.CODEC.fieldOf("depth").forGetter(FluidPoolFeature::depth)
    ).apply(i, FluidPoolFeature::new));

    @Override
    public MapCodec<FluidPoolFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int r = radius.sample(random);
        int deep = Math.max(1, depth.sample(random));
        List<BlockPos> cells = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r * r) {
                    continue;
                }
                int cellDepth = 4 * d2 <= r * r ? deep : 1;
                for (int k = 1; k <= cellDepth; k++) {
                    cells.add(origin.offset(dx, -k, dz));
                }
            }
        }
        // Rand und Boden müssen fest sein, sonst läuft das Fluid aus
        for (BlockPos c : cells) {
            if (!solid(level, c)) {
                return false;
            }
            for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos n = c.relative(d);
                if (!cells.contains(n) && !solid(level, n)) {
                    return false;
                }
            }
        }
        for (BlockPos c : cells) {
            level.setBlock(c, fluid, Block.UPDATE_CLIENTS);
            pos.set(c.getX(), origin.getY(), c.getZ());
            BlockState above = level.getBlockState(pos);
            if (!above.isAir() && above.canBeReplaced()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return !cells.isEmpty();
    }

    private static boolean solid(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced();
    }
}
