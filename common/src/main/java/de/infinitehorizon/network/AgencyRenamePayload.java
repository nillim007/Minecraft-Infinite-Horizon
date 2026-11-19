package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.AgencyActions;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.research.ResearchTerminalMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** C→S (23.1, ab M9): Agentur umbenennen; nur am offenen Forschungsterminal, prüft der Server. */
public record AgencyRenamePayload(String name) implements CustomPacketPayload {

    public static final Type<AgencyRenamePayload> TYPE = new Type<>(InfiniteHorizon.id("agency_rename"));
    public static final StreamCodec<ByteBuf, AgencyRenamePayload> CODEC =
            ByteBufCodecs.stringUtf8(AgencyData.MAX_NAME * 4).map(AgencyRenamePayload::new, AgencyRenamePayload::name);

    @Override
    public Type<AgencyRenamePayload> type() {
        return TYPE;
    }

    public static void handle(AgencyRenamePayload p, ServerPlayer player) {
        if (player.containerMenu instanceof ResearchTerminalMenu menu && menu.stillValid(player)) {
            AgencyActions.rename(player, p.name());
        }
    }
}
