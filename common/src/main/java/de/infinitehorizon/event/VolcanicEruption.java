package de.infinitehorizon.event;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Örtliche Ereignisse (19), z. B. der Vulkanausbruch auf Io: {@code chance_per} = „{@code <struktur>_within_<r>_of_a_player}“
 * würfelt je Struktur im Umkreis {@code chance_search_radius} eines Spielers. Während des Ereignisses fallen Blöcke
 * ({@code effects.falling_block}) im Umkreis {@code effects.radius} um die Struktur vom Himmel. Schutz: Abstand.
 */
public final class VolcanicEruption {

    private static final String WITHIN = "_within_";
    /** SPEC-GAP #104: Takt, Fallhöhe und Schaden der Brocken stehen nicht in spec/data; Vorgaben, per Datenpaket änderbar. */
    private static final int DEFAULT_INTERVAL = 5;
    private static final int DEFAULT_DROP_HEIGHT = 24;
    private static final double DEFAULT_HURT_PER_BLOCK = 1.0;
    private static final int DEFAULT_HURT_MAX = 6;

    private VolcanicEruption() {
    }

    /** Struktur aus {@code chance_per} (Teil vor {@code _within_}), z. B. {@code volcanic_vent}. */
    static @Nullable Structure structure(ServerLevel level, Identifier event, JsonObject e) {
        String per = e.has("chance_per") ? e.get("chance_per").getAsString() : "";
        int cut = per.indexOf(WITHIN);
        if (cut <= 0) {
            return null;
        }
        Identifier id = Identifier.fromNamespaceAndPath(event.getNamespace(), per.substring(0, cut));
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(id);
    }

    /** Mitten der Strukturen in geladenen Chunks im Umkreis {@code radius} (waagerecht) um {@code center}. */
    public static List<BlockPos> near(ServerLevel level, Structure structure, BlockPos center, int radius) {
        return de.infinitehorizon.world.StructureStarts.near(level, structure, center, radius);
    }

    /** Würfeln je Struktur nahe den Spielern; der erste Treffer wird geplant. */
    static PlanetEvents.@Nullable Planned roll(ServerLevel level, Identifier event, JsonObject e, double chance,
                                                JsonObject plan, RandomSource random) {
        Structure s = structure(level, event, e);
        if (s == null || chance <= 0) {
            return null;
        }
        int radius = (int) EventService.num(e, "chance_search_radius");
        List<BlockPos> seen = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            for (BlockPos vent : near(level, s, p.blockPosition(), radius)) {
                if (seen.contains(vent)) {
                    continue;
                }
                seen.add(vent);
                if (random.nextDouble() < chance) {
                    return planned(level, event, e, vent, (long) EventService.num(plan, "schedule_ahead_ticks"), random);
                }
            }
        }
        return null;
    }

    /** Ereignis an {@code vent} in {@code ahead} Ticks, Dauer aus {@code duration_min}/{@code duration_max}. */
    public static PlanetEvents.Planned planned(ServerLevel level, Identifier event, JsonObject e, BlockPos vent, long ahead,
                                               RandomSource random) {
        long start = level.getGameTime() + ahead;
        int min = (int) EventService.num(e, "duration_min");
        int max = (int) EventService.num(e, "duration_max");
        return new PlanetEvents.Planned(event, start, start + min + random.nextInt(Math.max(1, max - min + 1)), false,
                Optional.of(vent));
    }

    private static JsonObject effects(Identifier event) {
        JsonObject e = EventService.event(event);
        return e.has("effects") ? e.getAsJsonObject("effects") : new JsonObject();
    }

    private static double opt(JsonObject fx, String key, double fallback) {
        return fx.has(key) ? fx.get(key).getAsDouble() : fallback;
    }

    /** Takt während des Ereignisses: alle {@code interval_ticks} ein Brocken. */
    static void tick(ServerLevel level, Identifier event, BlockPos vent, long now) {
        JsonObject fx = effects(event);
        if (!fx.has("falling_block") || now % Math.max(1, (long) opt(fx, "interval_ticks", DEFAULT_INTERVAL)) != 0) {
            return;
        }
        drop(level, vent, fx, level.getRandom());
    }

    /** Ein fallender Block über einer zufälligen Stelle im Umkreis {@code radius} um den Schlot. */
    public static @Nullable FallingBlockEntity drop(ServerLevel level, BlockPos vent, JsonObject fx, RandomSource random) {
        int r = (int) EventService.num(fx, "radius");
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = Math.sqrt(random.nextDouble()) * r;
        int x = vent.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = vent.getZ() + (int) Math.round(Math.sin(angle) * dist);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int height = (int) opt(fx, "drop_height", DEFAULT_DROP_HEIGHT);
        int y = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + height, level.getMaxY() - 1);
        // freie Stelle über dem Boden suchen (Überhänge, Bäume)
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, y, z);
        while (!level.getBlockState(at).isAir() && at.getY() < Math.min(y + height, level.getMaxY() - 1)) {
            at.move(net.minecraft.core.Direction.UP);
        }
        if (!level.getBlockState(at).isAir()) {
            return null;
        }
        BlockState state = BuiltInRegistries.BLOCK.getValue(Identifier.parse(fx.get("falling_block").getAsString())).defaultBlockState();
        FallingBlockEntity block = FallingBlockEntity.fall(level, at.immutable(), state);
        block.setHurtsEntities((float) opt(fx, "hurt_per_block", DEFAULT_HURT_PER_BLOCK), (int) opt(fx, "hurt_max", DEFAULT_HURT_MAX));
        return block;
    }
}
