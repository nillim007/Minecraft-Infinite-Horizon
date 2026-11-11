package de.sternenwerft.machine;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.block.CableBlock;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.grid.EnergyGrid;
import de.sternenwerft.core.grid.GridManager;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Basis aller Maschinen und Generatoren (3A.3). Werte kommen per Block-ID aus {@link BalanceManager};
 * Slotanzahl und Puffer passen sich nach {@code /reload} an.
 *
 * <p>Addons erben von dieser Klasse (oder von {@link ProcessingMachineBlockEntity}) und überschreiben
 * {@link #slotCount()}, {@link #configureEnergy(SwEnergyStorage)} und {@link #serverTick(ServerLevel)}.
 */
public abstract class MachineBlockEntity extends BlockEntity implements WorldlyContainer, EnergyNode {

    protected NonNullList<ItemStack> items = NonNullList.withSize(0, ItemStack.EMPTY);
    protected final SwEnergyStorage energy = new SwEnergyStorage(0, 0, 0, this::setChanged);
    private @Nullable Object platformEnergy;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Block-ID, unter der die Werte in den Balance-Dateien stehen (z. B. {@code sternenwerft:crusher}). */
    public String machineId() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString();
    }

    /** Anzahl Item-Slots laut Balance. */
    protected abstract int slotCount();

    /** Kapazität und Ein-/Ausgaberate laut Balance setzen ({@link SwEnergyStorage#reconfigure}). */
    protected abstract void configureEnergy(SwEnergyStorage energy);

    /** Ein Server-Tick (nur in geladenen Chunks, 3A.7). */
    public abstract void serverTick(ServerLevel level);

    /** Passt Slots und Puffer an die aktuellen Balance-Werte an. False, solange keine Werte geladen sind. */
    protected final boolean ensureLayout() {
        if (BalanceManager.get().isEmpty()) {
            return false;
        }
        int n = slotCount();
        if (items.size() != n) {
            NonNullList<ItemStack> resized = NonNullList.withSize(n, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(n, items.size()); i++) {
                resized.set(i, items.get(i));
            }
            items = resized;
        }
        configureEnergy(energy);
        return true;
    }

    public MachineStatus status() {
        BlockState s = getBlockState();
        return s.hasProperty(MachineBlock.STATUS) ? s.getValue(MachineBlock.STATUS) : MachineStatus.IDLE;
    }

    protected void setStatus(MachineStatus status) {
        BlockState s = getBlockState();
        if (level != null && s.hasProperty(MachineBlock.STATUS) && s.getValue(MachineBlock.STATUS) != status) {
            level.setBlock(worldPosition, s.setValue(MachineBlock.STATUS, status), Block.UPDATE_ALL);
        }
    }

    /** Schiebt Energie in angrenzende Netze und Empfänger (Push-Prinzip 4.4). */
    protected void pushEnergy(ServerLevel level) {
        for (Direction d : Direction.values()) {
            if (energy.getAmount() <= 0) {
                return;
            }
            if (!providesEnergyTo(d)) {
                continue;
            }
            BlockPos n = worldPosition.relative(d);
            long offer = Math.min(energy.getAmount(), energy.getMaxExtract());
            long moved;
            if (level.getBlockState(n).getBlock() instanceof CableBlock) {
                EnergyGrid grid = GridManager.get(level).gridAt(level, n);
                moved = grid == null ? 0 : grid.push(level, worldPosition, offer);
            } else {
                BlockEntity be = level.getBlockEntity(n);
                EnergyTarget target = be instanceof EnergyNode node
                        ? (node.acceptsEnergyFrom(d.getOpposite()) ? node.asTarget() : null)
                        : Services.ENERGY.findTarget(level, n, d.getOpposite());
                moved = target == null ? 0 : target.insert(offer, false);
            }
            if (moved > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.extract(moved, tx);
                    tx.commit();
                }
            }
        }
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("energy", energy.getAmount());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ensureLayout();
        ContainerHelper.loadAllItems(input, items);
        energy.setAmountUnchecked(input.getLongOr("energy", 0L));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ------------------------------------------------------------------ Energie (EnergyNode)

    @Override
    public SwEnergyStorage energyStorage() {
        return energy;
    }

    @Override
    public void onEnergyChanged() {
        setChanged();
    }

    @Override
    public Object platformEnergyHandle(Function<EnergyNode, Object> factory) {
        if (platformEnergy == null) {
            platformEnergy = factory.apply(this);
        }
        return platformEnergy;
    }

    // ------------------------------------------------------------------ Inventar (Trichter, 4.1)

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) {
            setChanged();
        }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ensureLayout();
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }
}
