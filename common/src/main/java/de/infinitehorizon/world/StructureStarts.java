package de.infinitehorizon.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Strukturen in der Nähe finden, ohne Chunks zu laden (Vulkanausbruch 19, Eisqualle 18). */
public final class StructureStarts {

    private StructureStarts() {
    }

    public static @Nullable Structure structure(ServerLevel level, Identifier id) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(id);
    }

    /** Mitten der Strukturen in geladenen Chunks im Umkreis {@code radius} (waagerecht) um {@code center}. */
    public static List<BlockPos> near(ServerLevel level, Structure structure, BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        ChunkPos c = ChunkPos.containing(center);
        int rc = (radius >> 4) + 1;
        for (int dx = -rc; dx <= rc; dx++) {
            for (int dz = -rc; dz <= rc; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x() + dx, c.z() + dz);
                if (chunk == null) {
                    continue;
                }
                StructureStart start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) {
                    continue;
                }
                BlockPos mid = start.getBoundingBox().getCenter();
                double ddx = mid.getX() - center.getX();
                double ddz = mid.getZ() - center.getZ();
                if (ddx * ddx + ddz * ddz <= (double) radius * radius && !out.contains(mid)) {
                    out.add(mid);
                }
            }
        }
        return out;
    }

    /** Nächste Strukturmitte im Umkreis, sonst null. */
    public static @Nullable BlockPos nearest(ServerLevel level, Structure structure, BlockPos center, int radius) {
        BlockPos best = null;
        for (BlockPos p : near(level, structure, center, radius)) {
            if (best == null || p.distSqr(center) < best.distSqr(center)) {
                best = p;
            }
        }
        return best;
    }
}
