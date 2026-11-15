package de.infinitehorizon.client;

import de.infinitehorizon.suit.Jetpack;
import net.minecraft.client.Minecraft;

/** Jetpack (9.2): Schub für den eigenen Spieler je Client-Tick aus den Tasten Springen, Schleichen und Vorwärts. */
public final class JetpackClient {

    private JetpackClient() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            return;
        }
        var opts = mc.options;
        Jetpack.applyClient(mc.player, opts.keyJump.isDown(), opts.keyShift.isDown(), opts.keyUp.isDown());
    }
}
