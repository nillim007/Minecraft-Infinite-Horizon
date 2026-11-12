package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

/**
 * Lavaröhre (13.4, {@code lava_tube}): gerader Tunnel mit Querschnitt {@code size} × {@code size} in zufälliger
 * Himmelsrichtung, Länge aus {@code length}, Höhe aus {@code y}. Ab M4 ein Erkundungsziel.
 */
public final class LavaTubeStructure extends Structure {

    public static final MapCodec<LavaTubeStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.INT.fieldOf("size").forGetter(s -> s.size),
            IntProviders.CODEC.fieldOf("length").forGetter(s -> s.length),
            IntProviders.CODEC.fieldOf("y").forGetter(s -> s.y)
    ).apply(i, LavaTubeStructure::new));

    private final int size;
    private final IntProvider length;
    private final IntProvider y;

    public LavaTubeStructure(StructureSettings settings, int size, IntProvider length, IntProvider y) {
        super(settings);
        this.size = size;
        this.length = length;
        this.y = y;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        BlockPos start = new BlockPos(chunk.getMiddleBlockX(), y.sample(random), chunk.getMiddleBlockZ());
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Piece piece = new Piece(start, dir, length.sample(random), size);
        return Optional.of(new GenerationStub(start, b -> b.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.LAVA_TUBE.get();
    }

    /** Der ganze Tunnel als ein Stück (Quader entlang der Richtung); die Basisklasse speichert die BoundingBox. */
    public static final class Piece extends StructurePiece {

        Piece(BlockPos start, Direction dir, int length, int size) {
            super(ModWorldgen.LAVA_TUBE_PIECE.get(), 0, box(start, dir, length, size));
        }

        public Piece(CompoundTag tag) {
            super(ModWorldgen.LAVA_TUBE_PIECE.get(), tag);
        }

        private static BoundingBox box(BlockPos start, Direction dir, int length, int size) {
            int half = size / 2;
            BlockPos end = start.relative(dir, length - 1);
            int ax = dir.getAxis() == Direction.Axis.X ? 0 : half;
            int az = dir.getAxis() == Direction.Axis.Z ? 0 : half;
            return new BoundingBox(Math.min(start.getX(), end.getX()) - ax, start.getY() - half,
                    Math.min(start.getZ(), end.getZ()) - az, Math.max(start.getX(), end.getX()) + ax,
                    start.getY() - half + size - 1, Math.max(start.getZ(), end.getZ()) + az);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
                for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
                    for (int yy = boundingBox.minY(); yy <= boundingBox.maxY(); yy++) {
                        pos.set(x, yy, z);
                        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }
}
