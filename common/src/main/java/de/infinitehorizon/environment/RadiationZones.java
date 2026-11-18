package de.infinitehorizon.environment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Zeitweise Strahlungszonen einer Dimension (6.3: Notabschaltung des Kernreaktors), im Datenspeicher der Dimension.
 * Innerhalb des Radius gilt die Strahlung der Zone, bis {@code until} (Spielzeit der Dimension) vorbei ist.
 */
public final class RadiationZones extends SavedData {

    public record Zone(BlockPos center, int radius, int radiation, long until) {
        static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("center").forGetter(Zone::center),
                Codec.INT.fieldOf("radius").forGetter(Zone::radius),
                Codec.INT.fieldOf("radiation").forGetter(Zone::radiation),
                Codec.LONG.fieldOf("until").forGetter(Zone::until)
        ).apply(i, Zone::new));

        public boolean covers(BlockPos pos) {
            return center.distSqr(pos) <= (double) radius * radius;
        }
    }

    public static final Codec<RadiationZones> CODEC = Zone.CODEC.listOf().xmap(l -> {
        RadiationZones z = new RadiationZones();
        z.zones.addAll(l);
        return z;
    }, z -> List.copyOf(z.zones));

    public static final SavedDataType<RadiationZones> TYPE = new SavedDataType<>(InfiniteHorizon.id("radiation_zones"),
            RadiationZones::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Zone> zones = new ArrayList<>();

    public RadiationZones() {
    }

    public static RadiationZones get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void add(ServerLevel level, BlockPos center, int radius, int radiation, long ticks) {
        RadiationZones z = get(level);
        z.zones.add(new Zone(center.immutable(), radius, radiation, level.getGameTime() + ticks));
        z.setDirty();
    }

    /** Höchste Strahlung einer noch aktiven Zone an {@code pos}, sonst leer. Abgelaufene Zonen fallen weg. */
    public static OptionalInt at(ServerLevel level, BlockPos pos) {
        RadiationZones z = get(level);
        if (z.zones.isEmpty()) {
            return OptionalInt.empty();
        }
        long now = level.getGameTime();
        if (z.zones.removeIf(e -> e.until() <= now)) {
            z.setDirty();
        }
        return z.zones.stream().filter(e -> e.covers(pos)).mapToInt(Zone::radiation).max();
    }

    public List<Zone> zones() {
        return List.copyOf(zones);
    }

    public void clear() {
        zones.clear();
        setDirty();
    }
}
