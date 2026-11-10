package de.sternenwerft.core;

import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {

    @Test
    void Transaction_CloseWithoutCommit_RestoresState() {
        SwEnergyStorage storage = new SwEnergyStorage(1000, 100, 100, null);
        try (Transaction tx = Transaction.open()) {
            assertEquals(100, storage.insert(500, tx));
            assertEquals(100, storage.getAmount());
        }
        assertEquals(0, storage.getAmount());
    }

    @Test
    void Transaction_Commit_KeepsStateAndNotifiesOnce() {
        AtomicInteger changes = new AtomicInteger();
        SwEnergyStorage storage = new SwEnergyStorage(1000, 100, 100, changes::incrementAndGet);
        try (Transaction tx = Transaction.open()) {
            storage.insert(60, tx);
            storage.insert(60, tx);
            tx.commit();
        }
        assertEquals(120, storage.getAmount());
        assertEquals(1, changes.get());
    }

    @Test
    void EnergyStorage_RespectsCapacityAndLimits() {
        SwEnergyStorage storage = new SwEnergyStorage(150, 100, 30, null);
        try (Transaction tx = Transaction.open()) {
            assertEquals(100, storage.insert(1000, tx));
            assertEquals(50, storage.insert(1000, tx));
            assertEquals(30, storage.extract(1000, tx));
            assertFalse(storage.consumeInternal(500, tx));
            tx.commit();
        }
        assertEquals(120, storage.getAmount());
    }

    @Test
    void Transaction_UseAfterCommit_Throws() {
        SwEnergyStorage storage = new SwEnergyStorage(100, 100, 100, null);
        Transaction tx = Transaction.open();
        tx.commit();
        assertThrows(IllegalStateException.class, () -> storage.insert(1, tx));
    }
}
