package de.infinitehorizon.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stationskerne einer Orbit-Dimension (14.1): Position und Besitzer-Agentur, auch für nicht geladene Chunks. */
public final class StationData extends SavedData {

    public record Core(BlockPos pos, Optional<UUID> agency) {
        public static final Codec<Core> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Core::pos),
                UUIDUtil.CODEC.optionalFieldOf("agency").forGetter(Core::agency)
        ).apply(i, Core::new));
    }

    public static final Codec<StationData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Core.CODEC.listOf().optionalFieldOf("cores", List.of()).forGetter(d -> List.copyOf(d.cores))
    ).apply(i, StationData::new));

    // wie DomeData (SPEC-GAP #40)
    public static final SavedDataType<StationData> TYPE = new SavedDataType<>(InfiniteHorizon.id("stations"), StationData::new,
            CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Core> cores = new ArrayList<>();

    public StationData() {
    }

    private StationData(List<Core> list) {
        cores.addAll(list);
    }

    public static StationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Core> cores() {
        return List.copyOf(cores);
    }

    public void put(BlockPos pos, @Nullable UUID agency) {
        cores.removeIf(c -> c.pos().equals(pos));
        cores.add(new Core(pos.immutable(), Optional.ofNullable(agency)));
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (cores.removeIf(c -> c.pos().equals(pos))) {
            setDirty();
        }
    }

    /** Nächster Kern im Umkreis {@code radius} (horizontal und vertikal, Würfel), sonst null. */
    public @Nullable Core nearest(BlockPos pos, int radius) {
        Core best = null;
        double bestDist = Double.MAX_VALUE;
        for (Core c : cores) {
            double d = c.pos().distSqr(pos);
            if (d <= (double) radius * radius && d < bestDist) {
                best = c;
                bestDist = d;
            }
        }
        return best;
    }

    /** Liegt ein Kern einer anderen Agentur (oder ohne Besitzer) näher als {@code radius}? */
    public boolean foreignWithin(BlockPos pos, int radius, @Nullable UUID agency) {
        for (Core c : cores) {
            if (c.pos().distSqr(pos) < (double) radius * radius && (agency == null || !c.agency().equals(Optional.of(agency)))) {
                return true;
            }
        }
        return false;
    }
}
