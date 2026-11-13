package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.AgencyActions;
import de.infinitehorizon.research.ResearchTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * C→S: Agentur-Aktion im Reiter „Agentur“ (3A.5, 23.1): {@code invite} (Spieler), {@code accept} / {@code decline}
 * (Agentur), {@code leave} ({@code target} ohne Bedeutung). Der Server prüft, dass ein {@code research_terminal}
 * offen und gültig ist; die Regeln prüft {@link AgencyActions}.
 */
public record AgencyActionPayload(String action, UUID target) implements CustomPacketPayload {

    public static final UUID NONE = new UUID(0L, 0L);
    public static final Type<AgencyActionPayload> TYPE = new Type<>(InfiniteHorizon.id("agency_action"));
    public static final StreamCodec<ByteBuf, AgencyActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), AgencyActionPayload::action,
            UUIDUtil.STREAM_CODEC, AgencyActionPayload::target,
            AgencyActionPayload::new);

    @Override
    public Type<AgencyActionPayload> type() {
        return TYPE;
    }

    public static void handle(AgencyActionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof ResearchTerminalMenu menu) || !menu.stillValid(player)) {
            return;
        }
        AgencyActions.perform(player, payload.action(), payload.target());
    }
}
