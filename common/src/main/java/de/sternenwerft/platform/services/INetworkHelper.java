package de.sternenwerft.platform.services;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Payloads (3A.5) je Loader registrieren und senden. */
public interface INetworkHelper {

    /** Server → Client. Der Handler läuft auf dem Client-Hauptthread und darf nur Daten ablegen. */
    <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                             Consumer<T> clientHandler);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Client → Server. Der Handler läuft auf dem Server-Hauptthread und MUSS die Werte prüfen (3A.5). */
    <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type,
                                                             StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                             BiConsumer<T, ServerPlayer> serverHandler);

    /** Nur auf dem Client aufrufen. */
    void sendToServer(CustomPacketPayload payload);
}
