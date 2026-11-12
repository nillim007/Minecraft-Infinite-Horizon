package de.infinitehorizon;

import de.infinitehorizon.client.MachineScreen;
import de.infinitehorizon.client.NeoForgeClientTest;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import de.infinitehorizon.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import de.infinitehorizon.registry.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

/** Nur auf dem Client: Screens registrieren. */
@Mod(value = InfiniteHorizon.MOD_ID, dist = Dist.CLIENT)
public class InfiniteHorizonNeoForgeClient {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addMask(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer,
                                net.minecraft.world.entity.EntityType<?> type) {
        ((de.infinitehorizon.mixin.LivingEntityRendererAccessor) renderer).infinite_horizon$addLayer(
                new de.infinitehorizon.client.BreathingMaskLayer(renderer, type));
    }

    public InfiniteHorizonNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModMenus.MACHINE.get(), MachineScreen::new));
        // Umwelt-HUD (8.4) über der Hungerleiste
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerAbove(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.FOOD_LEVEL, de.infinitehorizon.InfiniteHorizon.id("hazards"),
                de.infinitehorizon.client.HazardHud::render));
        // Atemmaske der Planetenmonster (13.5)
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers e) -> {
            for (net.minecraft.world.entity.EntityType<?> type : de.infinitehorizon.client.BreathingMaskLayer.types()) {
                if (e.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> r) {
                    addMask(r, type);
                }
            }
        });
        // Empfangene Maschinenrezepte für JEI/REI (3.9); vor den Anzeige-Mods, daher höchste Priorität
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
                (net.neoforged.neoforge.client.event.RecipesReceivedEvent e) -> {
                    net.minecraft.world.item.crafting.RecipeMap map = e.getRecipeMap();
                    de.infinitehorizon.compat.ClientRecipes.setSource(map::byType);
                });
        // Ingenieurs-Handbuch (24.1): Taste H, Tastendruck über Items in Inventaren, Tooltip-Hinweis
        de.infinitehorizon.client.handbook.HandbookClient.init();
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) ->
                e.register(de.infinitehorizon.client.handbook.HandbookClient.KEY));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre e) -> {
            if (de.infinitehorizon.client.handbook.HandbookClient.onScreenKey(e.getScreen(), e.getKeyEvent())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) ->
                de.infinitehorizon.client.handbook.HandbookClient.appendTooltip(e.getItemStack(), e.getToolTip()));
        if (NeoForgeClientTest.enabled()) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> NeoForgeClientTest.tick());
        }
        // Fluid-Texturen (26.3: FluidModel statt Client-Extensions), Farbe aus fluids.json
        modBus.addListener((RegisterFluidModelsEvent e) -> ModFluids.handles().forEach((id, h) -> e.register(
                new FluidModel.Unbaked(new Material(InfiniteHorizon.id("block/" + id + "_still")),
                        new Material(InfiniteHorizon.id("block/" + id + "_flow")), null,
                        FluidTintSources.constant(ModFluids.defs().get(id).color())),
                h.source().get(), h.flowing().get())));
    }
}
