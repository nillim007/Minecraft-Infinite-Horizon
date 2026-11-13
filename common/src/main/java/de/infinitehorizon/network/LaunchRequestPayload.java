package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.LaunchControlMenu;
import de.infinitehorizon.space.ProbeService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: Startanfrage (3A.5, 12.5). {@code pos} = Position von {@code launch_control}, {@code entityId} = gelandetes
 * Raketen-Entity (−1 = Rampe), {@code target} = Ziel, Landeort als Landefeuer ({@code beacon}, sonst
 * {@link #NO_BEACON}) oder X/Z. In M4d nur für Sonden (12.3, SPEC-GAP #73); Raketen fliegen ab M4e.
 */
public record LaunchRequestPayload(BlockPos pos, int entityId, Identifier target, BlockPos beacon, int x, int z)
        implements CustomPacketPayload {

    public static final BlockPos NO_BEACON = new BlockPos(0, Integer.MIN_VALUE / 2, 0);
    public static final Type<LaunchRequestPayload> TYPE = new Type<>(InfiniteHorizon.id("launch_request"));
    public static final StreamCodec<ByteBuf, LaunchRequestPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LaunchRequestPayload::pos,
            ByteBufCodecs.VAR_INT, LaunchRequestPayload::entityId,
            Identifier.STREAM_CODEC, LaunchRequestPayload::target,
            BlockPos.STREAM_CODEC, LaunchRequestPayload::beacon,
            ByteBufCodecs.VAR_INT, LaunchRequestPayload::x,
            ByteBufCodecs.VAR_INT, LaunchRequestPayload::z,
            LaunchRequestPayload::new);

    /** Sondenstart an {@code pos} zu {@code target}. */
    public static LaunchRequestPayload probe(BlockPos pos, Identifier target) {
        return new LaunchRequestPayload(pos, -1, target, NO_BEACON, 0, 0);
    }

    @Override
    public Type<LaunchRequestPayload> type() {
        return TYPE;
    }

    public static void handle(LaunchRequestPayload p, ServerPlayer player) {
        if (!(player.containerMenu instanceof LaunchControlMenu menu) || !menu.stillValid(player) || !menu.pos().equals(p.pos())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !(level.getBlockEntity(p.pos()) instanceof LaunchControlBlockEntity be)) {
            return;
        }
        if (!Permissions.can(player, be.storedOwner(), Permissions.Action.LAUNCH)) {
            player.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
            return;
        }
        Agency agency = ProbeService.agencyOf(player);
        String error = be.requestProbeLaunch(level, agency, p.target());
        if (error != null) {
            player.sendOverlayMessage(Component.translatable("gui.infinite_horizon.launch.denied." + error));
        }
        be.sendState(player);
    }
}
