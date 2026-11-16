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

    public enum Kind { GIANT_VOLCANO, VOLCANIC_VENT, METEORITE_FIELD, ICE_VOLCANO, ASTEROID, HYDROTHERMAL_FIELD }

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
            Identifier.CODEC.fieldOf("fluid").forGetter(s -> s.fluid),
            IntProviders.CODEC.optionalFieldOf("y").forGetter(s -> Optional.ofNullable(s.y)),
            AsteroidShape.OreShare.CODEC.listOf().optionalFieldOf("ores", java.util.List.of()).forGetter(s -> s.ores),
            Identifier.CODEC.optionalFieldOf("guardian").forGetter(s -> Optional.ofNullable(s.guardian))
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
    /** Asteroid (13.4): Höhe des Mittelpunkts; ohne Angabe {@code height}. */
    private final @org.jspecify.annotations.Nullable IntProvider y;
    private final java.util.List<AsteroidShape.OreShare> ores;
    /** Wächter (18): eine Kreatur, die mit der Struktur entsteht ({@code reef_keeper} im Quellenfeld). */
    private final @org.jspecify.annotations.Nullable Identifier guardian;

    private SurfaceStructure(StructureSettings settings, Kind kind, IntProvider radius, int height, int divisor,
                             Optional<IntProvider> count, Optional<IntProvider> partRadius, Identifier block, Identifier core,
                             Identifier fluid, Optional<IntProvider> y, java.util.List<AsteroidShape.OreShare> ores,
                             Optional<Identifier> guardian) {
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
        this.y = y.orElse(null);
        this.ores = java.util.List.copyOf(ores);
        this.guardian = guardian.orElse(null);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        RandomSource random = context.random();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        if (kind == Kind.ASTEROID) {
            // Asteroid im leeren Raum: Mitte auf Höhe y (bzw. height), Kugel mit Radius radius
            int cy = this.y != null ? this.y.sample(random) : height;
            int r = radius.sample(random);
            AsteroidPiece piece = new AsteroidPiece(new BlockPos(x, cy, z), r, block, ores, random.nextLong());
            return Optional.of(new GenerationStub(new BlockPos(x, cy, z), b -> b.addPiece(piece)));
        }
        // Quellenfeld (Europa): auf dem Meeresboden, sonst auf der Oberfläche
        Heightmap.Types surface = kind == Kind.HYDROTHERMAL_FIELD ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
        int ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, surface, context.heightAccessor(), context.randomState());
        int r = radius.sample(random);
        // Teile: Krater des Meteoritenfelds (dx, dz, Radius je Krater)
        int n = (kind == Kind.METEORITE_FIELD || kind == Kind.HYDROTHERMAL_FIELD) && count != null ? count.sample(random) : 0;
        int[] parts = new int[n * 3];
        int maxPart = 0;
        for (int k = 0; k < n; k++) {
            parts[k * 3] = random.nextInt(2 * r + 1) - r;
            parts[k * 3 + 1] = random.nextInt(2 * r + 1) - r;
            // Meteoritenfeld: Kraterradius; Quellenfeld: Höhe des Schlots unter der Quelle
            parts[k * 3 + 2] = partRadius == null ? 1 : partRadius.sample(random);
            maxPart = Math.max(maxPart, parts[k * 3 + 2]);
        }
        Piece piece = new Piece(kind, new BlockPos(x, ground, z), r, height, divisor, parts, maxPart, block, core, fluid, guardian);
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), b -> b.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return switch (kind) {
            case GIANT_VOLCANO -> ModWorldgen.GIANT_VOLCANO.get();
            case VOLCANIC_VENT -> ModWorldgen.VOLCANIC_VENT.get();
            case METEORITE_FIELD -> ModWorldgen.METEORITE_FIELD.get();
            case ICE_VOLCANO -> ModWorldgen.ICE_VOLCANO.get();
            case ASTEROID -> ModWorldgen.LARGE_ASTEROID.get();
            case HYDROTHERMAL_FIELD -> ModWorldgen.HYDROTHERMAL_FIELD.get();
        };
    }

    /** Ein Asteroid als Strukturteil; die Blöcke je Chunk setzt {@link AsteroidShape}. */
    public static final class AsteroidPiece extends StructurePiece {

        private final BlockPos center;
        private final int radius;
        private final Identifier block;
        private final java.util.List<AsteroidShape.OreShare> ores;
        private final long seed;

        AsteroidPiece(BlockPos center, int radius, Identifier block, java.util.List<AsteroidShape.OreShare> ores, long seed) {
            super(ModWorldgen.ASTEROID_PIECE.get(), 0, new BoundingBox(center.getX() - radius, center.getY() - radius,
                    center.getZ() - radius, center.getX() + radius, center.getY() + radius, center.getZ() + radius));
            this.center = center;
            this.radius = radius;
            this.block = block;
            this.ores = ores;
            this.seed = seed;
        }

        public AsteroidPiece(CompoundTag tag) {
            super(ModWorldgen.ASTEROID_PIECE.get(), tag);
            int[] c = tag.getIntArray("center").orElse(new int[3]);
            this.center = new BlockPos(c[0], c[1], c[2]);
            this.radius = tag.getIntOr("radius", 1);
            this.block = Identifier.parse(tag.getStringOr("block", "minecraft:stone"));
            this.seed = tag.getLongOr("seed", 0L);
            java.util.List<AsteroidShape.OreShare> list = new java.util.ArrayList<>();
            String[] ids = tag.getStringOr("ores", "").split(",");
            String[] fr = tag.getStringOr("fractions", "").split(",");
            for (int k = 0; k < ids.length && k < fr.length; k++) {
                if (!ids[k].isEmpty()) {
                    list.add(new AsteroidShape.OreShare(Identifier.parse(ids[k]), Double.parseDouble(fr[k])));
                }
            }
            this.ores = list;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.put("center", new IntArrayTag(new int[]{center.getX(), center.getY(), center.getZ()}));
            tag.putInt("radius", radius);
            tag.putString("block", block.toString());
            tag.putLong("seed", seed);
            tag.putString("ores", String.join(",", ores.stream().map(o -> o.block().toString()).toList()));
            tag.putString("fractions", String.join(",", ores.stream().map(o -> Double.toString(o.fraction())).toList()));
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            AsteroidShape.fill(level, center, new double[]{radius, radius, radius}, block, ores, seed, chunkBox);
        }
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
        private final @org.jspecify.annotations.Nullable Identifier guardian;

        Piece(Kind kind, BlockPos center, int radius, int height, int divisor, int[] parts, int maxPart, Identifier block,
              Identifier core, Identifier fluid, @org.jspecify.annotations.Nullable Identifier guardian) {
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
            this.guardian = guardian;
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
            String g = tag.getStringOr("guardian", "");
            this.guardian = g.isEmpty() ? null : Identifier.parse(g);
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
                case HYDROTHERMAL_FIELD -> new BoundingBox(c.getX() - reach, c.getY() - 4, c.getZ() - reach,
                        c.getX() + reach, c.getY() + maxPart + 6, c.getZ() + reach);
                case ICE_VOLCANO, ASTEROID -> new BoundingBox(c.getX() - reach, c.getY() - 4, c.getZ() - reach,
                        c.getX() + reach, c.getY() + height + 4, c.getZ() + reach);
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
            if (guardian != null) {
                tag.putString("guardian", guardian.toString());
            }
        }

        /**
         * Quellenfeld (13.4): je Teil ein Schlot aus {@code core} mit der Quelle ({@code block}) obenauf, auf dem
         * Meeresboden; der Wächter erscheint einmal über der Mitte.
         */
        private void vents(WorldGenLevel level, BoundingBox chunkBox) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int k = 0; k + 2 < parts.length; k += 3) {
                int x = center.getX() + parts[k];
                int z = center.getZ() + parts[k + 1];
                if (!chunkBox.isInside(pos.set(x, chunkBox.minY(), z))) {
                    continue;
                }
                int top = seabed(level, x, z);
                for (int h = 0; h < parts[k + 2]; h++) {
                    level.setBlock(pos.set(x, top + h, z), state(core), Block.UPDATE_CLIENTS);
                }
                level.setBlock(pos.set(x, top + parts[k + 2], z), state(block), Block.UPDATE_CLIENTS);
            }
            if (guardian != null && chunkBox.isInside(pos.set(center.getX(), chunkBox.minY(), center.getZ()))) {
                var type = BuiltInRegistries.ENTITY_TYPE.getValue(guardian);
                var e = type.create(level.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                if (e != null) {
                    int top = seabed(level, center.getX(), center.getZ());
                    e.snapTo(center.getX() + 0.5, top + 3, center.getZ() + 0.5, 0, 0);
                    if (e instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setPersistenceRequired();
                    }
                    level.addFreshEntityWithPassengers(e);
                }
            }
        }

        /** Suche des Meeresbodens ab so vielen Blöcken über der geplanten Grundhöhe (Hänge im Feld, unter der Eisdecke). */
        private static final int SEABED_SEARCH = 6;

        /**
         * Erste Höhe über festem Grund, von wenig über der geplanten Grundhöhe abwärts gesucht. Der Heightmap-Typ
         * OCEAN_FLOOR_WG fehlt in fertigen Chunks ({@code /place structure}), und darüber kann schon die Eisdecke liegen.
         */
        private int seabed(WorldGenLevel level, int x, int z) {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, Math.min(center.getY() + SEABED_SEARCH, level.getMaxY()), z);
            while (p.getY() > level.getMinY()) {
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && s.getFluidState().isEmpty()) {
                    return p.getY() + 1;
                }
                p.move(net.minecraft.core.Direction.DOWN);
            }
            return center.getY();
        }

        private static BlockState state(Identifier id) {
            return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                RandomSource random, BoundingBox chunkBox, ChunkPos chunk, BlockPos referencePos) {
            if (kind == Kind.HYDROTHERMAL_FIELD) {
                vents(level, chunkBox);
                return;
            }
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
