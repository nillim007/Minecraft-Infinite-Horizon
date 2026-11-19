package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/** S→C (21.3): Finale starten; besuchte Dimensionen der Agentur und Länge der Sequenz in Ticks. */
public record FinalePayload(List<Identifier> visited, int sequenceTicks) implements CustomPacketPayload {

    public static final Type<FinalePayload> TYPE = new Type<>(InfiniteHorizon.id("finale"));
    public static final StreamCodec<ByteBuf, FinalePayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), FinalePayload::visited,
            ByteBufCodecs.VAR_INT, FinalePayload::sequenceTicks,
            FinalePayload::new);

    @Override
    public Type<FinalePayload> type() {
        return TYPE;
    }
}
