package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.LaunchControlBlockEntity;
import de.infinitehorizon.space.LaunchControlMenu;
import de.infinitehorizon.space.RocketEntity;
import de.infinitehorizon.space.RocketMenu;
import de.infinitehorizon.space.StarMapMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: Aktion an einer Rakete (3A.5, 12.5): {@code start} (Navigationskarte öffnen, von der Rampe oder einer gelandeten
 * Rakete), {@code refuel} (aus Frachtraum tanken), {@code disassemble} (zerlegen), {@code skip} (Flugsequenz geschlossen).
 * {@code entityId} = Rakete (−1 = Rampe an {@code pos}).
 */
public record RocketActionPayload(String action, BlockPos pos, int entityId) implements CustomPacketPayload {

    public static final String START = "start";
    public static final String REFUEL = "refuel";
    public static final String DISASSEMBLE = "disassemble";
    public static final String SKIP = "skip";
    /** Reichweite für Aktionen an einer Rakete von außen (wie Menüs, 3A.5). */
    private static final double MAX_DISTANCE = 8.0;

    public static final Type<RocketActionPayload> TYPE = new Type<>(InfiniteHorizon.id("rocket_action"));
    public static final StreamCodec<ByteBuf, RocketActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), RocketActionPayload::action,
            BlockPos.STREAM_CODEC, RocketActionPayload::pos,
            ByteBufCodecs.VAR_INT, RocketActionPayload::entityId,
            RocketActionPayload::new);

    @Override
    public Type<RocketActionPayload> type() {
        return TYPE;
    }

    private static RocketEntity rocket(ServerPlayer player, int id) {
        if (id < 0 || !(player.level().getEntity(id) instanceof RocketEntity r)) {
            return null;
        }
        boolean near = player.getVehicle() == r || player.distanceToSqr(r) <= MAX_DISTANCE * MAX_DISTANCE;
        return near ? r : null;
    }

    public static void handle(RocketActionPayload p, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        switch (p.action()) {
            case START -> {
                if (p.entityId() >= 0) {
                    RocketEntity r = rocket(player, p.entityId());
                    if (r != null && !r.inFlight()) {
                        StarMapMenu.openForRocket(player, r);
                    }
                } else if (player.containerMenu instanceof LaunchControlMenu m && m.stillValid(player) && m.pos().equals(p.pos())
                        && level.getBlockEntity(p.pos()) instanceof LaunchControlBlockEntity be) {
                    if (!Permissions.can(player, be.storedOwner(), Permissions.Action.LAUNCH)) {
                        player.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
                        return;
                    }
                    StarMapMenu.openForPad(player, level, be);
                }
            }
            case REFUEL -> {
                RocketEntity r = rocket(player, p.entityId());
                if (r != null && player.containerMenu instanceof RocketMenu && !r.inFlight()
                        && Permissions.can(player, r.owner(), Permissions.Action.CONFIGURE)) {
                    RocketMenu.refuelFromCargo(r);
                }
            }
            case DISASSEMBLE -> {
                RocketEntity r = rocket(player, p.entityId());
                if (r != null && !r.inFlight() && Permissions.can(player, r.owner(), Permissions.Action.BREAK)) {
                    player.closeContainer();
                    FlightService.disassemble(level, r);
                }
            }
            case SKIP -> {
                if (player.getVehicle() instanceof RocketEntity r && r.inFlight()) {
                    FlightService.complete(r);
                }
            }
            default -> {
            }
        }
    }
}
