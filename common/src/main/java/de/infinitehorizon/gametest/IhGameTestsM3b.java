package de.infinitehorizon.gametest;

import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.platform.Services;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GameTests für M3b (27.5): Belastung im Override Mond (Abnahme 1), Schwerkraft und sichere Fallhöhe, keine
 * Belastung im Kreativmodus.
 */
public final class IhGameTestsM3b {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");

    private IhGameTestsM3b() {
    }

    private static ServerPlayer player(GameTestHelper h, GameType mode) {
        ServerPlayer p = IhTestPlayers.inList(h, mode, new Vec3(2.5, 1, 2.5));
        Services.ATTACHMENTS.hazardState(p).reset();
        return p;
    }

    private static int buildupTicks(Hazard h) {
        return HazardTracker.rules().hazards().get(h).buildupTicks();
    }

    private static double fraction(ServerPlayer p, Hazard h) {
        HazardState s = Services.ATTACHMENTS.hazardState(p);
        return s.fraction(h, buildupTicks(h));
    }

    /**
     * Abnahme 1: Überlebensmodus, ohne Anzug, Override Mond, Tageszeit 6.000 (Testumgebung {@code moon_day}): nach
     * 190 t unter 100 %, nach 200 t (Toleranz ein Prüftakt) 100 % Sauerstoff- und Druck-Belastung.
     */
    public static void moonVacuumHazardBuildup(GameTestHelper h) {
        ServerPlayer p = player(h, GameType.SURVIVAL);
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 400);
        AtomicBoolean checked190 = new AtomicBoolean();
        h.runAtTickTime(190, () -> {
            h.assertTrue(fraction(p, Hazard.OXYGEN) < 1.0, "Sauerstoff nach 190 t schon " + fraction(p, Hazard.OXYGEN));
            h.assertTrue(fraction(p, Hazard.PRESSURE) < 1.0, "Druck nach 190 t schon " + fraction(p, Hazard.PRESSURE));
            checked190.set(true);
        });
        h.succeedWhen(() -> {
            h.assertTrue(checked190.get(), "Prüfung bei 190 t fehlt");
            h.assertTrue(fraction(p, Hazard.OXYGEN) >= 1.0, "Sauerstoff " + fraction(p, Hazard.OXYGEN));
            h.assertTrue(fraction(p, Hazard.PRESSURE) >= 1.0, "Druck " + fraction(p, Hazard.PRESSURE));
            h.assertTrue(fraction(p, Hazard.HEAT) >= 1.0, "Mittag auf dem Mond: Hitze " + fraction(p, Hazard.HEAT));
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
        });
    }

    /** Derselbe Spieler hat im Override Mond die Schwerkraft 0,0136 und eine sichere Fallhöhe von 17,6 Blöcken. */
    public static void moonGravity(GameTestHelper h) {
        ServerPlayer p = player(h, GameType.SURVIVAL);
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 40);
        GravityService.apply(p);
        double gravity = p.getAttributeValue(Attributes.GRAVITY);
        double safeFall = p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double fallDamage = p.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        h.assertTrue(Math.abs(gravity - 0.0136) < 1e-6, "Schwerkraft " + gravity);
        h.assertTrue(Math.round(safeFall * 10) / 10.0 == 17.6, "sichere Fallhöhe " + safeFall);
        h.assertTrue(Math.abs(fallDamage - 0.17) < 1e-6, "Fallschaden-Faktor " + fallDamage);
        EnvironmentOverrides.remove(override);
        GravityService.apply(p);
        double earth = p.getAttributeValue(Attributes.GRAVITY);
        h.assertTrue(Math.abs(earth - 0.08) < 1e-9, "Schwerkraft nach dem Override " + earth);
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Kreativmodus im Override Mond: keine Belastung (8.2). */
    public static void creativeNoBuildup(GameTestHelper h) {
        ServerPlayer p = player(h, GameType.CREATIVE);
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 60);
        h.runAtTickTime(50, () -> {
            h.assertTrue(HazardTracker.lastResult(p) != null, "keine Umweltprüfung");
            h.assertTrue(HazardTracker.lastResult(p).has(Hazard.OXYGEN), "Mond gilt nicht als Vakuum");
            h.assertTrue(fraction(p, Hazard.OXYGEN) == 0, "Belastung im Kreativmodus " + fraction(p, Hazard.OXYGEN));
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
            h.succeed();
        });
    }
}
