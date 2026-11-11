package de.sternenwerft.machine;

import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.BalanceManager.MachineStats;
import de.sternenwerft.data.BalanceManager.UpgradeStats;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
 * Verarbeitungsmaschine mit einem Eingang (z. B. Zerkleinerer). Slots laut machines.json:
 * Eingänge, dann Ausgänge, dann Upgrades. Verbrauch und Dauer: Rezept, sonst Maschine (25.4), dann Upgrades (4.2).
 *
 * <p>Addon-Maschine: eigene {@link ProcessingRecipe.Kind}, ein BE-Typ mit
 * {@code (pos, state) -> new ProcessingMachineBlockEntity(TYP, pos, state, () -> KIND)}, ein {@link MachineBlock}
 * und ein Eintrag in {@code data/<ns>/sternenwerft/balance/machines.json}.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity {

    /** Neutrale Upgrade-Werte, solange keine Upgrade-Items existieren. */
    private static final UpgradeStats NO_UPGRADE = new UpgradeStats("none", 1.0, 1.0, 0);

    private final Supplier<ProcessingRecipe.Kind> kind;
    private int progress;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        Supplier<ProcessingRecipe.Kind> kind) {
        super(type, pos, state);
        this.kind = kind;
    }

    public MachineStats stats() {
        return BalanceManager.get().machine(machineId());
    }

    public int progress() {
        return progress;
    }

    @Override
    protected int slotCount() {
        MachineStats s = stats();
        return s.inputSlots() + s.outputSlots() + s.upgradeSlots();
    }

    @Override
    protected void configureEnergy(SwEnergyStorage storage) {
        MachineStats s = stats();
        storage.reconfigure(s.buffer(), s.buffer(), 0);
    }

    private boolean isInput(int slot) {
        return slot >= 0 && slot < stats().inputSlots();
    }

    private boolean isOutput(int slot) {
        MachineStats s = stats();
        return slot >= s.inputSlots() && slot < s.inputSlots() + s.outputSlots();
    }

    // SPEC-GAP: Upgrade-Items kommen erst in M2; Upgrade-Slots nehmen bis dahin nichts an.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isInput(slot);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        MachineStats s = stats();
        int[] slots = new int[s.inputSlots() + s.outputSlots()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutput(slot);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    public Optional<RecipeHolder<ProcessingRecipe>> findRecipe(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(kind.get().type(), new SingleRecipeInput(input), level);
    }

    /** Wirksame Dauer und Verbrauch für ein Rezept (reine Rechnung, 4.2). */
    public ProcessingMath.Effective effective(ProcessingRecipe recipe) {
        MachineStats s = stats();
        MachineStats withRecipe = new MachineStats(s.id(), recipe.energyPerTick().orElse(s.energyPerTick()),
                recipe.ticks().orElse(s.ticks()), s.buffer(), s.inputSlots(), s.outputSlots(), s.upgradeSlots());
        return ProcessingMath.effective(withRecipe, NO_UPGRADE, 0, NO_UPGRADE, 0);
    }

    /** Dauer in Ticks für dieses Eingangs-Item; -1 ohne passendes Rezept. */
    public int durationFor(ServerLevel level, ItemStack input) {
        return findRecipe(level, input).map(h -> effective(h.value()).ticks()).orElse(-1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        ItemStack input = items.get(0);
        Optional<RecipeHolder<ProcessingRecipe>> found = findRecipe(level, input);
        if (found.isEmpty()) {
            progress = 0;
            setStatus(MachineStatus.IDLE);
            return;
        }
        ProcessingRecipe recipe = found.get().value();
        if (!outputsFit(recipe.results())) {
            setStatus(MachineStatus.ERROR);
            return;
        }
        ProcessingMath.Effective eff = effective(recipe);
        try (Transaction tx = Transaction.open()) {
            if (!energy.consumeInternal(eff.energyPerTick(), tx)) {
                setStatus(MachineStatus.IDLE);
                return;
            }
            tx.commit();
        }
        progress++;
        if (progress >= eff.ticks()) {
            progress = 0;
            input.shrink(recipe.inputCount());
            for (ProcessingRecipe.ChanceResult r : recipe.results()) {
                if (r.chance() >= 1.0f || level.getRandom().nextFloat() < r.chance()) {
                    insertOutput(r.toStack(), false);
                }
            }
        }
        setStatus(MachineStatus.RUNNING);
        setChanged();
    }

    private boolean outputsFit(List<ProcessingRecipe.ChanceResult> results) {
        MachineStats s = stats();
        List<ItemStack> copy = new ArrayList<>();
        for (int i = s.inputSlots(); i < s.inputSlots() + s.outputSlots(); i++) {
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

    private void insertOutput(ItemStack stack, boolean simulate) {
        MachineStats s = stats();
        ItemStack rest = stack;
        for (int i = s.inputSlots(); i < s.inputSlots() + s.outputSlots() && !rest.isEmpty(); i++) {
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
    public int countOutput(net.minecraft.world.item.Item item) {
        MachineStats s = stats();
        int n = 0;
        for (int i = s.inputSlots(); i < s.inputSlots() + s.outputSlots() && i < items.size(); i++) {
            if (items.get(i).is(item)) {
                n += items.get(i).getCount();
            }
        }
        return n;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
    }
}
