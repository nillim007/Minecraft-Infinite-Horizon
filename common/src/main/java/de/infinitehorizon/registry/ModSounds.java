package de.infinitehorizon.registry;

import com.google.gson.JsonElement;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.GeneratedData;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound-Events (24.6). Die Liste ({@code infinite_horizon/sound_events.json}) und {@code sounds.json} mit Verweisen auf
 * Vanilla-Klangdateien erzeugt tools/gen_resources.py; M3 hat die Warntöne {@code warning.*}.
 */
public final class ModSounds {

    static {
        for (JsonElement e : GeneratedData.array("sound_events", "events")) {
            String name = e.getAsString();
            Services.REGISTRY.register(BuiltInRegistries.SOUND_EVENT, name,
                    () -> SoundEvent.createVariableRangeEvent(InfiniteHorizon.id(name)));
        }
    }

    private ModSounds() {
    }

    public static void init() {
    }
}
