package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Feste Lage einer Struktur (13.4: „genau eine je Dimension“, z. B. Eisvulkan der Ceres, Spawn-Asteroid, Sender auf
 * Pluto): genau der Chunk, der den Block {@code x}/{@code z} enthält.
 */
public record FixedStructurePlacement(int x, int z) implements StructurePlacement {

    public static final MapCodec<FixedStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(FixedStructurePlacement::x),
            Codec.INT.fieldOf("z").forGetter(FixedStructurePlacement::z)).apply(i, FixedStructurePlacement::new));

    /** Reine Regel: Ist (chunkX, chunkZ) der Chunk des Blocks (x, z)? */
    public static boolean isChunk(int x, int z, int chunkX, int chunkZ) {
        return XBands.fixedChunk(x, z, chunkX, chunkZ);
    }

    @Override
    public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return isChunk(x, z, chunkX, chunkZ);
    }

    @Override
    public MapCodec<? extends StructurePlacement> codec() {
        return CODEC;
    }
}
