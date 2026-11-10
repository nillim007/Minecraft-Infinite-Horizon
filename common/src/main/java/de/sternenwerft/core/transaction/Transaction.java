package de.sternenwerft.core.transaction;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemeinsames Transaktionsmodell (Kapitel 3.3, 3A.3): simulieren → bestätigen oder zurückrollen.
 *
 * <p>Teilnehmer melden sich vor ihrer ersten Änderung mit {@link #enlist(Participant)} an; dabei wird ihr Zustand
 * gesichert. Wird die Transaktion ohne {@link #commit()} geschlossen, stellt sie alle Zustände wieder her.
 * Die Plattform-Adapter bilden die Transaktionen von NeoForge und Fabric auf dieses Modell ab.</p>
 */
public final class Transaction implements AutoCloseable {

    /** Ein Objekt, dessen Zustand innerhalb einer Transaktion gesichert und wiederhergestellt werden kann. */
    public interface Participant<S> {
        S createSnapshot();

        void restoreSnapshot(S snapshot);

        /** Wird nach dem endgültigen Bestätigen genau einmal aufgerufen (z. B. für setChanged()). */
        default void onCommit() {
        }
    }

    private final Map<Participant<?>, Object> snapshots = new IdentityHashMap<>();
    private final List<Participant<?>> order = new ArrayList<>();
    private boolean closed;
    private boolean committed;

    private Transaction() {
    }

    public static Transaction open() {
        return new Transaction();
    }

    /** Sichert den Zustand des Teilnehmers, falls noch nicht geschehen. Vor jeder Änderung aufrufen. */
    public <S> void enlist(Participant<S> participant) {
        ensureOpen();
        if (!snapshots.containsKey(participant)) {
            snapshots.put(participant, participant.createSnapshot());
            order.add(participant);
        }
    }

    public void commit() {
        ensureOpen();
        committed = true;
        closed = true;
        for (Participant<?> p : order) {
            p.onCommit();
        }
    }

    public boolean isOpen() {
        return !closed;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void close() {
        if (closed && committed) {
            return;
        }
        closed = true;
        for (int i = order.size() - 1; i >= 0; i--) {
            Participant p = order.get(i);
            p.restoreSnapshot(snapshots.get(p));
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Transaktion ist bereits abgeschlossen");
        }
    }
}
