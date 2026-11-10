package de.sternenwerft.data;

import de.sternenwerft.machine.ProcessingMath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Prüft den Balance-Lader direkt gegen spec/data (gleiche Zahlen wie die Spezifikation, Kapitel 3A.8). */
class BalanceManagerTest {

    private static BalanceManager balance;

    @BeforeAll
    static void load() throws Exception {
        Path dir = Path.of(System.getProperty("sternenwerft.specData")).resolve("balance");
        balance = BalanceManager.loadDirectory(dir);
    }

    @Test
    void Balance_AllFilesLoaded() {
        assertFalse(balance.isEmpty());
        assertFalse(balance.entries("machines").isEmpty());
        assertFalse(balance.entries("generators").isEmpty());
        assertFalse(balance.entries("transport").isEmpty());
    }

    @Test
    void Crusher_Stats_MatchSpec() {
        var crusher = balance.machine("sternenwerft:crusher");
        assertEquals(20, crusher.energyPerTick());
        assertEquals(200, crusher.ticks());
        assertEquals(40_000, crusher.buffer()); // 20 × 20 × min(200, 100)
    }

    @Test
    void CombustionGenerator_Stats_MatchSpec() {
        var gen = balance.generator("sternenwerft:combustion_generator");
        assertEquals(20, gen.outputPerTick());
        assertEquals(20_000, gen.buffer());
    }

    @Test
    void CopperCable_Throughput_MatchSpec() {
        assertEquals(256, balance.cableThroughput("sternenwerft:copper_cable"));
    }

    @Test
    void BufferFormula_ClampsToMinAndMax() {
        var f = balance.bufferFormula();
        assertEquals(10_000, f.apply(1, 10));
        assertEquals(1_000_000, f.apply(2_000, 24_000));
        assertEquals(f.factor() * 50 * f.defaultDuration(), f.apply(50, null));
    }

    @Test
    void Upgrades_ChangeDurationAndEnergy() {
        var crusher = balance.machine("sternenwerft:crusher");
        var speed = balance.upgrade("sternenwerft:speed_upgrade");
        var eff = balance.upgrade("sternenwerft:efficiency_upgrade");
        var none = ProcessingMath.effective(crusher, speed, 0, eff, 0);
        assertEquals(200, none.ticks());
        assertEquals(20, none.energyPerTick());
        var oneSpeed = ProcessingMath.effective(crusher, speed, 1, eff, 0);
        assertEquals(150, oneSpeed.ticks());
        assertEquals(30, oneSpeed.energyPerTick());
        var capped = ProcessingMath.effective(crusher, speed, 99, eff, 0);
        assertEquals(64, capped.ticks()); // 200 × 0,75^4 = 63,3 → 64
    }
}
