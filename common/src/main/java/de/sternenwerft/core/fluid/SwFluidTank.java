package de.sternenwerft.core.fluid;

import de.sternenwerft.core.transaction.Transaction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Tank mit genau einem Fluid (4.1): wechselt das Fluid erst, wenn er leer ist. Änderungen laufen über
 * {@link Transaction}; {@code filter} entscheidet, welche Fluide von außen hinein dürfen.
 */
public class SwFluidTank implements Transaction.Participant<SwFluidStack> {

    private long capacity;
    private Predicate<Identifier> filter;
    private final Runnable onChange;
    private SwFluidStack stack = SwFluidStack.EMPTY;

    public SwFluidTank(long capacity, Predicate<Identifier> filter, Runnable onChange) {
        this.capacity = Math.max(0, capacity);
        this.filter = filter == null ? id -> true : filter;
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    public SwFluidStack stack() {
        return stack;
    }

    public long capacity() {
        return capacity;
    }

    public long space() {
        return capacity - stack.amount();
    }

    public void reconfigure(long capacity, Predicate<Identifier> filter) {
        this.capacity = Math.max(0, capacity);
        this.filter = filter == null ? id -> true : filter;
        if (stack.amount() > this.capacity) {
            stack = stack.withAmount(this.capacity);
        }
    }

    /** Darf dieses Fluid (jetzt) hinein? */
    public boolean accepts(@Nullable Identifier fluid) {
        return fluid != null && filter.test(fluid) && (stack.isEmpty() || stack.is(fluid));
    }

    /** Nimmt bis zu {@code offer.amount()} mB auf; prüft Filter und Ein-Fluid-Regel. */
    public long insert(SwFluidStack offer, Transaction tx) {
        if (offer.isEmpty() || !accepts(offer.fluid())) {
            return 0;
        }
        return fill(offer, tx);
    }

    /** Füllt ohne Filter (für die Maschine selbst, z. B. Pumpe oder Rezept-Ausgabe). */
    public long fill(SwFluidStack offer, Transaction tx) {
        if (offer.isEmpty() || (!stack.isEmpty() && !stack.is(offer.fluid()))) {
            return 0;
        }
        long accepted = Math.min(offer.amount(), space());
        if (accepted <= 0) {
            return 0;
        }
        tx.enlist(this);
        stack = SwFluidStack.of(offer.fluid(), stack.amount() + accepted);
        return accepted;
    }

    /** Entnimmt bis zu {@code max} mB; {@code fluid} null = beliebiges (das enthaltene) Fluid. */
    public SwFluidStack extract(@Nullable Identifier fluid, long max, Transaction tx) {
        if (stack.isEmpty() || max <= 0 || (fluid != null && !stack.is(fluid))) {
            return SwFluidStack.EMPTY;
        }
        long taken = Math.min(max, stack.amount());
        tx.enlist(this);
        SwFluidStack out = stack.withAmount(taken);
        stack = stack.withAmount(stack.amount() - taken);
        return out;
    }

    public void setStackUnchecked(SwFluidStack s) {
        stack = s == null ? SwFluidStack.EMPTY : s.withAmount(Math.min(s.amount(), capacity));
    }

    @Override
    public SwFluidStack createSnapshot() {
        return stack;
    }

    @Override
    public void restoreSnapshot(SwFluidStack snapshot) {
        stack = snapshot;
    }

    @Override
    public void onCommit() {
        onChange.run();
    }
}
