package de.sternenwerft.core;

import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.core.transaction.Transaction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tankregeln (4.1): ein Fluid je Tank, Wechsel erst bei leerem Tank, Rücknahme per Transaktion. */
class FluidTankTest {

    private static final Identifier WATER = Identifier.fromNamespaceAndPath("minecraft", "water");
    private static final Identifier OIL = Identifier.fromNamespaceAndPath("sternenwerft", "crude_oil");

    @Test
    void onlyOneFluidUntilEmpty() {
        SwFluidTank tank = new SwFluidTank(1000, null, null);
        try (Transaction tx = Transaction.open()) {
            assertEquals(600, tank.insert(SwFluidStack.of(WATER, 600), tx));
            assertEquals(0, tank.insert(SwFluidStack.of(OIL, 100), tx));
            assertEquals(400, tank.insert(SwFluidStack.of(WATER, 900), tx));
            tx.commit();
        }
        assertFalse(tank.accepts(OIL));
        try (Transaction tx = Transaction.open()) {
            assertEquals(1000, tank.extract(null, 5000, tx).amount());
            tx.commit();
        }
        assertTrue(tank.accepts(OIL));
    }

    @Test
    void rollbackRestoresContent() {
        SwFluidTank tank = new SwFluidTank(1000, null, null);
        try (Transaction tx = Transaction.open()) {
            tank.insert(SwFluidStack.of(OIL, 500), tx);
        }
        assertTrue(tank.stack().isEmpty());
    }

    @Test
    void filterBlocksInsertButNotFill() {
        SwFluidTank tank = new SwFluidTank(1000, id -> false, null);
        try (Transaction tx = Transaction.open()) {
            assertEquals(0, tank.insert(SwFluidStack.of(WATER, 100), tx));
            assertEquals(100, tank.fill(SwFluidStack.of(WATER, 100), tx));
            tx.commit();
        }
        assertEquals(100, tank.stack().amount());
    }
}
