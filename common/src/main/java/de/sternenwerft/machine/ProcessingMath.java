package de.sternenwerft.machine;

import de.sternenwerft.data.BalanceManager.MachineStats;
import de.sternenwerft.data.BalanceManager.UpgradeStats;

import java.util.LinkedHashMap;
import java.util.Map;

/** Wirksame Dauer und Verbrauch einer Maschine mit Upgrades (4.2); rechnet über {@link UpgradeMath}. */
public final class ProcessingMath {

    private ProcessingMath() {
    }

    public record Effective(int ticks, long energyPerTick) {
        public long totalEnergy() {
            return (long) ticks * energyPerTick;
        }
    }

    public static Effective effective(MachineStats machine, Map<UpgradeStats, Integer> upgrades) {
        UpgradeMath.Result r = UpgradeMath.apply(machine.ticks(), machine.energyPerTick(), upgrades);
        return new Effective(r.ticks(), r.energyPerTick());
    }

    public static Effective effective(MachineStats machine, UpgradeStats speed, int speedCount,
                                      UpgradeStats efficiency, int efficiencyCount) {
        Map<UpgradeStats, Integer> map = new LinkedHashMap<>();
        map.put(speed, speedCount);
        map.put(efficiency, efficiencyCount);
        return effective(machine, map);
    }
}
