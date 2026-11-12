package de.infinitehorizon;

import de.infinitehorizon.platform.FabricNetworkHelper;
import net.fabricmc.api.ClientModInitializer;
import de.infinitehorizon.client.MachineScreen;
import de.infinitehorizon.registry.ModFluids;
import de.infinitehorizon.registry.ModMenus;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class InfiniteHorizonFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkHelper.clientEntries().forEach(InfiniteHorizonFabricClient::register);
        MenuScreens.register(ModMenus.MACHINE.get(), MachineScreen::new);
        // Synchronisierte Maschinenrezepte für JEI/REI (3.9), beim Abfragen aus der Client-Welt gelesen
        de.infinitehorizon.compat.ClientRecipes.setSource(type -> {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? java.util.List.of() : level.recipeAccess().getSynchronizedRecipes().getAllOfType(type);
        });
        // Ingenieurs-Handbuch (24.1): Taste H, Tastendruck über Items in Inventaren, Tooltip-Hinweis
        de.infinitehorizon.client.handbook.HandbookClient.init();
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                de.infinitehorizon.client.handbook.HandbookClient.KEY);
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.allowKeyPress(screen).register((s, e) ->
                        !de.infinitehorizon.client.handbook.HandbookClient.onScreenKey(s, e)));
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) ->
                de.infinitehorizon.client.handbook.HandbookClient.appendTooltip(stack, lines));
        // Fluid-Texturen (26.3: FluidModel), Farbe aus fluids.json
        ModFluids.handles().forEach((id, h) -> {
            int color = ModFluids.defs().get(id).color();
            FluidRenderingRegistry.register(h.source().get(), h.flowing().get(), new FluidModel.Unbaked(
                    new Material(InfiniteHorizon.id("block/" + id + "_still")), new Material(InfiniteHorizon.id("block/" + id + "_flow")),
                    null, state -> color));
        });
    }

    private static <T extends CustomPacketPayload> void register(FabricNetworkHelper.ClientEntry<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().accept(payload));
    }
}
