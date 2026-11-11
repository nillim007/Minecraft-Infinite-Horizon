package de.sternenwerft;

import de.sternenwerft.client.MachineScreen;
import de.sternenwerft.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Nur auf dem Client: Screens registrieren. */
@Mod(value = Sternenwerft.MOD_ID, dist = Dist.CLIENT)
public class SternenwerftNeoForgeClient {

    public SternenwerftNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModMenus.MACHINE.get(), MachineScreen::new));
    }
}
