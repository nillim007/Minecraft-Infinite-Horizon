package de.sternenwerft.machine;

import de.sternenwerft.data.BalanceManager.UpgradeStats;

import java.util.Map;

/**
 * Reine Upgrade-Rechnung (4.2), in {@code double}: Dauer = max(1, round(ticks × Π t_i^n_i)),
 * Verbrauch = max(1, round(J/t × Π e_i^n_i)). Die Stückzahl je Upgrade ist auf dessen {@code max_stack} begrenzt.
 * Funktioniert für beliebig viele Upgrade-Arten (auch aus Addons).
 */
public final class UpgradeMath {

    private UpgradeMath() {
    }

    public record Result(int ticks, long energyPerTick) {
    }

    public static Result apply(int baseTicks, long baseEnergyPerTick, Map<UpgradeStats, Integer> upgrades) {
        double ticks = baseTicks;
        double energy = baseEnergyPerTick;
        for (Map.Entry<UpgradeStats, Integer> e : upgrades.entrySet()) {
            UpgradeStats u = e.getKey();
            int n = Math.max(0, Math.min(e.getValue(), u.maxStack()));
            ticks *= Math.pow(u.ticksMultiplier(), n);
            energy *= Math.pow(u.energyMultiplier(), n);
        }
        return new Result((int) Math.max(1, Math.round(ticks)), Math.max(1, Math.round(energy)));
    }
}
