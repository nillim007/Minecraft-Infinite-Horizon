package de.sternenwerft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Sternenwerft.MOD_ID)
public class SternenwerftNeoForge {

    public SternenwerftNeoForge(IEventBus modEventBus) {
        Sternenwerft.init();
    }
}
