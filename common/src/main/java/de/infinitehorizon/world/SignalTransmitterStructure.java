package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

/**
 * {@code signal_transmitter} (13.4, Pluto): Halle {@code hall} (Breite × Tiefe × Höhe) aus {@code wall} (unzerstörbar) mit
 * {@code core} in der Mitte, Öffnung in der Südwand ({@code entrance_width} × {@code entrance_height}), von dort ein
 * Treppentunnel aus {@code tunnel_block} (gleiche Maße) nach Süden bis zur Oberfläche, oben ein Ring
 * ({@code marker_size} × {@code marker_size}) aus {@code wall}. Die Mitte der Hallensohle liegt auf Höhe {@code y}.
 */
public final class SignalTransmitterStructure extends Structure {

    public static final MapCodec<SignalTransmitterStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.INT.listOf(3, 3).fieldOf("hall").forGetter(s -> java.util.List.of(s.width, s.depth, s.height)),
            Codec.INT.fieldOf("y").forGetter(s -> s.y),
            Codec.INT.fieldOf("entrance_width").forGetter(s -> s.entranceWidth),
            Codec.INT.fieldOf("entrance_height").forGetter(s -> s.entranceHeight),
            Codec.INT.fieldOf("marker_size").forGetter(s -> s.markerSize),
            Identifier.CODEC.fieldOf("wall").forGetter(s -> s.wall),
            Identifier.CODEC.fieldOf("core").forGetter(s -> s.core),
            Identifier.CODEC.fieldOf("tunnel_block").forGetter(s -> s.tunnelBlock)
    ).apply(i, (st, hall, y, ew, eh, ms, wall, core, tunnel) -> new SignalTransmitterStructure(st, hall.get(0), hall.get(1),
            hall.get(2), y, ew, eh, ms, wall, core, tunnel)));

    private final int width;
    private final int depth;
    private final int height;
    private final int y;
    private final int entranceWidth;
    private final int entranceHeight;
    private final int markerSize;
    private final Identifier wall;
    private final Identifier core;
    private final Identifier tunnelBlock;

    private SignalTransmitterStructure(StructureSettings settings, int width, int depth, int height, int y, int entranceWidth,
                                       int entranceHeight, int markerSize, Identifier wall, Identifier core, Identifier tunnelBlock) {
        super(settings);
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.y = y;
        this.entranceWidth = entranceWidth;
        this.entranceHeight = entranceHeight;
        this.markerSize = markerSize;
        this.wall = wall;
        this.core = core;
        this.tunnelBlock = tunnelBlock;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMinBlockX();
        int cz = chunk.getMinBlockZ();
        // Tunnel: je Block nach Süden eine Stufe höher, bis die Sohle die Oberfläche (Generator-Schätzung) erreicht
        int south = cz + depth / 2;
        int steps = 0;
        int maxSteps = context.heightAccessor().getMaxY() - y;
        while (steps < maxSteps) {
            int surface = context.chunkGenerator().getFirstOccupiedHeight(cx, south + steps, Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            if (y + steps >= surface) {
                break;
            }
            steps++;
        }
        Piece piece = new Piece(new BlockPos(cx, y, cz), width, depth, height, entranceWidth, entranceHeight, markerSize, steps,
                wall, core, tunnelBlock);
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), b -> b.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.SIGNAL_TRANSMITTER.get();
    }

    /** Halle, Tunnel und Markierung als ein Stück; die Blöcke je Chunk aus den gespeicherten Maßen. */
    public static final class Piece extends StructurePiece {

        private final BlockPos center;
        private final int width;
        private final int depth;
        private final int height;
        private final int entranceWidth;
        private final int entranceHeight;
        private final int markerSize;
        private final int steps;
        private final Identifier wall;
        private final Identifier core;
        private final Identifier tunnelBlock;

        Piece(BlockPos center, int width, int depth, int height, int entranceWidth, int entranceHeight, int markerSize, int steps,
              Identifier wall, Identifier core, Identifier tunnelBlock) {
            super(ModWorldgen.SIGNAL_TRANSMITTER_PIECE.get(), 0, box(center, width, depth, height, entranceWidth, entranceHeight,
                    markerSize, steps));
            this.center = center;
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.entranceWidth = entranceWidth;
            this.entranceHeight = entranceHeight;
            this.markerSize = markerSize;
            this.steps = steps;
            this.wall = wall;
            this.core = core;
            this.tunnelBlock = tunnelBlock;
        }

        public Piece(CompoundTag tag) {
            super(ModWorldgen.SIGNAL_TRANSMITTER_PIECE.get(), tag);
            int[] c = tag.getIntArray("center").orElse(new int[3]);
            this.center = new BlockPos(c[0], c[1], c[2]);
            this.width = tag.getIntOr("width", 1);
            this.depth = tag.getIntOr("depth", 1);
            this.height = tag.getIntOr("height", 1);
            this.entranceWidth = tag.getIntOr("entrance_width", 1);
            this.entranceHeight = tag.getIntOr("entrance_height", 1);
            this.markerSize = tag.getIntOr("marker_size", 1);
            this.steps = tag.getIntOr("steps", 0);
            this.wall = Identifier.parse(tag.getStringOr("wall", "minecraft:bedrock"));
            this.core = Identifier.parse(tag.getStringOr("core", "minecraft:bedrock"));
            this.tunnelBlock = Identifier.parse(tag.getStringOr("tunnel_block", "minecraft:packed_ice"));
        }

        private static BoundingBox box(BlockPos c, int w, int d, int h, int ew, int eh, int ms, int steps) {
            int reach = Math.max(w, ms) / 2 + 1;
            return new BoundingBox(c.getX() - reach, c.getY() - 1, c.getZ() - d / 2 - 1,
                    c.getX() + reach, c.getY() + Math.max(h, steps + eh + 2), c.getZ() + d / 2 + steps + ms);
        }

        /** Mitte der Hallensohle. */
        public BlockPos center() {
            return center;
        }

        /** Lage des Senderkerns (Mitte, auf der Sohle). */
        public BlockPos corePos() {
            return center.above();
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.put("center", new net.minecraft.nbt.IntArrayTag(new int[]{center.getX(), center.getY(), center.getZ()}));
            tag.putInt("width", width);
            tag.putInt("depth", depth);
            tag.putInt("height", height);
            tag.putInt("entrance_width", entranceWidth);
            tag.putInt("entrance_height", entranceHeight);
            tag.putInt("marker_size", markerSize);
            tag.putInt("steps", steps);
            tag.putString("wall", wall.toString());
            tag.putString("core", core.toString());
            tag.putString("tunnel_block", tunnelBlock.toString());
        }

        private static BlockState state(Identifier id) {
            return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
        }

        private void put(WorldGenLevel level, BoundingBox chunkBox, BlockPos p, BlockState s) {
            if (chunkBox.isInside(p)) {
                level.setBlock(p, s, Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            BlockState wallState = state(wall);
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState tunnel = state(tunnelBlock);
            int x0 = center.getX() - width / 2;
            int z0 = center.getZ() - depth / 2;
            int y0 = center.getY();
            int halfE = entranceWidth / 2;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            // Halle: Hülle aus der Legierung, innen Luft; Öffnung in der Südwand
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    for (int dy = 0; dy < height; dy++) {
                        p.set(x0 + dx, y0 + dy, z0 + dz);
                        boolean shell = dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1 || dy == 0 || dy == height - 1;
                        boolean opening = dz == depth - 1 && Math.abs(x0 + dx - center.getX()) <= halfE
                                && dy >= 1 && dy <= entranceHeight;
                        put(level, chunkBox, p, shell && !opening ? wallState : air);
                    }
                }
            }
            put(level, chunkBox, corePos(), state(core));
            // Treppentunnel nach Süden: Sohle steigt je Block um 1, Hülle aus Eis, innen Luft
            int zs = z0 + depth;
            for (int k = 0; k <= steps; k++) {
                int floor = y0 + k;
                for (int dx = -halfE - 1; dx <= halfE + 1; dx++) {
                    for (int dy = 0; dy <= entranceHeight + 1; dy++) {
                        p.set(center.getX() + dx, floor + dy, zs + k);
                        boolean inside = Math.abs(dx) <= halfE && dy >= 1 && dy <= entranceHeight;
                        put(level, chunkBox, p, inside ? air : tunnel);
                    }
                }
            }
            // Markierung: Ring am oberen Ende des Tunnels, auf Höhe der Oberfläche
            int half = markerSize / 2;
            int mz = zs + steps;
            int my = y0 + steps;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    int z = mz + dz;
                    // nicht in den Gang des Tunnels setzen
                    boolean passage = Math.abs(dx) <= halfE && z >= zs && z <= zs + steps
                            && my >= y0 + (z - zs) + 1 && my <= y0 + (z - zs) + entranceHeight;
                    if ((Math.abs(dx) == half || Math.abs(dz) == half) && !passage) {
                        put(level, chunkBox, p.set(center.getX() + dx, my, z), wallState);
                    }
                }
            }
        }
    }
}
