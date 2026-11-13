package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.space.LandingBeaconBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: Name eines Landefeuers (3A.5, 12.6). Der Server prüft Abstand (≤ 8 Blöcke), Recht {@code CONFIGURE} und Länge
 * ({@code name_max_length}).
 */
public record NamePayload(BlockPos pos, String name) implements CustomPacketPayload {

    private static final double MAX_DISTANCE = 8.0;
    /** Obergrenze im Netzwerk; gekürzt wird nach {@code name_max_length}. */
    private static final int WIRE_LIMIT = 256;

    public static final Type<NamePayload> TYPE = new Type<>(InfiniteHorizon.id("name"));
    public static final StreamCodec<ByteBuf, NamePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NamePayload::pos,
            ByteBufCodecs.stringUtf8(WIRE_LIMIT), NamePayload::name,
            NamePayload::new);

    @Override
    public Type<NamePayload> type() {
        return TYPE;
    }

    public static void handle(NamePayload p, ServerPlayer player) {
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.pos())) > MAX_DISTANCE * MAX_DISTANCE
                || !(player.level().getBlockEntity(p.pos()) instanceof LandingBeaconBlockEntity be)) {
            return;
        }
        if (!Permissions.can(player, be.storedOwner(), Permissions.Action.CONFIGURE)) {
            player.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
            return;
        }
        be.rename(p.name());
    }
}
