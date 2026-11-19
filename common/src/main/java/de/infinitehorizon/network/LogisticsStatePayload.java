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

    /**
     * Start oder Ziel; {@code hangar} = Item-ID je Hangar-Slot ("" = leer, nur eigene Frachtterminals haben einen
     * Hangar). {@code kind}: {@code terminal}, {@code dock}, {@code trading_post} und ab M9 die Partner-Ziele
     * {@code partner_terminal}, {@code partner_dock}, {@code partner_trading_post} (23.3).
     */
    public record Terminal(Identifier dimension, BlockPos pos, List<String> hangar, String kind) {
        public static final String TERMINAL = "terminal";
        public static final String DOCK = "dock";
        public static final String TRADING_POST = "trading_post";
        public static final String PARTNER = "partner_";
        public static final StreamCodec<ByteBuf, Terminal> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Terminal::dimension,
                BlockPos.STREAM_CODEC, Terminal::pos,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Terminal::hangar,
                ByteBufCodecs.stringUtf8(32), Terminal::kind,
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
