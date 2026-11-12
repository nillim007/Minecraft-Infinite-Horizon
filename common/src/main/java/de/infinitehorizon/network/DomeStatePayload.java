package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.dome.DomeData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * S→C: Kuppeln der aktuellen Dimension (3A.5, 10.1): je Kuppel Mittelpunkt, Radius, Stufe, Zustand und Agentur. Bei
 * Login, Dimensionswechsel und jeder Änderung.
 */
public record DomeStatePayload(List<Dome> domes) implements CustomPacketPayload {

    /** Eine Kuppel für die Darstellung. */
    public record Dome(BlockPos center, int radius, int tier, boolean failing, Optional<UUID> agency) {

        public static final StreamCodec<ByteBuf, Dome> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Dome::center,
                ByteBufCodecs.VAR_INT, Dome::radius,
                ByteBufCodecs.VAR_INT, Dome::tier,
                ByteBufCodecs.BOOL, Dome::failing,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Dome::agency,
                Dome::new);
    }

    public static final Type<DomeStatePayload> TYPE = new Type<>(InfiniteHorizon.id("dome_state"));
    public static final StreamCodec<ByteBuf, DomeStatePayload> CODEC =
            Dome.CODEC.apply(ByteBufCodecs.list()).map(DomeStatePayload::new, DomeStatePayload::domes);

    public static DomeStatePayload of(List<DomeData.Entry> entries) {
        return new DomeStatePayload(entries.stream()
                .map(e -> new Dome(e.center(), e.radius(), e.tier(), e.failing(), e.agency())).toList());
    }

    @Override
    public Type<DomeStatePayload> type() {
        return TYPE;
    }
}
