package de.infinitehorizon.shipyard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Werften (21.1, 3A.4 {@code ShipyardState}) in der Overworld: je Werft-Terminal Besitzer, virtuelles Werftlager je
 * Abschnitt und Baufortschritt in Overworld-Zeit (läuft auch, wenn die Station nicht geladen ist).
 */
public final class ShipyardState extends SavedData {

    /** Eine Werft; Abschnitte mit Index ab 0 (Schlüssel als Text für den Speicher). */
    public record Yard(Identifier dimension, BlockPos pos, Optional<UUID> owner, int built, long buildEnd, long lastEnd,
                       Map<String, Map<String, Long>> delivered, Map<String, Long> completedAt) {
        static final Codec<Yard> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Yard::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Yard::pos),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Yard::owner),
                Codec.INT.fieldOf("built").forGetter(Yard::built),
                Codec.LONG.fieldOf("build_end").forGetter(Yard::buildEnd),
                Codec.LONG.fieldOf("last_end").forGetter(Yard::lastEnd),
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.LONG)).fieldOf("delivered")
                        .forGetter(Yard::delivered),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("completed_at").forGetter(Yard::completedAt)
        ).apply(i, Yard::new));

        public ShipyardMath.State state() {
            Map<Integer, Map<String, Long>> d = new HashMap<>();
            delivered.forEach((k, v) -> d.put(Integer.parseInt(k), new HashMap<>(v)));
            Map<Integer, Long> c = new HashMap<>();
            completedAt.forEach((k, v) -> c.put(Integer.parseInt(k), v));
            return new ShipyardMath.State(built, buildEnd, lastEnd, d, c);
        }

        Yard with(ShipyardMath.State s) {
            Map<String, Map<String, Long>> d = new LinkedHashMap<>();
            s.delivered().forEach((k, v) -> d.put(Integer.toString(k), new LinkedHashMap<>(v)));
            Map<String, Long> c = new LinkedHashMap<>();
            s.completedAt().forEach((k, v) -> c.put(Integer.toString(k), v));
            return new Yard(dimension, pos, owner, s.built(), s.buildEnd(), s.lastEnd(), d, c);
        }

        Yard withOwner(@Nullable UUID o) {
            return new Yard(dimension, pos, Optional.ofNullable(o), built, buildEnd, lastEnd, delivered, completedAt);
        }
    }

    public static final Codec<ShipyardState> CODEC = Yard.CODEC.listOf().xmap(l -> {
        ShipyardState s = new ShipyardState();
        l.forEach(y -> s.yards.put(key(y.dimension(), y.pos()), y));
        return s;
    }, s -> List.copyOf(s.yards.values()));

    public static final SavedDataType<ShipyardState> TYPE = new SavedDataType<>(InfiniteHorizon.id("shipyards"),
            ShipyardState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Yard> yards = new LinkedHashMap<>();

    public ShipyardState() {
    }

    public static ShipyardState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    static String key(Identifier dimension, BlockPos pos) {
        return dimension + "|" + pos.asLong();
    }

    public @Nullable Yard yard(Identifier dimension, BlockPos pos) {
        return yards.get(key(dimension, pos));
    }

    public List<Yard> yards() {
        return List.copyOf(yards.values());
    }

    /** Werft anlegen oder den Besitzer nachführen. */
    public Yard register(Identifier dimension, BlockPos pos, @Nullable UUID owner) {
        Yard y = yards.get(key(dimension, pos));
        if (y == null) {
            y = new Yard(dimension, pos.immutable(), Optional.ofNullable(owner), 0, 0, 0, Map.of(), Map.of());
        } else if (!y.owner().equals(Optional.ofNullable(owner))) {
            y = y.withOwner(owner);
        } else {
            return y;
        }
        yards.put(key(dimension, pos), y);
        setDirty();
        return y;
    }

    public void remove(Identifier dimension, BlockPos pos) {
        if (yards.remove(key(dimension, pos)) != null) {
            setDirty();
        }
    }

    // ------------------------------------------------------------------ Regeln aus shipyard.json

    /** Bauabschnitte 1–8 (Einträge mit Zahl als Schlüssel), in Reihenfolge. */
    public static List<ShipyardMath.Section> sections() {
        JsonObject all = BalanceManager.get().entries("shipyard").orElse(new JsonObject());
        List<Integer> keys = new ArrayList<>();
        for (String k : all.keySet()) {
            if (k.chars().allMatch(Character::isDigit)) {
                keys.add(Integer.parseInt(k));
            }
        }
        keys.sort(Integer::compare);
        List<ShipyardMath.Section> out = new ArrayList<>();
        for (int k : keys) {
            JsonObject s = all.getAsJsonObject(Integer.toString(k));
            List<ShipyardMath.Material> mats = new ArrayList<>();
            for (JsonElement e : s.getAsJsonArray("materials")) {
                JsonObject m = e.getAsJsonObject();
                if (m.has("fluid")) {
                    mats.add(new ShipyardMath.Material(m.get("fluid").getAsString(), m.get("amount").getAsLong(), true));
                } else {
                    mats.add(new ShipyardMath.Material(m.get("id").getAsString(), m.get("count").getAsLong(), false));
                }
            }
            out.add(new ShipyardMath.Section(List.copyOf(mats), s.get("build_ticks").getAsLong()));
        }
        return out;
    }

    public static JsonObject rules() {
        return BalanceManager.get().entry("shipyard", "shipyard_rules").orElse(new JsonObject());
    }

    public static int stationRadius() {
        JsonObject r = rules();
        return r.has("station_core_radius") ? r.get("station_core_radius").getAsInt() : 0;
    }

    // ------------------------------------------------------------------ Betrieb

    /** Baufortschritt bis jetzt übernehmen. */
    public Yard advance(Yard y, long now) {
        Yard n = y.with(ShipyardMath.advance(sections(), y.state(), now));
        if (!n.equals(y)) {
            yards.put(key(y.dimension(), y.pos()), n);
            setDirty();
        }
        return n;
    }

    /** Offener Bedarf der Werft für eine Item- oder Fluid-ID. */
    public static long demand(Yard y, String id) {
        return ShipyardMath.demand(sections(), y.state(), id);
    }

    public static long totalDemand(Yard y) {
        return ShipyardMath.totalDemand(sections(), y.state());
    }

    /** Liefert bis zu {@code amount} ins Werftlager; gibt die angenommene Menge zurück. */
    public long deliver(Yard y, String id, long amount, long now, boolean simulate) {
        ShipyardMath.Delivery d = ShipyardMath.deliver(sections(), y.state(), id, amount, now);
        if (!simulate && d.accepted() > 0) {
            Yard n = advance(y.with(d.state()), now);
            yards.put(key(y.dimension(), y.pos()), n);
            setDirty();
        }
        return d.accepted();
    }

    /** Insgesamt geliefert von einer ID (alle Abschnitte). */
    public static long delivered(Yard y, String id) {
        long n = 0;
        for (Map<String, Long> m : y.delivered().values()) {
            n += m.getOrDefault(id, 0L);
        }
        return n;
    }
}
