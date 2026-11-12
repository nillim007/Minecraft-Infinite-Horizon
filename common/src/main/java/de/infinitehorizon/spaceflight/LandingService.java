package de.infinitehorizon.spaceflight;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

/**
 * Landeplatz (12.5): spiralförmig im Radius {@code radius} um X/Z ein fester Block ohne Fluid darüber mit
 * {@code air_blocks_above} Luftblöcken. Findet sich keiner, entsteht eine Plattform ({@code fallback_platform_size})
 * aus dem Gestein des Planeten auf der Fluidoberfläche. Werte aus spaceflight.json ({@code landing_search}).
 */
public final class LandingService {

    /** Suchwerte aus spaceflight.json. */
    public record Search(int radius, int airAbove, int platformSize) {

        public static Search from(BalanceManager balance) {
            JsonObject o = balance.entry("spaceflight", "landing_search")
                    .orElseThrow(() -> new IllegalStateException("spaceflight.json: landing_search fehlt"));
            return new Search(o.get("radius").getAsInt(), o.get("air_blocks_above").getAsInt(),
                    o.get("fallback_platform_size").getAsInt());
        }
    }

    private LandingService() {
    }

    /** Position, an der ein Spieler steht (Füße), für X/Z in {@code level}. */
    public static BlockPos find(ServerLevel level, int x, int z) {
        Search s = Search.from(BalanceManager.get());
        // Spirale: Ring für Ring von innen nach außen
        for (int r = 0; r <= s.radius(); r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos feet = standable(level, x + dx, z + dz, s.airAbove());
                    if (feet != null) {
                        return feet;
                    }
                }
            }
        }
        return platform(level, x, z, s.platformSize());
    }

    private static BlockPos standable(ServerLevel level, int x, int z, int airAbove) {
        level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos ground = new BlockPos(x, y - 1, z);
        BlockState below = level.getBlockState(ground);
        if (below.isAir() || !below.getFluidState().isEmpty() || level.isOutsideBuildHeight(ground)) {
            return null;
        }
        for (int i = 0; i < airAbove; i++) {
            BlockPos p = ground.above(i + 1);
            if (!level.getBlockState(p).isAir() || level.isOutsideBuildHeight(p)) {
                return null;
            }
        }
        return ground.above();
    }

    private static BlockPos platform(ServerLevel level, int x, int z, int size) {
        int y = Math.max(level.getMinY() + 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        BlockState rock = level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator n
                ? n.generatorSettings().value().defaultBlock() : Blocks.STONE.defaultBlockState();
        int half = size / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), rock, Block.UPDATE_ALL);
            }
        }
        return new BlockPos(x, y, z);
    }
}
