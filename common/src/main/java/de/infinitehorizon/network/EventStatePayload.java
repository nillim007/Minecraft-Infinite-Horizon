package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S→C: Ereignis der Dimension für das HUD-Symbol (19): ID (leer = keins), Phase (1 Vorwarnung, 2 aktiv) und
 * Rest-Ticks bis Start bzw. Ende.
 */
public record EventStatePayload(String event, byte phase, int ticks) implements CustomPacketPayload {

    public static final Type<EventStatePayload> TYPE = new Type<>(InfiniteHorizon.id("event_state"));
    public static final StreamCodec<ByteBuf, EventStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EventStatePayload::event,
            ByteBufCodecs.BYTE, EventStatePayload::phase,
            ByteBufCodecs.VAR_INT, EventStatePayload::ticks,
            EventStatePayload::new);

    @Override
    public Type<EventStatePayload> type() {
        return TYPE;
    }
}
