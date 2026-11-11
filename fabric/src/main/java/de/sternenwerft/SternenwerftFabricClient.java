package de.sternenwerft;

import de.sternenwerft.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.registry.ModMenus;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SternenwerftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkHelper.clientEntries().forEach(SternenwerftFabricClient::register);
        MenuScreens.register(ModMenus.MACHINE.get(), MachineScreen::new);
    }

    private static <T extends CustomPacketPayload> void register(FabricNetworkHelper.ClientEntry<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().accept(payload));
    }
}
