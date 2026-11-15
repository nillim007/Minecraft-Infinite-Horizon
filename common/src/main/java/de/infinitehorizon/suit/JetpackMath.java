package de.infinitehorizon.suit;

import net.minecraft.world.phys.Vec3;

/** Reine Rechnung des Jetpacks (9.2, 14.3), ohne Registry-Zugriff testbar. */
public final class JetpackMath {

    private JetpackMath() {
    }

    /**
     * Neue Geschwindigkeit (reine Rechnung): Schwerkraft {@code gravity} wird aufgehoben, Springen beschleunigt um
     * {@code accel} nach oben bis {@code maxUp}, Schleichen ebenso nach unten; Vorwärts setzt waagerecht Blickrichtung ×
     * {@code horizontal}.
     */
    public static Vec3 thrust(Vec3 v, double gravity, boolean up, boolean down, boolean forward, Vec3 look,
                              double accel, double maxUp, double horizontal) {
        double y = v.y + gravity;
        if (up) {
            y = Math.min(y + accel, maxUp);
        } else if (down) {
            y = Math.max(y - accel, -maxUp);
        }
        double x = v.x;
        double z = v.z;
        if (forward) {
            double len = Math.sqrt(look.x * look.x + look.z * look.z);
            if (len > 1e-6) {
                x = look.x / len * horizontal;
                z = look.z / len * horizontal;
            }
        }
        return new Vec3(x, y, z);
    }
}
