package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

/**
 * Feature {@code ice_shell} (13.1, Europa): füllt {@code y_min}–{@code y_max} im ganzen Chunk mit {@code block}, wo Luft oder
 * Wasser ist. In den {@code excluded_biomes} (Eisspalten) bleiben Risse mit {@code rift_width} Blöcken Breite offen
 * ({@link EuropaRifts}). Ohne Platzierungsregeln läuft es einmal je Chunk.
 */
public record IceShellFeature(Identifier block, int yMin, int yMax, List<Integer> riftWidth, List<Identifier> excludedBiomes)
        implements Feature {

    public static final MapCodec<IceShellFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("block").forGetter(IceShellFeature::block),
            Codec.INT.fieldOf("y_min").forGetter(IceShellFeature::yMin),
            Codec.INT.fieldOf("y_max").forGetter(IceShellFeature::yMax),
            Codec.INT.listOf(2, 2).fieldOf("rift_width").forGetter(IceShellFeature::riftWidth),
            Identifier.CODEC.listOf().optionalFieldOf("excluded_biomes", List.of()).forGetter(IceShellFeature::excludedBiomes)
    ).apply(i, IceShellFeature::new));

    /** Versatz des Seeds für das Breitenrauschen. */
    private static final long WIDTH_SALT = 0x5EAL;

    @Override
    public MapCodec<IceShellFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockState ice = BuiltInRegistries.BLOCK.getValue(block).defaultBlockState();
        SimplexNoise lines = new SimplexNoise(new XoroshiroRandomSource(level.getSeed()));
        SimplexNoise widths = new SimplexNoise(new XoroshiroRandomSource(level.getSeed() ^ WIDTH_SALT));
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> excluded = excludedBiomes.stream()
                .map(id -> ResourceKey.create(Registries.BIOME, id)).toList();
        ChunkPos chunk = ChunkPos.containing(origin);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int mid = (yMin + yMax) / 2;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (!excluded.isEmpty()) {
                    var biome = level.getBiome(pos.set(x, mid, z));
                    if (excluded.stream().anyMatch(biome::is)
                            && EuropaRifts.crack(lines, widths, x, z, riftWidth.get(0), riftWidth.get(1))) {
                        continue;
                    }
                }
                for (int y = yMin; y <= yMax; y++) {
                    BlockState s = level.getBlockState(pos.set(x, y, z));
                    if (s.isAir() || s.is(Blocks.WATER)) {
                        level.setBlock(pos, ice, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }
}
