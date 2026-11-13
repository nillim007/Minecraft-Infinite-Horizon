package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S→C: Flugsequenz für die Insassen (3A.5, 12.5): Ziel, Sequenztyp ({@link #START} = Countdown und Aufstieg,
 * {@link #LANDING}, {@link #END}) und Dauer in Ticks. Nur Darstellung (Kamera von außen, HUD aus).
 */
public record FlightSequencePayload(Identifier target, byte kind, int ticks) implements CustomPacketPayload {

    public static final byte START = 0;
    public static final byte LANDING = 1;
    public static final byte END = 2;

    public static final Type<FlightSequencePayload> TYPE = new Type<>(InfiniteHorizon.id("flight_sequence"));
    public static final StreamCodec<ByteBuf, FlightSequencePayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, FlightSequencePayload::target,
            ByteBufCodecs.BYTE, FlightSequencePayload::kind,
            ByteBufCodecs.VAR_INT, FlightSequencePayload::ticks,
            FlightSequencePayload::new);

    @Override
    public Type<FlightSequencePayload> type() {
        return TYPE;
    }
}
