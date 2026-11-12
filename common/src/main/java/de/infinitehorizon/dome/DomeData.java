package de.infinitehorizon.dome;

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

/**
 * Aktive Kuppeln einer Dimension (10.1, 3A.4) als {@link SavedData} des Levels: Mittelpunkt (Generator), Radius, Stufe,
 * Agentur, Zustand ({@code failing} mit Zähler). Eine Kuppel, die aus ist, steht nicht in der Liste.
 */
public final class DomeData extends SavedData {

    /** Eine aktive Kuppel. {@code failingTicks} zählt seit Beginn von {@code failing} (−1 = läuft normal). */
    public record Entry(BlockPos center, int radius, int tier, Optional<UUID> agency, int failingTicks) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("center").forGetter(Entry::center),
                Codec.INT.fieldOf("radius").forGetter(Entry::radius),
                Codec.INT.fieldOf("tier").forGetter(Entry::tier),
                UUIDUtil.CODEC.optionalFieldOf("agency").forGetter(Entry::agency),
                Codec.INT.optionalFieldOf("failing_ticks", -1).forGetter(Entry::failingTicks)
        ).apply(i, Entry::new));

        public boolean failing() {
            return failingTicks >= 0;
        }

        /** Halbkugel (10.1): Abstand ≤ Radius und y ≥ Mittelpunkt − {@code belowCenter}. */
        public boolean contains(BlockPos pos, int belowCenter) {
            return pos.getY() >= center.getY() - belowCenter && pos.distSqr(center) <= (double) radius * radius;
        }
    }

    public static final Codec<DomeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("domes", List.of()).forGetter(d -> List.copyOf(d.entries))
    ).apply(i, DomeData::new));

    // wie AgencyData: Vanilla verlangt einen DataFixTypes-Wert (SPEC-GAP #40)
    public static final SavedDataType<DomeData> TYPE = new SavedDataType<>(InfiniteHorizon.id("domes"), DomeData::new,
            CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final List<Entry> entries = new ArrayList<>();
    private int version;

    public DomeData() {
    }

    private DomeData(List<Entry> list) {
        entries.addAll(list);
    }

    public static DomeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** Zählt jede Änderung (für Index und Sync). */
    public int version() {
        return version;
    }

    public @Nullable Entry at(BlockPos center) {
        for (Entry e : entries) {
            if (e.center().equals(center)) {
                return e;
            }
        }
        return null;
    }

    public void put(Entry entry) {
        entries.removeIf(e -> e.center().equals(entry.center()));
        entries.add(entry);
        changed();
    }

    public void remove(BlockPos center) {
        if (entries.removeIf(e -> e.center().equals(center))) {
            changed();
        }
    }

    private void changed() {
        version++;
        setDirty();
    }
}
