package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
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
 * Oberflächenstrukturen von Ring 1 (13.4) mit einer gemeinsamen Klasse: {@code giant_volcano} (Kegel bis
 * {@code max_y} mit Lavakrater), {@code volcanic_vent} (kleiner Hügel mit Lavaschlot, Erkundungsziel) und
 * {@code meteorite_field} (mehrere kleine Krater mit Meteoritenblock, Erkundungsziel). Formwerte aus der
 * Struktur-JSON (gen_resources.py); SPEC-GAP #87.
 */
public final class SurfaceStructure extends Structure {

    public enum Kind { GIANT_VOLCANO, VOLCANIC_VENT, METEORITE_FIELD }

    public static final MapCodec<SurfaceStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.xmap(s -> Kind.valueOf(s.toUpperCase(java.util.Locale.ROOT)), k -> k.name().toLowerCase(java.util.Locale.ROOT))
                    .fieldOf("kind").forGetter(s -> s.kind),
            IntProviders.CODEC.fieldOf("radius").forGetter(s -> s.radius),
            Codec.INT.fieldOf("height").forGetter(s -> s.height),
            Codec.INT.optionalFieldOf("divisor", 1).forGetter(s -> s.divisor),
            IntProviders.CODEC.optionalFieldOf("count").forGetter(s -> Optional.ofNullable(s.count)),
            IntProviders.CODEC.optionalFieldOf("part_radius").forGetter(s -> Optional.ofNullable(s.partRadius)),
            Identifier.CODEC.fieldOf("block").forGetter(s -> s.block),
            Identifier.CODEC.fieldOf("core").forGetter(s -> s.core),
            Identifier.CODEC.fieldOf("fluid").forGetter(s -> s.fluid)
    ).apply(i, SurfaceStructure::new));

    private final Kind kind;
    private final IntProvider radius;
    private final int height;
    private final int divisor;
    private final IntProvider count;
    private final IntProvider partRadius;
    private final Identifier block;
    private final Identifier core;
    private final Identifier fluid;

    private SurfaceStructure(StructureSettings settings, Kind kind, IntProvider radius, int height, int divisor,
                             Optional<IntProvider> count, Optional<IntProvider> partRadius, Identifier block, Identifier core,
                             Identifier fluid) {
        super(settings);
        this.kind = kind;
        this.radius = radius;
        this.height = height;
        this.divisor = Math.max(1, divisor);
        this.count = count.orElse(null);
        this.partRadius = partRadius.orElse(null);
        this.block = block;
        this.core = core;
        this.fluid = fluid;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int r = radius.sample(random);
        // Teile: Krater des Meteoritenfelds (dx, dz, Radius je Krater)
        int n = kind == Kind.METEORITE_FIELD && count != null ? count.sample(random) : 0;
        int[] parts = new int[n * 3];
        int maxPart = 0;
        for (int k = 0; k < n; k++) {
            parts[k * 3] = random.nextInt(2 * r + 1) - r;
            parts[k * 3 + 1] = random.nextInt(2 * r + 1) - r;
            parts[k * 3 + 2] = partRadius == null ? 1 : partRadius.sample(random);
            maxPart = Math.max(maxPart, parts[k * 3 + 2]);
        }
        Piece piece = new Piece(kind, new BlockPos(x, ground, z), r, height, divisor, parts, maxPart, block, core, fluid);
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), b -> b.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case GIANT_VOLCANO -> ModWorldgen.GIANT_VOLCANO.get();
            case VOLCANIC_VENT -> ModWorldgen.VOLCANIC_VENT.get();
            case METEORITE_FIELD -> ModWorldgen.METEORITE_FIELD.get();
        };
    }

    /** Die ganze Struktur als ein Stück; Form je Spalte aus Mitte, Grundhöhe und Parametern. */
    public static final class Piece extends StructurePiece {

        private final Kind kind;
        private final BlockPos center;
        private final int radius;
        private final int height;
        private final int divisor;
        private final int[] parts;
        private final Identifier block;
        private final Identifier core;
        private final Identifier fluid;

        Piece(Kind kind, BlockPos center, int radius, int height, int divisor, int[] parts, int maxPart, Identifier block,
              Identifier core, Identifier fluid) {
            super(ModWorldgen.SURFACE_PIECE.get(), 0, box(kind, center, radius, height, maxPart));
            this.kind = kind;
            this.center = center;
            this.radius = radius;
            this.height = height;
            this.divisor = divisor;
            this.parts = parts;
            this.block = block;
            this.core = core;
            this.fluid = fluid;
        }

        public Piece(CompoundTag tag) {
            super(ModWorldgen.SURFACE_PIECE.get(), tag);
            this.kind = Kind.valueOf(tag.getStringOr("kind", Kind.VOLCANIC_VENT.name()));
            int[] c = tag.getIntArray("center").orElse(new int[3]);
            this.center = new BlockPos(c[0], c[1], c[2]);
            this.radius = tag.getIntOr("radius", 1);
            this.height = tag.getIntOr("height", 0);
            this.divisor = tag.getIntOr("divisor", 1);
            this.parts = tag.getIntArray("parts").orElse(new int[0]);
            this.block = Identifier.parse(tag.getStringOr("block", "minecraft:stone"));
            this.core = Identifier.parse(tag.getStringOr("core", "minecraft:stone"));
            this.fluid = Identifier.parse(tag.getStringOr("fluid", "minecraft:lava"));
        }

        private static BoundingBox box(Kind kind, BlockPos c, int r, int height, int maxPart) {
            int reach = r + maxPart + 2;
            return switch (kind) {
                case GIANT_VOLCANO -> new BoundingBox(c.getX() - reach, c.getY() - 4, c.getZ() - reach,
                        c.getX() + reach, Math.max(height, c.getY()) + 2, c.getZ() + reach);
                case VOLCANIC_VENT -> new BoundingBox(c.getX() - reach, c.getY() - 4, c.getZ() - reach,
                        c.getX() + reach, c.getY() + height + 4, c.getZ() + reach);
                case METEORITE_FIELD -> new BoundingBox(c.getX() - reach, c.getY() - reach, c.getZ() - reach,
                        c.getX() + reach, c.getY() + reach, c.getZ() + reach);
            };
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("kind", kind.name());
            tag.put("center", new IntArrayTag(new int[]{center.getX(), center.getY(), center.getZ()}));
            tag.putInt("radius", radius);
            tag.putInt("height", height);
            tag.putInt("divisor", divisor);
            tag.put("parts", new IntArrayTag(parts));
            tag.putString("block", block.toString());
            tag.putString("core", core.toString());
            tag.putString("fluid", fluid.toString());
        }

        private static BlockState state(Identifier id) {
            return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            if (kind == Kind.METEORITE_FIELD) {
                for (int k = 0; k + 2 < parts.length; k += 3) {
                    int cx = center.getX() + parts[k];
                    int cz = center.getZ() + parts[k + 1];
                    int r = parts[k + 2];
                    int depth = Math.max(1, r / divisor);
                    for (int dx = -r - 2; dx <= r + 2; dx++) {
                        for (int dz = -r - 2; dz <= r + 2; dz++) {
                            CraterShape.column(level, cx + dx, cz + dz, cx, cz, r, depth, 1, 0, 0, chunkBox);
                        }
                    }
                    BlockPos bottom = new BlockPos(cx, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1, cz);
                    if (chunkBox.isInside(bottom)) {
                        level.setBlock(bottom, state(core), Block.UPDATE_CLIENTS);
                    }
                }
                return;
            }
            BlockState fill = state(block);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
                for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
                    double d = Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2));
                    if (d > radius) {
                        continue;
                    }
                    int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    int peak = kind == Kind.GIANT_VOLCANO ? height : center.getY() + height;
                    int top = (int) Math.round(center.getY() + (peak - center.getY()) * (1 - d / radius));
                    // Krater oben: Radius radius / divisor, Boden so hoch wie der Kegel am Kraterrand, darin Lava
                    double craterR = (double) radius / divisor;
                    boolean crater = d < craterR;
                    if (crater) {
                        top = (int) Math.round(center.getY() + (peak - center.getY()) * (1 - craterR / radius)) - 1;
                    }
                    top = Math.min(top, level.getMaxY() - 1);
                    for (int y = ground + 1; y <= top; y++) {
                        pos.set(x, y, z);
                        if (chunkBox.isInside(pos)) {
                            level.setBlock(pos, kind == Kind.VOLCANIC_VENT && d > radius - 1.5 ? state(core) : fill,
                                    Block.UPDATE_CLIENTS);
                        }
                    }
                    pos.set(x, Math.max(top, ground) + 1, z);
                    if (crater && chunkBox.isInside(pos)) {
                        level.setBlock(pos, state(fluid), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

}
