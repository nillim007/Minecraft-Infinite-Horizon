package de.sternenwerft.platform;

import de.sternenwerft.platform.services.INetworkHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Payloads werden gesammelt und in {@link RegisterPayloadHandlersEvent} (Mod-Bus) angemeldet. */
public class NeoForgeNetworkHelper implements INetworkHelper {

    private static final String PROTOCOL = "1";
    private static final List<Entry<?>> ENTRIES = new ArrayList<>();

    private record Entry<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         Consumer<T> handler) {
        void register(PayloadRegistrar registrar) {
            registrar.playToClient(type, codec, (payload, context) -> handler.accept(payload));
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type,
                                                                    StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                    Consumer<T> clientHandler) {
        ENTRIES.add(new Entry<>(type, codec, clientHandler));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        ENTRIES.forEach(e -> e.register(registrar));
    }
}
