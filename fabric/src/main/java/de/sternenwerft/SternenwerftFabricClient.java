package de.sternenwerft;

import de.sternenwerft.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SternenwerftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkHelper.clientEntries().forEach(SternenwerftFabricClient::register);
    }

    private static <T extends CustomPacketPayload> void register(FabricNetworkHelper.ClientEntry<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().accept(payload));
    }
}
