package de.sternenwerft.machine;

import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.BalanceManager.MachineStats;
import de.sternenwerft.recipe.MachineRecipeInput;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Verarbeitungsmaschine (Zerkleinerer, Presse, …). Slots laut machines.json; Rezeptarten als {@link ProcessingRecipe.Kind}.
 * Dauer und Verbrauch: Rezept, sonst Maschine (25.4), dann Upgrades (4.2). Der Fortschritt ist ein Anteil (0–1),
 * damit er beim Ein- und Ausbau von Upgrades erhalten bleibt.
 *
 * <p>Addon-Maschine: {@code (pos, state) -> new ProcessingMachineBlockEntity(TYP, pos, state, () -> List.of(KIND))},
 * ein {@link MachineBlock} und ein Eintrag in {@code data/<ns>/sternenwerft/balance/machines.json}.
 * Für andere Rezeptquellen {@link #findPlan(ServerLevel)} überschreiben.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity {

    /** Ein konkreter Vorgang: welche Slots wie viel verbrauchen, was herauskommt, Rezept-Überschreibungen. */
    public record Plan(int[] consumeSlots, int[] consumeCounts, List<ProcessingRecipe.ChanceResult> results,
                       Optional<Integer> ticks, Optional<Long> energyPerTick) {
    }

    private static final double DONE = 1.0 - 1e-9;

    private final Supplier<List<ProcessingRecipe.Kind>> kinds;
    private double progress;
    private int currentTicks;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        Supplier<List<ProcessingRecipe.Kind>> kinds) {
        super(type, pos, state);
        this.kinds = kinds;
    }

    public MachineStats stats() {
        return BalanceManager.get().machine(machineId());
    }

    /** Fortschritt des laufenden Vorgangs als Anteil 0–1. */
    public double progress() {
        return progress;
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        MachineStats s = stats();
        return new MachineLayout(s.inputSlots(), s.moldSlots(), s.outputSlots(), s.upgradeSlots(), 0);
    }

    @Override
    protected void configureEnergy(SwEnergyStorage storage) {
        MachineStats s = stats();
        storage.reconfigure(s.buffer(), s.buffer(), 0);
    }

    @Override
    protected long syncValue(int index) {
        return switch (index) {
            case 2 -> Math.round(progress * currentTicks);
            case 3 -> currentTicks;
            default -> super.syncValue(index);
        };
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    /** Aktuelle Eingabe: alle Eingabeslots und der Formslot. */
    public MachineRecipeInput recipeInput() {
        MachineLayout l = layout();
        List<ItemStack> in = new ArrayList<>(l.inputs());
        for (int i = 0; i < l.inputs(); i++) {
            in.add(items.get(i));
        }
        ItemStack mold = l.molds() > 0 ? items.get(l.moldStart()) : ItemStack.EMPTY;
        return new MachineRecipeInput(in, mold);
    }

    /** Sucht den nächsten Vorgang über alle Rezeptarten der Maschine. */
    protected Optional<Plan> findPlan(ServerLevel level) {
        MachineRecipeInput input = recipeInput();
        if (input.inputs().stream().allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        for (ProcessingRecipe.Kind kind : kinds.get()) {
            Optional<RecipeHolder<ProcessingRecipe>> found = level.recipeAccess().getRecipeFor(kind.type(), input, level);
            if (found.isPresent()) {
                ProcessingRecipe r = found.get().value();
                int[] slots = r.assign(input);
                if (slots != null) {
                    int[] counts = r.ingredients().stream().mapToInt(ProcessingRecipe.CountedIngredient::count).toArray();
                    return Optional.of(new Plan(slots, counts, r.results(), r.ticks(), r.energyPerTick()));
                }
            }
        }
        return Optional.empty();
    }

    /** Wirksame Dauer und Verbrauch für einen Vorgang (reine Rechnung, 4.2). */
    public ProcessingMath.Effective effective(Plan plan) {
        MachineStats s = stats();
        MachineStats withRecipe = s.withProcess(plan.energyPerTick().orElse(s.energyPerTick()), plan.ticks().orElse(s.ticks()));
        return ProcessingMath.effective(withRecipe, installedUpgrades());
    }

    /** Dauer in Ticks, wenn {@code input} im ersten Eingabeslot läge; -1 ohne passendes Rezept. */
    public int durationFor(ServerLevel level, ItemStack input) {
        ensureLayout();
        ItemStack old = items.get(0);
        items.set(0, input);
        try {
            return findPlan(level).map(p -> effective(p).ticks()).orElse(-1);
        } finally {
            items.set(0, old);
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        Optional<Plan> found = findPlan(level);
        if (found.isEmpty()) {
            progress = 0;
            currentTicks = 0;
            setStatus(MachineStatus.IDLE);
            return;
        }
        Plan plan = found.get();
        if (!outputsFit(plan.results())) {
            setStatus(MachineStatus.ERROR);
            return;
        }
        ProcessingMath.Effective eff = effective(plan);
        currentTicks = eff.ticks();
        try (Transaction tx = Transaction.open()) {
            if (!energy.consumeInternal(eff.energyPerTick(), tx)) {
                setStatus(MachineStatus.IDLE);
                return;
            }
            tx.commit();
        }
        progress += 1.0 / eff.ticks();
        if (progress >= DONE) {
            progress = 0;
            for (int k = 0; k < plan.consumeSlots().length; k++) {
                items.get(plan.consumeSlots()[k]).shrink(plan.consumeCounts()[k]);
            }
            for (ProcessingRecipe.ChanceResult r : plan.results()) {
                if (r.chance() >= 1.0f || level.getRandom().nextFloat() < r.chance()) {
                    insertOutput(r.toStack());
                }
            }
        }
        setStatus(MachineStatus.RUNNING);
        setChanged();
    }

    private boolean outputsFit(List<ProcessingRecipe.ChanceResult> results) {
        MachineLayout l = layout();
        List<ItemStack> copy = new ArrayList<>();
        for (int i = l.outputStart(); i < l.upgradeStart(); i++) {
            copy.add(items.get(i).copy());
        }
        for (ProcessingRecipe.ChanceResult r : results) {
            ItemStack rest = r.toStack();
            for (int i = 0; i < copy.size() && !rest.isEmpty(); i++) {
                rest = merge(copy, i, rest);
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void insertOutput(ItemStack stack) {
        MachineLayout l = layout();
        ItemStack rest = stack;
        for (int i = l.outputStart(); i < l.upgradeStart() && !rest.isEmpty(); i++) {
            rest = merge(items, i, rest);
        }
    }

    /** Legt {@code stack} in Slot {@code i} der Liste und gibt den Rest zurück. */
    private static ItemStack merge(List<ItemStack> list, int i, ItemStack stack) {
        ItemStack slot = list.get(i);
        if (slot.isEmpty()) {
            list.set(i, stack.copy());
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(slot, stack)) {
            return stack;
        }
        int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
        if (move <= 0) {
            return stack;
        }
        slot.grow(move);
        ItemStack rest = stack.copy();
        rest.shrink(move);
        return rest;
    }

    /** Anzahl eines Items in den Ausgabeslots (für Tests und Anzeigen). */
    public int countOutput(Item item) {
        MachineLayout l = layout();
        int n = 0;
        for (int i = l.outputStart(); i < l.upgradeStart() && i < items.size(); i++) {
            if (items.get(i).is(item)) {
                n += items.get(i).getCount();
            }
        }
        return n;
    }

    /** Nimmt bis zu {@code max} Stück eines Items aus den Ausgabeslots (für Tests und Logistik). */
    public ItemStack takeOutput(Item item, int max) {
        MachineLayout l = layout();
        for (int i = l.outputStart(); i < l.upgradeStart() && i < items.size(); i++) {
            if (items.get(i).is(item)) {
                return removeItem(i, max);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getDoubleOr("progress", 0.0);
    }
}
