package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.Route;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * S→C (20.4): Inhalt des Logistik-Computers: Terminals der Agentur mit Hangar-Belegung, Routen, Raketenstatus,
 * Routen mit Treibstoff-Warnung, Rückmeldung zur letzten Änderung ({@code message}, leer = keine).
 */
public record LogisticsStatePayload(BlockPos computer, List<Terminal> terminals, List<Route> routes,
                                    List<CargoFlights.RocketStatus> statuses, List<UUID> warnings, String message)
        implements CustomPacketPayload {

    /** Terminal der Agentur; {@code hangar} = Item-ID je Hangar-Slot ("" = leer). */
    public record Terminal(Identifier dimension, BlockPos pos, List<String> hangar) {
        public static final StreamCodec<ByteBuf, Terminal> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Terminal::dimension,
                BlockPos.STREAM_CODEC, Terminal::pos,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Terminal::hangar,
                Terminal::new);
    }

    public static final Type<LogisticsStatePayload> TYPE = new Type<>(InfiniteHorizon.id("logistics_state"));
    public static final StreamCodec<ByteBuf, LogisticsStatePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LogisticsStatePayload::computer,
            Terminal.STREAM_CODEC.apply(ByteBufCodecs.list()), LogisticsStatePayload::terminals,
            Route.STREAM_CODEC.apply(ByteBufCodecs.list()), LogisticsStatePayload::routes,
            CargoFlights.RocketStatus.STREAM_CODEC.apply(ByteBufCodecs.list()), LogisticsStatePayload::statuses,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), LogisticsStatePayload::warnings,
            ByteBufCodecs.STRING_UTF8, LogisticsStatePayload::message,
            LogisticsStatePayload::new);

    @Override
    public Type<LogisticsStatePayload> type() {
        return TYPE;
    }
}
