package de.sternenwerft;

import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import de.sternenwerft.registry.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;

/** Nur auf dem Client: Screens registrieren. */
@Mod(value = Sternenwerft.MOD_ID, dist = Dist.CLIENT)
public class SternenwerftNeoForgeClient {

    public SternenwerftNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModMenus.MACHINE.get(), MachineScreen::new));
        // Fluid-Texturen (26.3: FluidModel statt Client-Extensions), Farbe aus fluids.json
        modBus.addListener((RegisterFluidModelsEvent e) -> ModFluids.handles().forEach((id, h) -> e.register(
                new FluidModel.Unbaked(new Material(Sternenwerft.id("block/" + id + "_still")),
                        new Material(Sternenwerft.id("block/" + id + "_flow")), null,
                        FluidTintSources.constant(ModFluids.defs().get(id).color())),
                h.source().get(), h.flowing().get())));
    }
}
