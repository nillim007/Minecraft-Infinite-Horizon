package de.sternenwerft.platform;

import de.sternenwerft.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Payload-Typen werden sofort angemeldet; Client-Empfänger registriert {@code SternenwerftFabricClient}. */
public class FabricNetworkHelper implements INetworkHelper {

    public record ClientEntry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
    }

    private static final List<ClientEntry<?>> CLIENT_ENTRIES = new ArrayList<>();

    public static List<ClientEntry<?>> clientEntries() {
        return Collections.unmodifiableList(CLIENT_ENTRIES);
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type,
                                                                    StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                    Consumer<T> clientHandler) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        CLIENT_ENTRIES.add(new ClientEntry<>(type, clientHandler));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
