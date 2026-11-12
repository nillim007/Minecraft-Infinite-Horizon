package de.infinitehorizon.core.energy;

import de.infinitehorizon.core.transaction.Transaction;

/**
 * Energiespeicher in J (1 J = 1 FE), Kapitel 3.3. Alle Änderungen laufen über eine {@link Transaction}.
 *
 * <p>{@code maxInsert}/{@code maxExtract} gelten je Aufruf. Mit {@link #setPerTickLimit(boolean)} gelten sie stattdessen
 * je Tick über alle Aufrufe zusammen (Akku: „Ein/Aus 512 J/t“, 6.3); der Besitzer ruft dann zu Beginn seines Ticks
 * {@link #newTick()} auf. Loader-Adapter nutzen {@link #remainingInsert()}, {@link #applyExternal(long)} und
 * {@link #snapshot()}/{@link #restore(State)}, damit die Tick-Grenze auch für fremde Mods gilt.
 */
public class IhEnergyStorage implements Transaction.Participant<IhEnergyStorage.State> {

    /** Zustand für Transaktionen: Menge und das in diesem Tick schon Ein- bzw. Ausgegebene. */
    public record State(long amount, long tickIn, long tickOut) {
    }

    private long capacity;
    private long maxInsert;
    private long maxExtract;
    private long amount;
    private boolean perTick;
    private long tickIn;
    private long tickOut;
    private final Runnable onChange;

    public IhEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onChange) {
        this.capacity = Math.max(0, capacity);
        this.maxInsert = Math.max(0, maxInsert);
        this.maxExtract = Math.max(0, maxExtract);
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    /** Grenzen je Tick statt je Aufruf (siehe Klassenbeschreibung). */
    public void setPerTickLimit(boolean perTick) {
        this.perTick = perTick;
    }

    /** Beginn eines neuen Ticks: die Tick-Grenzen stehen wieder voll zur Verfügung. */
    public void newTick() {
        tickIn = 0;
        tickOut = 0;
    }

    /** Wie viel J dürfen jetzt noch hinein (Grenze und freier Platz)? */
    public long remainingInsert() {
        long limit = perTick ? Math.max(0, maxInsert - tickIn) : maxInsert;
        return Math.max(0, Math.min(limit, capacity - amount));
    }

    /** Wie viel J dürfen jetzt noch hinaus (Grenze und Inhalt)? */
    public long remainingExtract() {
        long limit = perTick ? Math.max(0, maxExtract - tickOut) : maxExtract;
        return Math.max(0, Math.min(limit, amount));
    }

    /** Fügt höchstens {@code maxAmount} J ein und gibt die tatsächlich eingefügte Menge zurück. */
    public long insert(long maxAmount, Transaction tx) {
        long accepted = Math.min(maxAmount, remainingInsert());
        if (accepted <= 0) {
            return 0;
        }
        tx.enlist(this);
        amount += accepted;
        tickIn += accepted;
        return accepted;
    }

    /** Entnimmt höchstens {@code maxAmount} J und gibt die tatsächlich entnommene Menge zurück. */
    public long extract(long maxAmount, Transaction tx) {
        long taken = Math.min(maxAmount, remainingExtract());
        if (taken <= 0) {
            return 0;
        }
        tx.enlist(this);
        amount -= taken;
        tickOut += taken;
        return taken;
    }

    /**
     * Änderung durch einen Loader-Adapter (dessen Transaktion sichert vorher {@link #snapshot()}): positiv = Eingang,
     * negativ = Ausgang. Der Aufrufer begrenzt vorher mit {@link #remainingInsert()} bzw. {@link #remainingExtract()}.
     */
    public void applyExternal(long delta) {
        amount = Math.max(0, Math.min(capacity, amount + delta));
        if (delta > 0) {
            tickIn += delta;
        } else {
            tickOut -= delta;
        }
    }

    public State snapshot() {
        return new State(amount, tickIn, tickOut);
    }

    public void restore(State s) {
        amount = s.amount();
        tickIn = s.tickIn();
        tickOut = s.tickOut();
    }

    /**
     * Interner Verbrauch durch die eigene Maschine ohne Transaktion (jeder Tick, Leistungstest 27.2); false, wenn zu
     * wenig da ist. Der Besitzer markiert sich danach selbst als geändert.
     */
    public boolean useInternal(long needed) {
        if (needed <= 0) {
            return true;
        }
        if (amount < needed) {
            return false;
        }
        amount -= needed;
        return true;
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

    /** Eingabegrenze laut Daten (je Aufruf bzw. je Tick); was jetzt noch geht: {@link #remainingInsert()}. */
    public long getMaxInsert() {
        return maxInsert;
    }

    /** Ausgabegrenze laut Daten (je Aufruf bzw. je Tick); was jetzt noch geht: {@link #remainingExtract()}. */
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
    public State createSnapshot() {
        return snapshot();
    }

    @Override
    public void restoreSnapshot(State snapshot) {
        restore(snapshot);
    }

    @Override
    public void onCommit() {
        onChange.run();
    }
}
