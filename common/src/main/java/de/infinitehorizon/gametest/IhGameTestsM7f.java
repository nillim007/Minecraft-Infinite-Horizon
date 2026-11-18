package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abnahme M7 (27): Jedes Ziel ist mit der vorgesehenen Ausrüstung betretbar (keine Gefahr) und ohne sie tödlich (mindestens
 * eine Gefahr, die Belastung baut sich auf und warnt im HUD). Ausrüstung nach der Tabelle in 13.2, Orte über den
 * Umwelt-Override (Planet + Zone).
 */
public final class IhGameTestsM7f {

    private static final String T1 = "pressure_suit";
    private static final String T2 = "heat_suit";
    private static final String T3 = "deep_space_suit";
    private static final String SHIELD = "radiation_shield";
    private static final String HEATING = "heating_module";
    private static final String DIVING = "diving_module";
    /**
     * Zone, die es nicht gibt: Es gelten nur die Werte des Planeten (Oberfläche). Ohne Angabe würden die Höhenzonen nach
     * der Höhe des Testbereichs greifen (z. B. Tiefsee der Europa unter y 80).
     */
    private static final String SURFACE = "-";
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Ort und Ausrüstung: {@code suit} null = ohne Anzug; erwartet sicher (keine Gefahr) oder tödlich. */
    record Case(String planet, @Nullable String zone, @Nullable String suit, List<String> modules, boolean safe) {
        @Override
        public String toString() {
            return planet + (SURFACE.equals(zone) ? "" : "/" + zone) + " mit " + (suit == null ? "nichts" : suit) + modules;
        }
    }

    private static Case ok(String planet, @Nullable String zone, String suit, String... modules) {
        return new Case(planet, zone, suit, List.of(modules), true);
    }

    private static Case deadly(String planet, @Nullable String zone, @Nullable String suit, String... modules) {
        return new Case(planet, zone, suit, List.of(modules), false);
    }

    /** Tabelle 13.2 für alle 20 Ziele (12 Oberflächen, 8 Orbits) und ihre besonderen Zonen. */
    static List<Case> cases() {
        List<Case> c = new ArrayList<>();
        for (String p : List.of("moon", "mars", "asteroid_belt", "ceres", "titan", "earth_orbit", "venus_orbit", "mars_orbit",
                "mercury_orbit", "saturn_orbit", "uranus_orbit", "neptune_orbit")) {
            c.add(ok(p, SURFACE, T1));
            c.add(deadly(p, SURFACE, null));
        }
        c.add(ok("moon", "polar_crater", T2));
        c.add(deadly("moon", "polar_crater", T1));
        c.add(ok("venus", SURFACE, T2));
        c.add(deadly("venus", SURFACE, T1));
        c.add(ok("venus", "cloud_layer", T1));
        c.add(deadly("venus", "cloud_layer", null));
        for (String z : List.of("twilight_belt", "frost_plains", "shadow_crater")) {
            c.add(ok("mercury", z, T1, SHIELD));
            c.add(ok("mercury", z, T3));
            c.add(deadly("mercury", z, T1));
        }
        c.add(ok("mercury", "scorched_plains", T2, SHIELD));
        c.add(ok("mercury", "scorched_plains", T3));
        c.add(deadly("mercury", "scorched_plains", T1, SHIELD));
        c.add(ok("io", SURFACE, T1, SHIELD));
        c.add(deadly("io", SURFACE, T1));
        c.add(ok("io", "near_lava", T2, SHIELD));
        c.add(deadly("io", "near_lava", T1, SHIELD));
        c.add(ok("europa", SURFACE, T1, SHIELD));
        c.add(ok("europa", SURFACE, T3));
        c.add(deadly("europa", SURFACE, T1));
        c.add(ok("europa", "deep_sea", T1, SHIELD, DIVING));
        c.add(deadly("europa", "deep_sea", T1, SHIELD));
        c.add(ok("enceladus", SURFACE, T2));
        c.add(deadly("enceladus", SURFACE, T1));
        c.add(ok("triton", SURFACE, T3, HEATING));
        c.add(deadly("triton", SURFACE, T3));
        c.add(ok("pluto", SURFACE, T2, HEATING));
        c.add(deadly("pluto", SURFACE, T2));
        c.add(ok("jupiter_orbit", SURFACE, T1, SHIELD));
        c.add(deadly("jupiter_orbit", SURFACE, T1));
        return c;
    }

    /** Legt den Anzug mit vollem O₂-Vorrat und die Module an (oder zieht alles aus). */
    static void equip(ServerPlayer p, @Nullable String suit, List<String> modules) {
        Map<EquipmentSlot, List<Identifier>> installed = new EnumMap<>(EquipmentSlot.class);
        for (String m : modules) {
            String piece = BalanceManager.get().entry("suits", InfiniteHorizon.id(m).toString()).orElseThrow()
                    .getAsJsonArray("piece").get(0).getAsString();
            for (int k = 0; k < PIECES.length; k++) {
                if (PIECES[k].equals(piece)) {
                    installed.computeIfAbsent(SLOTS[k], s -> new ArrayList<>()).add(InfiniteHorizon.id(m));
                }
            }
        }
        for (int k = 0; k < PIECES.length; k++) {
            ItemStack stack = suit == null ? ItemStack.EMPTY
                    : new ItemStack(BuiltInRegistries.ITEM.getValue(InfiniteHorizon.id(suit + "_" + PIECES[k])));
            if (!stack.isEmpty() && installed.containsKey(SLOTS[k])) {
                stack.set(ModDataComponents.SUIT_MODULES.get(), List.copyOf(installed.get(SLOTS[k])));
            }
            if (!stack.isEmpty() && SLOTS[k] == EquipmentSlot.CHEST) {
                stack.set(ModDataComponents.OXYGEN.get(), Suits.supply().capacity());
            }
            p.setItemSlot(SLOTS[k], stack);
        }
    }

    /** Abnahme M7: alle Ziele mit und ohne vorgesehene Ausrüstung. */
    public static void allTargets(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        List<String> wrong = new ArrayList<>();
        try {
            for (Case c : cases()) {
                EnvironmentOverrides.Entry o = EnvironmentOverrides.set(h, InfiniteHorizon.id(c.planet()), c.zone(), null, 20);
                try {
                    equip(p, c.suit(), c.modules());
                    Set<Hazard> hazards = HazardTracker.evaluate(p, HazardTracker.rules()).hazards();
                    if (c.safe() != hazards.isEmpty()) {
                        wrong.add(c + (c.safe() ? " gefährlich: " + hazards : " sicher"));
                    }
                } finally {
                    EnvironmentOverrides.remove(o);
                }
            }
        } finally {
            IhTestPlayers.remove(p);
        }
        h.assertTrue(wrong.isEmpty(), String.join("; ", wrong));
        h.succeed();
    }
}
