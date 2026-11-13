package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S→C: Zustand des Raketen-Computers (12.4) für ein offenes {@code launch_control}-GUI: Checkliste, Rakete, Ziele,
 * Sondenziele, Countdown als JSON ({@code LaunchChecklist#toJson}). Beim Öffnen und jede Sekunde. SPEC-GAP #75: nicht
 * in der Liste 3A.5; der Client rechnet nichts.
 */
public record LaunchStatePayload(BlockPos pos, String json) implements CustomPacketPayload {

    public static final int MAX_LENGTH = 1 << 16;
    public static final Type<LaunchStatePayload> TYPE = new Type<>(InfiniteHorizon.id("launch_state"));
    public static final StreamCodec<ByteBuf, LaunchStatePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LaunchStatePayload::pos,
            ByteBufCodecs.stringUtf8(MAX_LENGTH), LaunchStatePayload::json,
            LaunchStatePayload::new);

    @Override
    public Type<LaunchStatePayload> type() {
        return TYPE;
    }
}
