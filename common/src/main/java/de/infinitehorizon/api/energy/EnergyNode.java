package de.infinitehorizon.api.energy;

import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.transaction.Transaction;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** Eigene Block-Entity mit Energiespeicher (Maschinen, Generatoren, später Speicher). */
public interface EnergyNode {

    IhEnergyStorage energyStorage();

    /** Nimmt der Block über diese Seite Energie an? {@code null} = ohne Seitenangabe. */
    boolean acceptsEnergyFrom(@Nullable Direction side);

    /** Gibt der Block über diese Seite Energie ab? */
    boolean providesEnergyTo(@Nullable Direction side);

    /** Wird nach jeder bestätigten Änderung von außen aufgerufen (Speichern markieren). */
    void onEnergyChanged();

    /** Ablage für das Adapter-Objekt des Loaders, damit es nicht jeden Tick neu entsteht. */
    Object platformEnergyHandle(Function<EnergyNode, Object> factory);

    default long receiveEnergy(long amount, boolean simulate) {
        try (Transaction tx = Transaction.open()) {
            long accepted = energyStorage().insert(amount, tx);
            if (!simulate && accepted > 0) {
                tx.commit();
            }
            return accepted;
        }
    }

    default EnergyTarget asTarget() {
        return this::receiveEnergy;
    }
}
