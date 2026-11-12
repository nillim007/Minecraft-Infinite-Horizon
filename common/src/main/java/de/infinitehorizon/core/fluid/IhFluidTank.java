package de.infinitehorizon.core.fluid;

import de.infinitehorizon.core.transaction.Transaction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Tank mit genau einem Fluid (4.1): wechselt das Fluid erst, wenn er leer ist. Änderungen laufen über
 * {@link Transaction}; {@code filter} entscheidet, welche Fluide von außen hinein dürfen.
 */
public class IhFluidTank implements Transaction.Participant<IhFluidStack> {

    private long capacity;
    private Predicate<Identifier> filter;
    private final Runnable onChange;
    private IhFluidStack stack = IhFluidStack.EMPTY;

    public IhFluidTank(long capacity, Predicate<Identifier> filter, Runnable onChange) {
        this.capacity = Math.max(0, capacity);
        this.filter = filter == null ? id -> true : filter;
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    public IhFluidStack stack() {
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
    public long insert(IhFluidStack offer, Transaction tx) {
        if (offer.isEmpty() || !accepts(offer.fluid())) {
            return 0;
        }
        return fill(offer, tx);
    }

    /** Füllt ohne Filter (für die Maschine selbst, z. B. Pumpe oder Rezept-Ausgabe). */
    public long fill(IhFluidStack offer, Transaction tx) {
        if (offer.isEmpty() || (!stack.isEmpty() && !stack.is(offer.fluid()))) {
            return 0;
        }
        long accepted = Math.min(offer.amount(), space());
        if (accepted <= 0) {
            return 0;
        }
        tx.enlist(this);
        stack = IhFluidStack.of(offer.fluid(), stack.amount() + accepted);
        return accepted;
    }

    /** Entnimmt bis zu {@code max} mB; {@code fluid} null = beliebiges (das enthaltene) Fluid. */
    public IhFluidStack extract(@Nullable Identifier fluid, long max, Transaction tx) {
        if (stack.isEmpty() || max <= 0 || (fluid != null && !stack.is(fluid))) {
            return IhFluidStack.EMPTY;
        }
        long taken = Math.min(max, stack.amount());
        tx.enlist(this);
        IhFluidStack out = stack.withAmount(taken);
        stack = stack.withAmount(stack.amount() - taken);
        return out;
    }

    public void setStackUnchecked(IhFluidStack s) {
        stack = s == null ? IhFluidStack.EMPTY : s.withAmount(Math.min(s.amount(), capacity));
    }

    @Override
    public IhFluidStack createSnapshot() {
        return stack;
    }

    @Override
    public void restoreSnapshot(IhFluidStack snapshot) {
        stack = snapshot;
    }

    @Override
    public void onCommit() {
        onChange.run();
    }
}
