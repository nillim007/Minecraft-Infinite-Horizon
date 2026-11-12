package de.infinitehorizon.client;

import de.infinitehorizon.dome.DomeManager;
import de.infinitehorizon.network.DomeStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Darstellung der Kuppeln (10.1) auf dem Client: Lichtpunkte auf der Halbkugel-Hülle, T1 weißlich, T2 blau, T3 violett,
 * beim Ausfall rot pulsierend; nur Kuppeln innerhalb von {@code render_distance}. SPEC-GAP #60: statt der
 * halbtransparenten Hülle mit Sechseckmuster vorerst Partikel (wie die Geisterprojektion, 4.1); die Hülle folgt mit dem
 * grafischen Feinschliff.
 */
public final class DomeVisuals {

    private static final int[] TIER_COLORS = {0xF0F0F0, 0xF0F0F0, 0x5A8CFF, 0xB070FF};
    private static final int FAILING = 0xFF3030;
    /** Partikel je Kuppel und Durchgang, Takt in Ticks. */
    private static final int POINTS = 24;
    private static final int EVERY = 4;

    private static List<DomeStatePayload.Dome> domes = List.of();
    private static int tick;

    private DomeVisuals() {
    }

    public static void receive(DomeStatePayload payload) {
        domes = payload.domes();
    }

    public static List<DomeStatePayload.Dome> domes() {
        return domes;
    }

    /** Jeder Client-Tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || domes.isEmpty() || mc.isPaused()) {
            return;
        }
        if (++tick % EVERY != 0) {
            return;
        }
        var rules = DomeManager.rules();
        int distance = rules.has("render_distance") ? rules.get("render_distance").getAsInt() : 0;
        int below = rules.has("below_center_offset") ? rules.get("below_center_offset").getAsInt() : 0;
        Vec3 eye = mc.player.position();
        RandomSource random = mc.level.getRandom();
        for (DomeStatePayload.Dome d : domes) {
            Vec3 c = Vec3.atCenterOf(d.center());
            if (c.distanceTo(eye) > distance + d.radius()) {
                continue;
            }
            boolean pulseOn = !d.failing() || (tick / 10) % 2 == 0;
            if (!pulseOn) {
                continue;
            }
            int color = d.failing() ? FAILING : TIER_COLORS[Math.min(d.tier(), TIER_COLORS.length - 1)];
            DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
            for (int i = 0; i < POINTS; i++) {
                // gleichmäßig auf der Halbkugel: Höhe y ≥ Mittelpunkt − below
                double phi = random.nextDouble() * Math.PI * 2;
                double cos = random.nextDouble();
                double sin = Math.sqrt(1 - cos * cos);
                double x = c.x + d.radius() * sin * Math.cos(phi);
                double y = c.y - below + (d.radius()) * cos;
                double z = c.z + d.radius() * sin * Math.sin(phi);
                mc.level.addParticle(dust, x, y, z, 0, 0, 0);
            }
        }
    }
}
