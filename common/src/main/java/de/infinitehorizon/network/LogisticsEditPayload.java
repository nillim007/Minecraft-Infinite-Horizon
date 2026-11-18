package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.logistics.LogisticsService;
import de.infinitehorizon.logistics.Route;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S (3A.5, 20.4): Route anlegen oder ändern ({@link #SAVE}) bzw. löschen ({@link #DELETE}). Der Server prüft Abstand
 * zum Logistik-Computer, Rechte und Werte und antwortet mit {@link LogisticsStatePayload}.
 */
public record LogisticsEditPayload(BlockPos computer, int action, Route route) implements CustomPacketPayload {

    public static final int SAVE = 0;
    public static final int DELETE = 1;
    /** Nur neu laden (Statusliste aktualisieren). */
    public static final int REFRESH = 2;

    public static final Type<LogisticsEditPayload> TYPE = new Type<>(InfiniteHorizon.id("logistics_edit"));
    public static final StreamCodec<ByteBuf, LogisticsEditPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LogisticsEditPayload::computer,
            ByteBufCodecs.VAR_INT, LogisticsEditPayload::action,
            Route.STREAM_CODEC, LogisticsEditPayload::route,
            LogisticsEditPayload::new);

    @Override
    public Type<LogisticsEditPayload> type() {
        return TYPE;
    }

    public static void handle(LogisticsEditPayload p, ServerPlayer player) {
        LogisticsService.apply(player, p);
    }
}
