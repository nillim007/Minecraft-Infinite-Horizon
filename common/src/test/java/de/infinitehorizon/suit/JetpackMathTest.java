package de.infinitehorizon.suit;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Jetpack (9.2, 14.3): Schub, Höchstgeschwindigkeit, Abwärts, Vorwärts in Blickrichtung. */
class JetpackMathTest {

    private static final double G = 0.0004; // Schwerkraft je Tick im Orbit (0,08 × 0,005)

    @Test
    void upCancelsGravityAndCaps() {
        Vec3 v = JetpackMath.thrust(new Vec3(0, -G, 0), G, true, false, false, new Vec3(0, 0, 1), 0.12, 0.4, 0.1);
        assertEquals(0.12, v.y, 1e-9);
        Vec3 fast = JetpackMath.thrust(new Vec3(0, 0.35, 0), G, true, false, false, new Vec3(0, 0, 1), 0.12, 0.4, 0.1);
        assertEquals(0.4, fast.y, 1e-9, "höchstens 0,4 Blöcke/t");
    }

    @Test
    void downAndForward() {
        Vec3 v = JetpackMath.thrust(new Vec3(0, 0, 0), 0, false, true, true, new Vec3(1, 0.5, 1), 0.12, 0.4, 0.1);
        assertEquals(-0.12, v.y, 1e-9);
        assertEquals(0.1, Math.sqrt(v.x * v.x + v.z * v.z), 1e-9, "waagerecht 0,1 Blöcke/t");
    }
}
