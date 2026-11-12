package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.List;
import java.util.Optional;

/**
 * Großkrater auf dem Mond (13.4, {@code great_crater}): Radius aus {@code radius}, Tiefe = Radius /
 * {@code depth_divisor}, Zentralberg mit Radius / {@code peak_radius_divisor}, Rand. „Erze ×{@code ore_multiplier}“: In
 * jedem Chunk, dessen Mitte im Krater liegt, setzt die Struktur die Erzadern des Bioms noch einmal (je weiterer Stufe).
 */
public final class GreatCraterStructure extends Structure {

    public static final MapCodec<GreatCraterStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            IntProviders.CODEC.fieldOf("radius").forGetter(s -> s.radius),
            Codec.INT.fieldOf("ore_multiplier").forGetter(s -> s.oreMultiplier),
            Codec.INT.fieldOf("depth_divisor").forGetter(s -> s.depthDivisor),
            Codec.INT.fieldOf("rim_height").forGetter(s -> s.rimHeight),
            Codec.INT.fieldOf("peak_radius_divisor").forGetter(s -> s.peakRadiusDivisor),
            Codec.INT.fieldOf("peak_height_divisor").forGetter(s -> s.peakHeightDivisor)
    ).apply(i, GreatCraterStructure::new));

    private final IntProvider radius;
    private final int oreMultiplier;
    private final int depthDivisor;
    private final int rimHeight;
    private final int peakRadiusDivisor;
    private final int peakHeightDivisor;

    public GreatCraterStructure(StructureSettings settings, IntProvider radius, int oreMultiplier, int depthDivisor,
                                int rimHeight, int peakRadiusDivisor, int peakHeightDivisor) {
        super(settings);
        this.radius = radius;
        this.oreMultiplier = oreMultiplier;
        this.depthDivisor = depthDivisor;
        this.rimHeight = rimHeight;
        this.peakRadiusDivisor = peakRadiusDivisor;
        this.peakHeightDivisor = peakHeightDivisor;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int r = radius.sample(context.random());
        int depth = Math.max(1, r / Math.max(1, depthDivisor));
        int peakR = peakRadiusDivisor > 0 ? r / peakRadiusDivisor : 0;
        // Zentralberg: ragt vom Kraterboden bis (peak_height_divisor − 1) / peak_height_divisor der Tiefe auf
        int peakH = peakHeightDivisor > 0 ? depth * (peakHeightDivisor - 1) / peakHeightDivisor : 0;
        BlockPos center = new BlockPos(x, y, z);
        Piece piece = new Piece(center, r, depth, rimHeight, peakR, peakH, oreMultiplier,
                context.heightAccessor().getMinY(), context.heightAccessor().getMaxY());
        return Optional.of(new GenerationStub(center, b -> b.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.GREAT_CRATER.get();
    }

    /** Ein Stück für den ganzen Krater; formt beim Generieren jedes Chunks dessen Spalten. */
    public static final class Piece extends StructurePiece {

        private final int cx;
        private final int cy;
        private final int cz;
        private final int radius;
        private final int depth;
        private final int rim;
        private final int peakRadius;
        private final int peakHeight;
        private final int oreMultiplier;

        Piece(BlockPos center, int radius, int depth, int rim, int peakRadius, int peakHeight, int oreMultiplier,
              int minY, int maxY) {
            super(ModWorldgen.GREAT_CRATER_PIECE.get(), 0, new BoundingBox(center.getX() - radius - 2, minY,
                    center.getZ() - radius - 2, center.getX() + radius + 2, maxY, center.getZ() + radius + 2));
            this.cx = center.getX();
            this.cy = center.getY();
            this.cz = center.getZ();
            this.radius = radius;
            this.depth = depth;
            this.rim = rim;
            this.peakRadius = peakRadius;
            this.peakHeight = peakHeight;
            this.oreMultiplier = oreMultiplier;
        }

        public Piece(CompoundTag tag) {
            super(ModWorldgen.GREAT_CRATER_PIECE.get(), tag);
            this.cx = tag.getIntOr("cx", 0);
            this.cy = tag.getIntOr("cy", 0);
            this.cz = tag.getIntOr("cz", 0);
            this.radius = tag.getIntOr("radius", 0);
            this.depth = tag.getIntOr("depth", 0);
            this.rim = tag.getIntOr("rim", 0);
            this.peakRadius = tag.getIntOr("peak_radius", 0);
            this.peakHeight = tag.getIntOr("peak_height", 0);
            this.oreMultiplier = tag.getIntOr("ore_multiplier", 1);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("cx", cx);
            tag.putInt("cy", cy);
            tag.putInt("cz", cz);
            tag.putInt("radius", radius);
            tag.putInt("depth", depth);
            tag.putInt("rim", rim);
            tag.putInt("peak_radius", peakRadius);
            tag.putInt("peak_height", peakHeight);
            tag.putInt("ore_multiplier", oreMultiplier);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            for (int x = chunkBox.minX(); x <= chunkBox.maxX(); x++) {
                for (int z = chunkBox.minZ(); z <= chunkBox.maxZ(); z++) {
                    CraterShape.column(level, x, z, cx, cz, radius, depth, rim, peakRadius, peakHeight, chunkBox);
                }
            }
            int mx = chunk.getMiddleBlockX();
            int mz = chunk.getMiddleBlockZ();
            if ((long) (mx - cx) * (mx - cx) + (long) (mz - cz) * (mz - cz) > (long) radius * radius) {
                return;
            }
            // Erze des Bioms ein weiteres Mal (13.4 „Erze ×2“)
            BlockPos origin = new BlockPos(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ());
            List<HolderSet<PlacedFeature>> steps =
                    level.getBiome(new BlockPos(mx, cy, mz)).value().getGenerationSettings().features();
            int ores = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
            if (steps.size() <= ores) {
                return;
            }
            for (int n = 1; n < oreMultiplier; n++) {
                for (Holder<PlacedFeature> f : steps.get(ores)) {
                    f.value().place(level, generator, random, origin);
                }
            }
        }
    }
}
