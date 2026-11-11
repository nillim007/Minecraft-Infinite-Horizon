package de.sternenwerft.machine;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.block.CableBlock;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.grid.EnergyGrid;
import de.sternenwerft.core.grid.GridManager;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.BalanceManager.UpgradeStats;
import de.sternenwerft.menu.MachineMenu;
import de.sternenwerft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Basis aller Maschinen und Generatoren (3A.3). Werte kommen per Block-ID aus {@link BalanceManager};
 * Slots und Puffer passen sich nach {@code /reload} an. Slot-Regeln (4.1) folgen aus {@link MachineLayout}.
 *
 * <p>Addons erben von dieser Klasse (oder von {@link ProcessingMachineBlockEntity}) und überschreiben
 * {@link #layout()}, {@link #configureEnergy(SwEnergyStorage)} und {@link #serverTick(ServerLevel)}.
 */
public abstract class MachineBlockEntity extends BlockEntity implements WorldlyContainer, EnergyNode, MenuProvider {

    /** Item-Tag der Pressformen (von gen_resources erzeugt). */
    public static final TagKey<Item> PRESS_MOLDS = TagKey.create(Registries.ITEM, Sternenwerft.id("press_molds"));
    /** Anzahl synchronisierter Werte (je 64 Bit, als 4 × 16 Bit übertragen). */
    public static final int SYNC_VALUES = 4;

    protected NonNullList<ItemStack> items = NonNullList.withSize(0, ItemStack.EMPTY);
    protected final SwEnergyStorage energy = new SwEnergyStorage(0, 0, 0, this::setChanged);
    private @Nullable Object platformEnergy;
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            long v = syncValue(index / 4);
            return (int) ((v >>> (16 * (index % 4))) & 0xFFFF);
        }

        @Override
        public void set(int index, int value) {
            // nur Server → Client
        }

        @Override
        public int getCount() {
            return SYNC_VALUES * 4;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Block-ID, unter der die Werte in den Balance-Dateien stehen (z. B. {@code sternenwerft:crusher}). */
    public String machineId() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString();
    }

    /** Slot-Aufbau laut Balance. */
    public abstract MachineLayout layout();

    /** Kapazität und Ein-/Ausgaberate laut Balance setzen ({@link SwEnergyStorage#reconfigure}). */
    protected abstract void configureEnergy(SwEnergyStorage energy);

    /** Ein Server-Tick (nur in geladenen Chunks, 3A.7). */
    public abstract void serverTick(ServerLevel level);

    /**
     * Werte für das GUI: 0 = Energie, 1 = Kapazität, 2 = Fortschritt bzw. Restbrennzeit, 3 = Gesamtdauer.
     * Unterklassen überschreiben 2 und 3.
     */
    protected long syncValue(int index) {
        return switch (index) {
            case 0 -> energy.getAmount();
            case 1 -> energy.getCapacity();
            default -> 0;
        };
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    /** Passt Slots und Puffer an die aktuellen Balance-Werte an. False, solange keine Werte geladen sind. */
    protected final boolean ensureLayout() {
        if (BalanceManager.get().isEmpty()) {
            return false;
        }
        int n = layout().total();
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

    /** Liest die Balance-Werte neu ein (Slots, Puffer); für Tests und Werkzeuge. */
    public boolean refreshLayout() {
        return ensureLayout();
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

    /** Eingebaute Upgrades mit Stückzahl (4.2). */
    public Map<UpgradeStats, Integer> installedUpgrades() {
        Map<UpgradeStats, Integer> map = new LinkedHashMap<>();
        MachineLayout l = layout();
        for (int i = l.upgradeStart(); i < l.fuelStart() && i < items.size(); i++) {
            ItemStack s = items.get(i);
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (!s.isEmpty() && BalanceManager.get().upgradeSlotOf(id) > 0) {
                map.merge(BalanceManager.get().upgrade(id), s.getCount(), Integer::sum);
            }
        }
        return map;
    }

    /** Darf {@code stack} in den Eingabeslot? Standard: ja. */
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return true;
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

    // ------------------------------------------------------------------ GUI

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ensureLayout();
        return new MachineMenu(containerId, inventory, this);
    }

    /** Daten, die der Client zum Aufbau des GUI braucht. */
    public MachineMenu.OpenData openData() {
        ensureLayout();
        int max = 0;
        for (int i = 0; i < layout().upgrades(); i++) {
            max = Math.max(max, upgradeMaxForSlot(i));
        }
        return new MachineMenu.OpenData(worldPosition, layout(), max);
    }

    /** Größte Stückzahl im Upgrade-Slot {@code i} (0-basiert), aus machines.json. */
    public int upgradeMaxForSlot(int i) {
        BalanceManager b = BalanceManager.get();
        int best = 0;
        for (String key : b.entries("machines").map(e -> e.keySet()).orElse(java.util.Set.of())) {
            if (b.upgradeSlotOf(key) == i + 1) {
                best = Math.max(best, b.upgrade(key).maxStack());
            }
        }
        return best;
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        MachineLayout l = layout();
        return switch (l.role(slot)) {
            case INPUT -> acceptsInput(slot, stack);
            case MOLD -> stack.is(PRESS_MOLDS);
            case UPGRADE -> BalanceManager.get().upgradeSlotOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    == slot - l.upgradeStart() + 1;
            case FUEL -> FuelHelper.isFuel(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        MachineLayout l = layout();
        int[] slots = new int[l.total() - l.upgrades()];
        int k = 0;
        for (int i = 0; i < l.total(); i++) {
            if (l.role(i) != MachineLayout.Role.UPGRADE) {
                slots[k++] = i;
            }
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return layout().role(slot) != MachineLayout.Role.UPGRADE && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return layout().role(slot) == MachineLayout.Role.OUTPUT;
    }

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
}
