package de.sternenwerft;

import de.sternenwerft.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.registry.ModFluids;
import de.sternenwerft.registry.ModMenus;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SternenwerftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkHelper.clientEntries().forEach(SternenwerftFabricClient::register);
        MenuScreens.register(ModMenus.MACHINE.get(), MachineScreen::new);
        // Fluid-Texturen (26.3: FluidModel), Farbe aus fluids.json
        ModFluids.handles().forEach((id, h) -> {
            int color = ModFluids.defs().get(id).color();
            FluidRenderingRegistry.register(h.source().get(), h.flowing().get(), new FluidModel.Unbaked(
                    new Material(Sternenwerft.id("block/" + id + "_still")), new Material(Sternenwerft.id("block/" + id + "_flow")),
                    null, state -> color));
        });
    }

    private static <T extends CustomPacketPayload> void register(FabricNetworkHelper.ClientEntry<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().accept(payload));
    }
}
