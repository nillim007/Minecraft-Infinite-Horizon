package de.sternenwerft.core.energy;

import de.sternenwerft.core.transaction.Transaction;

/**
 * Energiespeicher in J (1 J = 1 FE), Kapitel 3.3. Alle Änderungen laufen über eine {@link Transaction}.
 */
public class SwEnergyStorage implements Transaction.Participant<Long> {

    private long capacity;
    private long maxInsert;
    private long maxExtract;
    private long amount;
    private final Runnable onChange;

    public SwEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onChange) {
        this.capacity = Math.max(0, capacity);
        this.maxInsert = Math.max(0, maxInsert);
        this.maxExtract = Math.max(0, maxExtract);
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    /** Fügt höchstens {@code maxAmount} J ein und gibt die tatsächlich eingefügte Menge zurück. */
    public long insert(long maxAmount, Transaction tx) {
        long accepted = Math.min(Math.min(maxAmount, maxInsert), capacity - amount);
        if (accepted <= 0) {
            return 0;
        }
        tx.enlist(this);
        amount += accepted;
        return accepted;
    }

    /** Entnimmt höchstens {@code maxAmount} J und gibt die tatsächlich entnommene Menge zurück. */
    public long extract(long maxAmount, Transaction tx) {
        long taken = Math.min(Math.min(maxAmount, maxExtract), amount);
        if (taken <= 0) {
            return 0;
        }
        tx.enlist(this);
        amount -= taken;
        return taken;
    }

    /** Interner Verbrauch durch die eigene Maschine, ohne die Entnahmegrenze nach außen. */
    public boolean consumeInternal(long needed, Transaction tx) {
        if (needed <= 0) {
            return true;
        }
        if (amount < needed) {
            return false;
        }
        tx.enlist(this);
        amount -= needed;
        return true;
    }

    /** Interne Erzeugung durch die eigene Maschine (Generator), ohne die Einfügegrenze nach außen. */
    public long produceInternal(long produced, Transaction tx) {
        long accepted = Math.min(produced, capacity - amount);
        if (accepted <= 0) {
            return 0;
        }
        tx.enlist(this);
        amount += accepted;
        return accepted;
    }

    public long getAmount() {
        return amount;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getMaxInsert() {
        return maxInsert;
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    /** Übernimmt neue Grenzwerte (z. B. nach /reload); überschüssige Energie wird gekappt. */
    public void reconfigure(long capacity, long maxInsert, long maxExtract) {
        this.capacity = Math.max(0, capacity);
        this.maxInsert = Math.max(0, maxInsert);
        this.maxExtract = Math.max(0, maxExtract);
        this.amount = Math.min(amount, this.capacity);
    }

    /** Nur zum Laden gespeicherter Daten. */
    public void setAmountUnchecked(long amount) {
        this.amount = Math.max(0, Math.min(amount, capacity));
    }

    @Override
    public Long createSnapshot() {
        return amount;
    }

    @Override
    public void restoreSnapshot(Long snapshot) {
        amount = snapshot;
    }

    @Override
    public void onCommit() {
        onChange.run();
    }
}
