package de.infinitehorizon.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Asteroid (13.4): Kugel oder Ellipsoid aus Gestein, ein Anteil der Blöcke wird zu Erz ({@code fraction} je Erz).
 * Gemeinsam für das Feature {@code asteroid}, die Struktur {@code large_asteroid} und den Spawn-Asteroiden.
 */
public final class AsteroidShape {

    /** Erzanteil: {@code fraction} der Gesteinsblöcke werden zu {@code block}. */
    public record OreShare(Identifier block, double fraction) {
        public static final Codec<OreShare> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("block").forGetter(OreShare::block),
                Codec.DOUBLE.fieldOf("fraction").forGetter(OreShare::fraction)).apply(i, OreShare::new));
    }

    private AsteroidShape() {
    }

    /** Liegt der Versatz (dx, dy, dz) im Ellipsoid mit den Halbachsen rx, ry, rz? Reine Rechnung. */
    public static boolean inside(int dx, int dy, int dz, double rx, double ry, double rz) {
        double v = dx * dx / (rx * rx) + dy * dy / (ry * ry) + dz * dz / (rz * rz);
        return v <= 1.0;
    }

    /** Welches Erz ein Block wird, für eine Zufallszahl {@code roll} in [0, 1): Index oder -1 (Gestein). */
    public static int pick(double roll, double[] fractions) {
        double sum = 0;
        for (int k = 0; k < fractions.length; k++) {
            sum += fractions[k];
            if (roll < sum) {
                return k;
            }
        }
        return -1;
    }

    /** Halbachsen eines Asteroiden: Kugel oder flaches Ellipsoid (je Achse 70–100 % des Radius). */
    public static double[] axes(int radius, boolean ellipsoid, RandomSource random) {
        if (!ellipsoid) {
            return new double[]{radius, radius, radius};
        }
        return new double[]{radius * (0.7 + 0.3 * random.nextDouble()), radius * (0.7 + 0.3 * random.nextDouble()),
                radius * (0.7 + 0.3 * random.nextDouble())};
    }

    /**
     * Füllt den Asteroiden; nur Luft wird ersetzt. Mit {@code clip} nur innerhalb dieses Bereichs (Strukturteil je Chunk).
     * Der Zufall je Block hängt nur von der Position ab, damit Teile über Chunkgrenzen zusammenpassen.
     *
     * @return Positionen, an denen das erste Erz der Liste liegt (Schwärme der Driftmilbe), höchstens eine
     */
    public static @Nullable BlockPos fill(WorldGenLevel level, BlockPos center, double[] r, Identifier rock, List<OreShare> ores,
                                          long seed, @Nullable BoundingBox clip) {
        BlockState stone = state(rock);
        BlockState[] oreStates = new BlockState[ores.size()];
        double[] fractions = new double[ores.size()];
        for (int k = 0; k < ores.size(); k++) {
            oreStates[k] = state(ores.get(k).block());
            fractions[k] = ores.get(k).fraction();
        }
        BlockPos firstOre = null;
        int ex = (int) Math.ceil(r[0]);
        int ey = (int) Math.ceil(r[1]);
        int ez = (int) Math.ceil(r[2]);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -ex; dx <= ex; dx++) {
            for (int dy = -ey; dy <= ey; dy++) {
                for (int dz = -ez; dz <= ez; dz++) {
                    if (!inside(dx, dy, dz, r[0], r[1], r[2])) {
                        continue;
                    }
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (clip != null && !clip.isInside(pos) || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    int ore = pick(roll(seed, pos), fractions);
                    level.setBlock(pos, ore < 0 ? stone : oreStates[ore], Block.UPDATE_CLIENTS);
                    if (ore == 0 && firstOre == null) {
                        firstOre = pos.immutable();
                    }
                }
            }
        }
        return firstOre;
    }

    /** Zufallszahl in [0, 1) aus Seed und Position. */
    static double roll(long seed, BlockPos p) {
        long h = seed ^ p.asLong() * 0x9E3779B97F4A7C15L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    static BlockState state(Identifier id) {
        return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
    }
}
