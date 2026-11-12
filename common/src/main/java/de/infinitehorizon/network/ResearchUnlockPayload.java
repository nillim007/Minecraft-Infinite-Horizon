package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.research.ResearchService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: Klick auf einen verfügbaren Knoten im Forschungsterminal (3A.5, 11.3). Der Server prüft alles selbst
 * ({@link ResearchService#researchFromTerminal}).
 */
public record ResearchUnlockPayload(Identifier node) implements CustomPacketPayload {

    public static final Type<ResearchUnlockPayload> TYPE = new Type<>(InfiniteHorizon.id("research_unlock"));
    public static final StreamCodec<ByteBuf, ResearchUnlockPayload> CODEC =
            Identifier.STREAM_CODEC.map(ResearchUnlockPayload::new, ResearchUnlockPayload::node);

    @Override
    public Type<ResearchUnlockPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchUnlockPayload payload, ServerPlayer player) {
        ResearchService.researchFromTerminal(player, payload.node());
    }
}
