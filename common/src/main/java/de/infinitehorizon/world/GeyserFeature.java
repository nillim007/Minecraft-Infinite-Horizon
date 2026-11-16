package de.infinitehorizon.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature {@code geyser} (13.4, Enceladus): setzt den Geysir-Block {@code block} in den obersten Block der Oberfläche
 * (Platzierung über die Heightmap: {@code origin} ist der erste Luftblock darüber).
 */
public record GeyserFeature(Identifier block) implements Feature {

    public static final MapCodec<GeyserFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("block").forGetter(GeyserFeature::block)
    ).apply(i, GeyserFeature::new));

    @Override
    public MapCodec<GeyserFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos ground = origin.below();
        BlockState s = level.getBlockState(ground);
        if (s.isAir() || !s.getFluidState().isEmpty()) {
            return false;
        }
        level.setBlock(ground, BuiltInRegistries.BLOCK.getValue(block).defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
}
