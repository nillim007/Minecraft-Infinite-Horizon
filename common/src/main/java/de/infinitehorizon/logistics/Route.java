package de.infinitehorizon.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.UUID;

/**
 * Route des Logistik-Computers (20.4): Start (immer das Heimatterminal der Raketen), Ziel, Fracht-Filter (Item- oder
 * Fluid-IDs, leer = alles), Startbedingung mit N und X, zugewiesene Hangar-Slots, letzter Start.
 */
public record Route(UUID id, UUID agency, Identifier homeDimension, BlockPos home, Identifier targetDimension, BlockPos target,
                    List<String> filter, String condition, long amount, String item, List<Integer> rockets, long lastStart) {

    public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Route::id),
            UUIDUtil.CODEC.fieldOf("agency").forGetter(Route::agency),
            Identifier.CODEC.fieldOf("home_dimension").forGetter(Route::homeDimension),
            BlockPos.CODEC.fieldOf("home").forGetter(Route::home),
            Identifier.CODEC.fieldOf("target_dimension").forGetter(Route::targetDimension),
            BlockPos.CODEC.fieldOf("target").forGetter(Route::target),
            Codec.STRING.listOf().fieldOf("filter").forGetter(Route::filter),
            Codec.STRING.fieldOf("condition").forGetter(Route::condition),
            Codec.LONG.fieldOf("amount").forGetter(Route::amount),
            Codec.STRING.fieldOf("item").forGetter(Route::item),
            Codec.INT.listOf().fieldOf("rockets").forGetter(Route::rockets),
            Codec.LONG.fieldOf("last_start").forGetter(Route::lastStart)
    ).apply(i, Route::new));

    public static final StreamCodec<ByteBuf, Route> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public RouteRules.Condition conditionType() {
        return RouteRules.Condition.byId(condition);
    }

    public Route withLastStart(long tick) {
        return new Route(id, agency, homeDimension, home, targetDimension, target, filter, condition, amount, item, rockets, tick);
    }

    /** Passt eine Item- oder Fluid-ID zum Filter? */
    public boolean accepts(String id) {
        return filter.isEmpty() || filter.contains(id);
    }
}
