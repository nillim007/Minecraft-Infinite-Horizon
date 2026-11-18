package de.infinitehorizon.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sender und Sternenkarte je Agentur (18, Ä4), im Datenspeicher der Overworld: lebender Wächter, Zeit des letzten Siegs
 * (Spielzeit der Overworld) und die gültige Karte ({@code chart_uuid}).
 */
public final class SignalData extends SavedData {

    public record Entry(Optional<UUID> sentinel, Optional<Long> defeatedAt, Optional<UUID> chart) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.optionalFieldOf("sentinel").forGetter(Entry::sentinel),
                Codec.LONG.optionalFieldOf("defeated_at").forGetter(Entry::defeatedAt),
                UUIDUtil.CODEC.optionalFieldOf("chart_uuid").forGetter(Entry::chart)
        ).apply(i, Entry::new));

        static final Entry EMPTY = new Entry(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static final Codec<SignalData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC)
            .xmap(m -> {
                SignalData d = new SignalData();
                d.entries.putAll(m);
                return d;
            }, d -> d.entries);

    public static final SavedDataType<SignalData> TYPE = new SavedDataType<>(InfiniteHorizon.id("signal"), SignalData::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Entry> entries = new HashMap<>();

    public SignalData() {
    }

    public static SignalData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Entry entry(UUID agency) {
        return entries.getOrDefault(agency, Entry.EMPTY);
    }

    private void put(UUID agency, Entry e) {
        entries.put(agency, e);
        setDirty();
    }

    /** Darf die Agentur beschwören? Kein lebender Wächter und der letzte Sieg mindestens {@code cooldown} Ticks her. */
    public boolean canSummon(UUID agency, long now, long cooldown) {
        Entry e = entry(agency);
        return e.sentinel().isEmpty() && e.defeatedAt().map(t -> now - t >= cooldown).orElse(true);
    }

    public void summoned(UUID agency, UUID sentinel) {
        Entry e = entry(agency);
        put(agency, new Entry(Optional.of(sentinel), e.defeatedAt(), e.chart()));
    }

    public void defeated(UUID agency, long now) {
        Entry e = entry(agency);
        put(agency, new Entry(Optional.empty(), Optional.of(now), e.chart()));
    }

    /** Wächter ohne Sieg verschwunden (z. B. Befehl {@code /kill}): neu beschwörbar, ohne Wartezeit. */
    public void vanished(UUID agency) {
        Entry e = entry(agency);
        if (e.sentinel().isPresent()) {
            put(agency, new Entry(Optional.empty(), e.defeatedAt(), e.chart()));
        }
    }

    public @Nullable UUID chart(UUID agency) {
        return entry(agency).chart().orElse(null);
    }

    public void setChart(UUID agency, @Nullable UUID chart) {
        Entry e = entry(agency);
        put(agency, new Entry(e.sentinel(), e.defeatedAt(), Optional.ofNullable(chart)));
    }

    /** Eine Karte wurde zerstört: Gehört sie zur Agentur, gilt diese wieder als ohne Karte. */
    public void chartLost(UUID agency, UUID chart) {
        if (chart.equals(chart(agency))) {
            setChart(agency, null);
        }
    }
}
