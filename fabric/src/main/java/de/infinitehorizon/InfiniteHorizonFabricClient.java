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
        MenuScreens.register(ModMenus.RESEARCH_TERMINAL.get(), de.infinitehorizon.client.ResearchTerminalScreen::new);
        MenuScreens.register(ModMenus.LAUNCH_CONTROL.get(), de.infinitehorizon.client.LaunchControlScreen::new);
        MenuScreens.register(ModMenus.STAR_MAP.get(), de.infinitehorizon.client.StarMapScreen::new);
        MenuScreens.register(ModMenus.ROCKET.get(), de.infinitehorizon.client.RocketScreen::new);
        // Entities (M4): Sonde als Block, Sitz unsichtbar
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                de.infinitehorizon.registry.ModEntities.PROBE_ENTITY.get(), de.infinitehorizon.client.ProbeRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                de.infinitehorizon.registry.ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        // Rakete aus ihren Teilen, roter Strahl des Landefeuers (M4e)
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                de.infinitehorizon.registry.ModEntities.ROCKET.get(), de.infinitehorizon.client.RocketRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                de.infinitehorizon.registry.ModBlockEntities.LANDING_BEACON.get(), ctx -> new de.infinitehorizon.client.LandingBeaconRenderer());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                client -> de.infinitehorizon.client.DomeVisuals.tick());
        // Umwelt-HUD (8.4) über der Hungerleiste
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.FOOD_BAR,
                de.infinitehorizon.InfiniteHorizon.id("hazards"), de.infinitehorizon.client.HazardHud::render);
        // Atemmaske der Planetenmonster (13.5)
        java.util.List<net.minecraft.world.entity.EntityType<?>> masked = de.infinitehorizon.client.BreathingMaskLayer.types();
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (type, renderer, helper, context) -> {
                    if (masked.contains(type)) {
                        addMask(helper, renderer, type);
                    }
                });
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addMask(net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.RegistrationHelper helper,
                                net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer,
                                net.minecraft.world.entity.EntityType<?> type) {
        helper.register(new de.infinitehorizon.client.BreathingMaskLayer(renderer, type));
    }

    private static <T extends CustomPacketPayload> void register(FabricNetworkHelper.ClientEntry<T> entry) {
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> entry.handler().accept(payload));
    }
}
