package de.sternenwerft.network;

import de.sternenwerft.Sternenwerft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S→C: Serverkonfiguration (24.4) als JSON, beim Join und bei Änderung (3A.5). */
public record SyncConfigPayload(String json) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE = new Type<>(Sternenwerft.id("sync_config"));
    public static final StreamCodec<ByteBuf, SyncConfigPayload> CODEC =
            ByteBufCodecs.STRING_UTF8.map(SyncConfigPayload::new, SyncConfigPayload::json);

    @Override
    public Type<SyncConfigPayload> type() {
        return TYPE;
    }
}
