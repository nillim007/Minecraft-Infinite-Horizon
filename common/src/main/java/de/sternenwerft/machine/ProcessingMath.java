package de.sternenwerft.machine;

import de.sternenwerft.data.BalanceManager.MachineStats;
import de.sternenwerft.data.BalanceManager.UpgradeStats;

/**
 * Reine Funktion (Kapitel 3A.1): wirksame Vorgangsdauer und wirksamer Verbrauch einer Maschine mit Upgrades (Kapitel 4.2).
 */
public final class ProcessingMath {

    private ProcessingMath() {
    }

    public record Effective(int ticks, long energyPerTick) {
        public long totalEnergy() {
            return (long) ticks * energyPerTick;
        }
    }

    /**
     * @param speedCount      Anzahl Geschwindigkeits-Upgrades (wird auf maxStack begrenzt)
     * @param efficiencyCount Anzahl Effizienz-Upgrades (wird auf maxStack begrenzt)
     */
    public static Effective effective(MachineStats machine, UpgradeStats speed, int speedCount,
                                      UpgradeStats efficiency, int efficiencyCount) {
        int s = Math.max(0, Math.min(speedCount, speed.maxStack()));
        int e = Math.max(0, Math.min(efficiencyCount, efficiency.maxStack()));
        double ticks = machine.ticks() * Math.pow(speed.ticksMultiplier(), s);
        double energy = machine.energyPerTick()
                * Math.pow(speed.energyMultiplier(), s)
                * Math.pow(efficiency.energyMultiplier(), e);
        return new Effective(Math.max(1, (int) Math.ceil(ticks - 1e-9)), Math.max(0, Math.round(energy)));
    }
}
