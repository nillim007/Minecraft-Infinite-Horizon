package de.infinitehorizon.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Kraterform (13.4) für {@link CraterFeature} und {@link GreatCraterStructure}: Schüssel mit Tiefe {@code depth}
 * (Paraboloid, folgt dem Gelände je Spalte), Boden und Rand aus dem Oberflächenblock der Spalte, optional ein
 * Zentralberg. Grundgestein bleibt.
 */
public final class CraterShape {

    private CraterShape() {
    }

    /**
     * Formt die Spalte {@code (x, z)}.
     *
     * @param clip       nur innerhalb dieses Bereichs setzen (Chunk der Struktur), {@code null} = überall
     * @param peakRadius Radius des Zentralbergs (0 = keiner); {@code peakHeight} seine Höhe über dem Kraterboden
     */
    public static void column(WorldGenLevel level, int x, int z, double cx, double cz, int radius, int depth,
                              int rimHeight, int peakRadius, int peakHeight, @Nullable BoundingBox clip) {
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
        if (d > radius + 1.5) {
            return;
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        BlockState surface = level.getBlockState(pos);
        if (surface.isAir() || surface.is(Blocks.BEDROCK)) {
            return;
        }
        if (d > radius) {
            // Rand: rimHeight Blöcke aus dem Oberflächenblock
            for (int i = 1; i <= rimHeight; i++) {
                set(level, pos.setY(top + i), surface, clip);
            }
            return;
        }
        double t = d / radius;
        int floor = top - (int) Math.round(depth * (1 - t * t));
        if (peakRadius > 0 && d <= peakRadius) {
            floor += (int) Math.round(peakHeight * (1 - d / peakRadius));
        }
        BlockState fill = level.getBlockState(pos.setY(Math.min(top, floor) - 4));
        if (fill.isAir()) {
            fill = surface;
        }
        for (int y = Math.max(top, floor); y > Math.min(top, floor); y--) {
            set(level, pos.setY(y), y > floor ? Blocks.AIR.defaultBlockState() : fill, clip);
        }
        set(level, pos.setY(floor), surface, clip);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state, @Nullable BoundingBox clip) {
        if (clip != null && !clip.isInside(pos)) {
            return;
        }
        if (level.isOutsideBuildHeight(pos) || level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return;
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
