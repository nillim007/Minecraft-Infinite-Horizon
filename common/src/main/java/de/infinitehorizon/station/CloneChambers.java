package de.infinitehorizon.station;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.HazardTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Klon-Kammern (14.4), global im Datenspeicher der Overworld: Bindung je Spieler und gemeldeter Zustand je Kammer (Strom,
 * Kulturen, noch abzuziehende Kulturen). Beim Respawn zählt nur dieser Zustand; die Kammer muss nicht geladen sein.
 */
public final class CloneChambers extends SavedData {

    /** Zustand einer Kammer; {@code pending} Kulturen zieht die Kammer beim nächsten Laden ab. */
    public record State(boolean powered, int cultures, int pending) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("powered").forGetter(State::powered),
                Codec.INT.fieldOf("cultures").forGetter(State::cultures),
                Codec.INT.optionalFieldOf("pending", 0).forGetter(State::pending)
        ).apply(i, State::new));
    }

    private record Entry(GlobalPos pos, State state) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                State.CODEC.fieldOf("state").forGetter(Entry::state)
        ).apply(i, Entry::new));
    }

    public static final Codec<CloneChambers> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC).optionalFieldOf("bindings", Map.of())
                    .forGetter(d -> d.bindings),
            Entry.CODEC.listOf().optionalFieldOf("chambers", List.of()).forGetter(d -> {
                List<Entry> out = new ArrayList<>();
                d.states.forEach((p, s) -> out.add(new Entry(p, s)));
                return out;
            })
    ).apply(i, (b, c) -> {
        CloneChambers d = new CloneChambers();
        d.bindings.putAll(b);
        c.forEach(e -> d.states.put(e.pos(), e.state()));
        return d;
    }));

    public static final SavedDataType<CloneChambers> TYPE = new SavedDataType<>(InfiniteHorizon.id("clone_chambers"),
            CloneChambers::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, GlobalPos> bindings = new HashMap<>();
    private final Map<GlobalPos, State> states = new HashMap<>();

    public CloneChambers() {
    }

    public static CloneChambers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Bindet den Spieler an die Kammer (ein Bindungsort je Spieler). */
    public void bind(UUID player, GlobalPos pos) {
        bindings.put(player, pos);
        setDirty();
    }

    public @Nullable GlobalPos binding(UUID player) {
        return bindings.get(player);
    }

    public @Nullable State state(GlobalPos pos) {
        return states.get(pos);
    }

    /** Meldung der Kammer bei jeder Änderung; gibt die noch abzuziehenden Kulturen zurück und setzt sie auf 0. */
    public int report(GlobalPos pos, boolean powered, int cultures) {
        State old = states.get(pos);
        int pending = old == null ? 0 : old.pending();
        State now = new State(powered, Math.max(0, cultures - pending), 0);
        if (!now.equals(old)) {
            states.put(pos, now);
            setDirty();
        }
        return pending;
    }

    /** Kammer abgebaut: Zustand weg, Bindungen dorthin bleiben wirkungslos. */
    public void remove(GlobalPos pos) {
        if (states.remove(pos) != null) {
            setDirty();
        }
    }

    /**
     * Respawn (14.4): Hat der Spieler eine Bindung an eine Kammer mit Strom und Kultur und ist der Platz über ihr ohne
     * Anzug sicher, erscheint er dort (auch in einer anderen Dimension); eine Kultur wird verbraucht. Sonst false.
     */
    public static boolean respawn(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        CloneChambers data = get(server);
        GlobalPos bound = data.binding(player.getUUID());
        State s = bound == null ? null : data.state(bound);
        if (s == null || !s.powered() || s.cultures() <= 0) {
            return false;
        }
        ServerLevel level = server.getLevel(bound.dimension());
        if (level == null) {
            return false;
        }
        BlockPos at = bound.pos().above();
        if (HazardTracker.unsafe(level, at)) {
            player.sendSystemMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".clone.unsafe"));
            return false;
        }
        player.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(at), Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.DO_NOTHING));
        data.states.put(bound, new State(s.powered(), s.cultures() - 1, s.pending() + 1));
        data.setDirty();
        player.sendSystemMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".clone.respawn"));
        return true;
    }
}
