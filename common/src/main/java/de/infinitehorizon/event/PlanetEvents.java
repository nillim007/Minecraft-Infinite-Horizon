package de.infinitehorizon.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Ereignis- und Wetterzustand einer Dimension (19): geplantes oder laufendes Ereignis und eigenes Wetter. Liegt im
 * Datenspeicher der jeweiligen Dimension.
 */
public final class PlanetEvents extends SavedData {

    /** Geplantes bzw. laufendes Ereignis: Start und Ende in Spielzeit der Dimension, Warnung schon gesendet? */
    public record Planned(Identifier event, long start, long end, boolean warned) {
        public static final Codec<Planned> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("event").forGetter(Planned::event),
                Codec.LONG.fieldOf("start").forGetter(Planned::start),
                Codec.LONG.fieldOf("end").forGetter(Planned::end),
                Codec.BOOL.optionalFieldOf("warned", false).forGetter(Planned::warned)
        ).apply(i, Planned::new));
    }

    public static final Codec<PlanetEvents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Planned.CODEC.optionalFieldOf("planned").forGetter(d -> Optional.ofNullable(d.planned)),
            Codec.BOOL.optionalFieldOf("weather", false).forGetter(d -> d.weather),
            Codec.LONG.optionalFieldOf("weather_until", 0L).forGetter(d -> d.weatherUntil)
    ).apply(i, (p, w, u) -> {
        PlanetEvents d = new PlanetEvents();
        d.planned = p.orElse(null);
        d.weather = w;
        d.weatherUntil = u;
        return d;
    }));

    public static final SavedDataType<PlanetEvents> TYPE = new SavedDataType<>(InfiniteHorizon.id("planet_events"),
            PlanetEvents::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Nullable Planned planned;
    boolean weather;
    long weatherUntil;

    public PlanetEvents() {
    }

    public static PlanetEvents get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public @Nullable Planned planned() {
        return planned;
    }

    public boolean weather() {
        return weather;
    }

    void setPlanned(@Nullable Planned p) {
        planned = p;
        setDirty();
    }

    void setWeather(boolean active, long until) {
        weather = active;
        weatherUntil = until;
        setDirty();
    }
}
