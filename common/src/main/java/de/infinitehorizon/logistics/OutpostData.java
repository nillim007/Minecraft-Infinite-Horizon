package de.infinitehorizon.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Register aller Außenposten (20.1, 3A.4 {@code OutpostData}) in der Overworld: Frachtterminals und die
 * Produzenten-Schnappschüsse (Maschine, Status, Rate, letzter Tick). Zeiten in Overworld-Spielzeit.
 */
public final class OutpostData extends SavedData {

    /** Ein Frachtterminal mit Besitzer-Agentur (leer = ohne Besitzer). */
    public record Terminal(Identifier dimension, BlockPos pos, java.util.Optional<java.util.UUID> owner) {
        static final Codec<Terminal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Terminal::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Terminal::pos),
                net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Terminal::owner)
        ).apply(i, Terminal::new));
    }

    /**
     * Schnappschuss eines Produzenten: zuletzt gesehener Status, Ausgabe je Vorgang und Vorgangsdauer (Rate) und der
     * Tick, zu dem er zuletzt lief.
     */
    public record Producer(Identifier dimension, BlockPos pos, String machine, boolean running, long outputPerCycle,
                           int cycleTicks, long lastTick, long token) {
        static final Codec<Producer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Producer::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Producer::pos),
                Codec.STRING.fieldOf("machine").forGetter(Producer::machine),
                Codec.BOOL.fieldOf("running").forGetter(Producer::running),
                Codec.LONG.fieldOf("output_per_cycle").forGetter(Producer::outputPerCycle),
                Codec.INT.fieldOf("cycle_ticks").forGetter(Producer::cycleTicks),
                Codec.LONG.fieldOf("last_tick").forGetter(Producer::lastTick),
                Codec.LONG.fieldOf("token").forGetter(Producer::token)
        ).apply(i, Producer::new));

        public OfflineProduction.Snapshot snapshot() {
            return new OfflineProduction.Snapshot(running, outputPerCycle, cycleTicks, OfflineProduction.UNLIMITED,
                    OfflineProduction.UNLIMITED);
        }
    }

    private record Stored(List<Terminal> terminals, List<Producer> producers) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Terminal.CODEC.listOf().fieldOf("terminals").forGetter(Stored::terminals),
                Producer.CODEC.listOf().fieldOf("producers").forGetter(Stored::producers)
        ).apply(i, Stored::new));
    }

    public static final Codec<OutpostData> CODEC = Stored.CODEC.xmap(s -> {
        OutpostData d = new OutpostData();
        s.terminals().forEach(t -> d.terminals.put(key(t.dimension(), t.pos()), t));
        s.producers().forEach(p -> d.producers.put(key(p.dimension(), p.pos()), p));
        return d;
    }, d -> new Stored(List.copyOf(d.terminals.values()), List.copyOf(d.producers.values())));

    public static final SavedDataType<OutpostData> TYPE = new SavedDataType<>(InfiniteHorizon.id("outposts"),
            OutpostData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Terminal> terminals = new LinkedHashMap<>();
    private final Map<String, Producer> producers = new LinkedHashMap<>();

    public OutpostData() {
    }

    public static OutpostData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + "|" + pos.asLong();
    }

    public void addTerminal(Identifier dimension, BlockPos pos, java.util.@Nullable UUID owner) {
        Terminal t = new Terminal(dimension, pos.immutable(), java.util.Optional.ofNullable(owner));
        if (!t.equals(terminals.put(key(dimension, pos), t))) {
            setDirty();
        }
    }

    /** Entfernt Terminal oder Produzent an dieser Stelle (Block abgebaut). */
    public void remove(Identifier dimension, BlockPos pos) {
        String k = key(dimension, pos);
        if (terminals.remove(k) != null | producers.remove(k) != null) {
            setDirty();
        }
    }

    public boolean hasTerminal(Identifier dimension, BlockPos pos) {
        return terminals.containsKey(key(dimension, pos));
    }

    /** Nächstes Terminal derselben Dimension im Umkreis {@code radius} (20.1: nächstgelegenes Terminal). */
    public @Nullable BlockPos nearestTerminal(Identifier dimension, BlockPos pos, int radius) {
        BlockPos best = null;
        double bestDist = (double) radius * radius;
        for (Terminal t : terminals.values()) {
            if (!t.dimension().equals(dimension)) {
                continue;
            }
            double d = t.pos().distSqr(pos);
            if (d <= bestDist && (best == null || d < bestDist || d == bestDist && t.pos().asLong() < best.asLong())) {
                best = t.pos();
                bestDist = d;
            }
        }
        return best;
    }

    public @Nullable Producer producer(Identifier dimension, BlockPos pos) {
        return producers.get(key(dimension, pos));
    }

    public void putProducer(Producer p) {
        producers.put(key(p.dimension(), p.pos()), p);
        setDirty();
    }

    /** Produzenten, die zum Terminal gehören (jeweils nächstgelegenes Terminal im Umkreis). */
    public List<Producer> producersOf(Identifier dimension, BlockPos terminal, int radius) {
        List<Producer> out = new ArrayList<>();
        for (Producer p : producers.values()) {
            if (p.dimension().equals(dimension) && terminal.equals(nearestTerminal(dimension, p.pos(), radius))) {
                out.add(p);
            }
        }
        return out;
    }

    public List<Terminal> terminals() {
        return List.copyOf(terminals.values());
    }
}
