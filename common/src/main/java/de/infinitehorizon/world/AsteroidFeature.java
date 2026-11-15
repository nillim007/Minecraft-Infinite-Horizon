package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.List;
import java.util.Optional;

/**
 * Feature {@code asteroid} (13.4): mit Wahrscheinlichkeit {@code chance} ein Asteroid mit Radius aus {@code radius}
 * (Kugel oder Ellipsoid) aus {@code block} mit Erzanteilen. Optional ein Schwarm ({@code swarm}) um das erste Erz der
 * Liste (Driftmilbe um Platinerz, 18).
 */
public record AsteroidFeature(Identifier block, IntProvider radius, double chance, boolean ellipsoid,
                              List<AsteroidShape.OreShare> ores, Optional<Swarm> swarm) implements Feature {

    /** Schwarm: {@code entity}, {@code min}–{@code max} Tiere im Umkreis {@code radius} um das Erz. */
    public record Swarm(Identifier entity, int min, int max, int radius) {
        public static final Codec<Swarm> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("entity").forGetter(Swarm::entity),
                Codec.INT.fieldOf("min").forGetter(Swarm::min),
                Codec.INT.fieldOf("max").forGetter(Swarm::max),
                Codec.INT.fieldOf("radius").forGetter(Swarm::radius)).apply(i, Swarm::new));
    }

    public static final MapCodec<AsteroidFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("block").forGetter(AsteroidFeature::block),
            IntProviders.CODEC.fieldOf("radius").forGetter(AsteroidFeature::radius),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(AsteroidFeature::chance),
            Codec.BOOL.optionalFieldOf("ellipsoid", true).forGetter(AsteroidFeature::ellipsoid),
            AsteroidShape.OreShare.CODEC.listOf().optionalFieldOf("ores", List.of()).forGetter(AsteroidFeature::ores),
            Swarm.CODEC.optionalFieldOf("swarm").forGetter(AsteroidFeature::swarm)
    ).apply(i, AsteroidFeature::new));

    @Override
    public MapCodec<AsteroidFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (random.nextDouble() >= chance) {
            return false;
        }
        double[] r = AsteroidShape.axes(radius.sample(random), ellipsoid && random.nextBoolean(), random);
        // nur innerhalb der Nachbarchunks setzen (Feature-Grenze); der Mittelpunkt liegt im eigenen Chunk
        BlockPos first = AsteroidShape.fill(level, origin, r, block, ores, random.nextLong(), null);
        swarm.ifPresent(s -> {
            if (first != null) {
                spawnSwarm(level, first, s, random);
            }
        });
        return true;
    }

    private static void spawnSwarm(WorldGenLevel level, BlockPos near, Swarm s, RandomSource random) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(s.entity());
        int n = s.min() + random.nextInt(Math.max(1, s.max() - s.min() + 1));
        for (int k = 0; k < n; k++) {
            BlockPos at = near.offset(random.nextInt(2 * s.radius() + 1) - s.radius(), random.nextInt(s.radius() + 1),
                    random.nextInt(2 * s.radius() + 1) - s.radius());
            if (!level.getBlockState(at).isAir()) {
                continue;
            }
            var e = type.create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
            if (e == null) {
                return;
            }
            e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(e);
        }
    }
}
