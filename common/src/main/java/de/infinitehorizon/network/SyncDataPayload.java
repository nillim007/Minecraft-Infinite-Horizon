package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S→C: Datenpaket-Daten für den Client (3A.5), beim Join und nach {@code /reload}. Ab M3 der Planeten-Teil (13.1)
 * als JSON-Liste, ab M4 der Forschungs-Teil (11.4, {@code ResearchRegistry#toJson}). SPEC-GAP #41: ohne Kompression
 * und Teilung; die Grenze liegt bei {@link #MAX_LENGTH} Zeichen je Teil.
 */
public record SyncDataPayload(String planets, String research) implements CustomPacketPayload {

    public static final int MAX_LENGTH = 1 << 20;
    public static final Type<SyncDataPayload> TYPE = new Type<>(InfiniteHorizon.id("sync_data"));
    public static final StreamCodec<ByteBuf, SyncDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_LENGTH), SyncDataPayload::planets,
            ByteBufCodecs.stringUtf8(MAX_LENGTH), SyncDataPayload::research,
            SyncDataPayload::new);

    @Override
    public Type<SyncDataPayload> type() {
        return TYPE;
    }
}
