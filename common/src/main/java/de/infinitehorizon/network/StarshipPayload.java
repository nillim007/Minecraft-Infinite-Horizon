package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.vehicle.StarshipEntity;
import de.infinitehorizon.vehicle.StarshipService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Sternenschiff (21.3): Sprungziele (S→C) und Sprung (C→S). */
public final class StarshipPayload {

    private StarshipPayload() {
    }

    /** S→C: Sprungziele für das Schiff {@code entityId}. */
    public record Targets(int entityId, List<Identifier> targets) implements CustomPacketPayload {
        public static final Type<Targets> TYPE = new Type<>(InfiniteHorizon.id("starship_targets"));
        public static final StreamCodec<ByteBuf, Targets> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Targets::entityId,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), Targets::targets,
                Targets::new);

        @Override
        public Type<Targets> type() {
            return TYPE;
        }
    }

    /** C→S: Sprung des Schiffs {@code entityId} nach {@code target}. */
    public record Jump(int entityId, Identifier target) implements CustomPacketPayload {
        public static final Type<Jump> TYPE = new Type<>(InfiniteHorizon.id("starship_jump"));
        public static final StreamCodec<ByteBuf, Jump> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Jump::entityId,
                Identifier.STREAM_CODEC, Jump::target,
                Jump::new);

        @Override
        public Type<Jump> type() {
            return TYPE;
        }

        public static void handle(Jump j, ServerPlayer player) {
            if (player.getVehicle() instanceof StarshipEntity ship && ship.getId() == j.entityId()) {
                StarshipService.Jump r = StarshipService.jump(player, ship, j.target());
                if (r != StarshipService.Jump.OK) {
                    player.sendOverlayMessage(Component.translatable("message.infinite_horizon.starship."
                            + r.name().toLowerCase(java.util.Locale.ROOT)));
                }
            }
        }
    }
}
