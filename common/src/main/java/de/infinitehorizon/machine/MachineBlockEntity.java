package de.infinitehorizon.machine;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.api.energy.EnergyNode;
import de.infinitehorizon.api.energy.EnergyTarget;
import de.infinitehorizon.api.fluid.FluidNode;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.block.FluidPipeBlock;
import de.infinitehorizon.core.fluid.ComponentContainers;
import de.infinitehorizon.core.fluid.FluidContainers;
import de.infinitehorizon.core.fluid.FluidIds;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.grid.FluidGrid;
import de.infinitehorizon.block.CableBlock;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.grid.EnergyGrid;
import de.infinitehorizon.core.grid.GridManager;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.UpgradeStats;
import de.infinitehorizon.menu.MachineMenu;
import de.infinitehorizon.platform.Services;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Basis aller Maschinen und Generatoren (3A.3). Werte kommen per Block-ID aus {@link BalanceManager};
 * Slots und Puffer passen sich nach {@code /reload} an. Slot-Regeln (4.1) folgen aus {@link MachineLayout}.
 *
 * <p>Addons erben von dieser Klasse (oder von {@link ProcessingMachineBlockEntity}) und überschreiben
 * {@link #layout()}, {@link #configureEnergy(IhEnergyStorage)} und {@link #serverTick(ServerLevel)}.
 */
public abstract class MachineBlockEntity extends de.infinitehorizon.block.IhBlockEntity implements WorldlyContainer, EnergyNode, FluidNode, MenuProvider {

    /** Item-Tag der Pressformen (von gen_resources erzeugt). */
    public static final TagKey<Item> PRESS_MOLDS = TagKey.create(Registries.ITEM, InfiniteHorizon.id("press_molds"));
    /** Höchstzahl Tanks, die das GUI anzeigt (Fusionsreaktor: 3 Eingänge + 1 Ausgang). */
    public static final int MAX_TANKS = 4;
    /**
     * Anzahl synchronisierter Werte (je 64 Bit, als 4 × 16 Bit übertragen): 4 Grundwerte + je Tank Fluid, Menge,
     * Kapazität + Ausgangsseite, Tankwahl, Multiblock-Zustand, fehlender Block, Status-Hinweis.
     */
    public static final int SYNC_VALUES = 4 + 3 * MAX_TANKS + 5;
    /** Index des Sync-Werts „Ausgangsseite + 1“ (0 = keine Fluidausgabe). */
    public static final int SYNC_SIDE = 4 + 3 * MAX_TANKS;
    /** Tankwahl gepackt: Bits 0–7 gewählter Tank, 8–15 Zahl der wählbaren Tanks, 16–23 Index des ersten im GUI. */
    public static final int SYNC_TANK = SYNC_SIDE + 1;
    /** Multiblock: 0 = kein Multiblock, 1 = vollständig, 2 = unvollständig. */
    public static final int SYNC_MULTIBLOCK = SYNC_SIDE + 2;
    /** Erster fehlender Block ({@code BlockPos#asLong}), nur bei {@link #SYNC_MULTIBLOCK} = 2. */
    public static final int SYNC_MISSING = SYNC_SIDE + 3;
    /** Zusatz zur Statuslampe: {@link #HINT_LOCKED}, {@link #HINT_NO_OWNER} oder 0. */
    public static final int SYNC_HINT = SYNC_SIDE + 4;
    /** Alle passenden Rezepte sind für die Besitzer-Agentur gesperrt (11.3, Tooltip {@code status.locked}). */
    public static final int HINT_LOCKED = 1;
    /** Die Maschine braucht einen Besitzer (Analysator, 6.1, Tooltip {@code status.no_owner}). */
    public static final int HINT_NO_OWNER = 2;
    /** Falscher Ort (Ortsbindung, z. B. Regolith-Erhitzer nur auf dem Mond). */
    public static final int HINT_LOCATION = 3;

    protected NonNullList<ItemStack> items = NonNullList.withSize(0, ItemStack.EMPTY);
    protected final IhEnergyStorage energy = new IhEnergyStorage(0, 0, 0, this::setChanged);
    private @Nullable Object platformEnergy;
    private @Nullable Object platformFluid;
    /** Gewählte Ausgangsseite für Fluide (4.1); null = {@code facing}. */
    private @Nullable Direction outputSide;
    /** Im GUI gewählter Tank für leere Behälter im Füllslot (4.1, Standard Tank 1). */
    private int selectedTank;
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

    /** Block-ID, unter der die Werte in den Balance-Dateien stehen (z. B. {@code infinite_horizon:crusher}). */
    public String machineId() {
        if (machineId == null) {
            machineId = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString();
        }
        return machineId;
    }

    private @Nullable String machineId;
    /** Balance-Stand, für den Slots und Puffer zuletzt eingerichtet wurden (neu nach /reload). */
    private @Nullable BalanceManager configuredFor;

    /** Slot-Aufbau laut Balance. */
    public abstract MachineLayout layout();

    /** Kapazität und Ein-/Ausgaberate laut Balance setzen ({@link IhEnergyStorage#reconfigure}). */
    protected abstract void configureEnergy(IhEnergyStorage energy);

    /** Ein Server-Tick (nur in geladenen Chunks, 3A.7). */
    public abstract void serverTick(ServerLevel level);

    /**
     * Werte für das GUI: 0 = Energie, 1 = Kapazität, 2 = Fortschritt bzw. Restbrennzeit, 3 = Gesamtdauer.
     * Unterklassen überschreiben 2 und 3.
     */
    protected long syncValue(int index) {
        if (index == SYNC_SIDE) {
            return hasOutputSide() ? outputSide().ordinal() + 1 : 0;
        }
        if (index == SYNC_TANK) {
            List<IhFluidTank> sources = fillSources();
            int offset = outputTanks().isEmpty() ? 0 : inputTanks().size();
            return Math.min(selectedTank, Math.max(0, sources.size() - 1)) | ((long) sources.size() << 8) | ((long) offset << 16);
        }
        if (index == SYNC_MULTIBLOCK || index == SYNC_MISSING) {
            return 0;
        }
        if (index == SYNC_HINT) {
            return statusHint();
        }
        if (index >= 4) {
            int t = (index - 4) / 3;
            List<IhFluidTank> tanks = fluidTanks();
            if (t >= tanks.size()) {
                return 0;
            }
            IhFluidTank tank = tanks.get(t);
            return switch ((index - 4) % 3) {
                case 0 -> FluidIds.toNetworkId(tank.stack().fluid());
                case 1 -> tank.stack().amount();
                default -> tank.capacity();
            };
        }
        return switch (index) {
            case 0 -> energy.getAmount();
            case 1 -> energy.getCapacity();
            default -> 0;
        };
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    /** Sind alle passenden Rezepte gesperrt (11.3)? Maschinen ohne Rezeptsuche: nie. */
    public boolean recipeLocked() {
        return false;
    }

    /** Zusatz zur Statuslampe im GUI ({@link #HINT_LOCKED}, {@link #HINT_NO_OWNER}, sonst 0). */
    public int statusHint() {
        return recipeLocked() ? HINT_LOCKED : 0;
    }

    /** Passt Slots und Puffer an die aktuellen Balance-Werte an. False, solange keine Werte geladen sind. */
    protected final boolean ensureLayout() {
        BalanceManager balance = BalanceManager.get();
        if (balance.isEmpty()) {
            return false;
        }
        if (balance == configuredFor) {
            return true;
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
        configuredFor = balance;
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
            long offer = energy.remainingExtract();
            long moved = pushEnergyTo(level, worldPosition, d, offer);
            if (moved > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.extract(moved, tx);
                    tx.commit();
                }
            }
        }
    }

    /**
     * Bietet {@code offer} J dem Nachbarn auf Seite {@code side} von {@code from} an (Kabelnetz, eigener Block oder
     * Loader-API) und gibt die angenommene Menge zurück. Der Aufrufer zieht sie aus seinem Speicher ab.
     */
    public static long pushEnergyTo(ServerLevel level, BlockPos from, Direction side, long offer) {
        BlockPos n = from.relative(side);
        if (level.getBlockState(n).getBlock() instanceof CableBlock) {
            EnergyGrid grid = GridManager.get(level).gridAt(level, n);
            return grid == null ? 0 : grid.push(level, from, offer);
        }
        BlockEntity be = level.getBlockEntity(n);
        EnergyTarget target = be instanceof EnergyNode node
                ? (node.acceptsEnergyFrom(side.getOpposite()) ? node.asTarget() : null)
                : Services.ENERGY.findTarget(level, n, side.getOpposite());
        return target == null ? 0 : target.insert(offer, false);
    }

    // ------------------------------------------------------------------ Fluide (4.1, 4.4)

    /** Hat der Block eine wählbare Ausgangsseite (Knopf „Seite“)? Standard: wenn er Ausgangstanks hat. */
    protected boolean hasOutputSide() {
        return !outputTanks().isEmpty();
    }

    /** Eingangstanks (nehmen von außen an). Standard: keine. */
    protected List<IhFluidTank> inputTanks() {
        return List.of();
    }

    /** Ausgangstanks (geben über die Ausgangsseite ab). Standard: keine. */
    protected List<IhFluidTank> outputTanks() {
        return List.of();
    }

    @Override
    public List<IhFluidTank> fluidTanks() {
        List<IhFluidTank> all = new ArrayList<>(inputTanks());
        for (IhFluidTank t : outputTanks()) {
            if (!all.contains(t)) {
                all.add(t);
            }
        }
        return all;
    }

    public Direction outputSide() {
        if (outputSide != null) {
            return outputSide;
        }
        BlockState s = getBlockState();
        return s.hasProperty(MachineBlock.FACING) ? s.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /**
     * Tanks, aus denen der Füllslot füllt und zwischen denen der Knopf „Tank“ wählt (4.1): die Ausgangstanks, bei
     * Speichern ohne Ausgangstank die Eingangstanks.
     */
    // SPEC-GAP: Knopf „Tank“ wählt nur unter den Füllquellen (spec-gaps #25)
    protected List<IhFluidTank> fillSources() {
        return outputTanks().isEmpty() ? inputTanks() : outputTanks();
    }

    /** Wählt den Tank (Index in {@link #fillSources()}) für leere Behälter im Füllslot. */
    public void setSelectedTank(int tank) {
        selectedTank = Math.max(0, Math.min(tank, Math.max(0, fillSources().size() - 1)));
        setChanged();
    }

    public int selectedTank() {
        return selectedTank;
    }

    public void setOutputSide(@Nullable Direction side) {
        outputSide = side;
        setChanged();
        if (level instanceof LevelAccessor accessor) {
            GridManager.invalidate(accessor);
        }
    }

    @Override
    public @Nullable FluidTarget fluidInput(@Nullable Direction side) {
        if (inputTanks().isEmpty() || (side != null && side == outputSide() && !outputTanks().isEmpty())) {
            return null;
        }
        return (offer, simulate) -> {
            try (Transaction tx = Transaction.open()) {
                long done = 0;
                for (IhFluidTank t : inputTanks()) {
                    done = t.insert(offer, tx);
                    if (done > 0) {
                        break;
                    }
                }
                if (!simulate && done > 0) {
                    tx.commit();
                }
                return done;
            }
        };
    }

    @Override
    public IhFluidStack extractFluid(@Nullable Direction side, @Nullable Identifier fluid, long max, boolean simulate) {
        if (side != null && !providesFluidTo(side)) {
            return IhFluidStack.EMPTY;
        }
        try (Transaction tx = Transaction.open()) {
            for (IhFluidTank t : outputTanks()) {
                IhFluidStack out = t.extract(fluid, max, tx);
                if (!out.isEmpty()) {
                    if (!simulate) {
                        tx.commit();
                    }
                    return out;
                }
            }
            return IhFluidStack.EMPTY;
        }
    }

    @Override
    public boolean providesFluidTo(@Nullable Direction side) {
        return !outputTanks().isEmpty() && side == outputSide();
    }

    @Override
    public void onFluidChanged() {
        setChanged();
    }

    @Override
    public Object platformFluidHandle(Function<FluidNode, Object> factory) {
        if (platformFluid == null) {
            platformFluid = factory.apply(this);
        }
        return platformFluid;
    }

    /** Schiebt den Inhalt der Ausgangstanks über die Ausgangsseite (Rohrnetz oder Nachbar). */
    protected void pushFluids(ServerLevel level) {
        Direction side = outputSide();
        for (IhFluidTank tank : outputTanks()) {
            IhFluidStack offer = tank.stack();
            if (offer.isEmpty()) {
                continue;
            }
            long moved = pushFluidTo(level, worldPosition, side, offer);
            if (moved > 0) {
                try (Transaction tx = Transaction.open()) {
                    tank.extract(offer.fluid(), moved, tx);
                    tx.commit();
                }
            }
        }
    }

    /**
     * Bietet {@code offer} dem Nachbarn auf Seite {@code side} von {@code from} an (Rohrnetz, eigener Block oder
     * Loader-API) und gibt die angenommene Menge in mB zurück. Der Aufrufer zieht sie aus seinem Tank ab.
     */
    public static long pushFluidTo(ServerLevel level, BlockPos from, Direction side, IhFluidStack offer) {
        BlockPos n = from.relative(side);
        if (level.getBlockState(n).getBlock() instanceof FluidPipeBlock) {
            FluidGrid grid = GridManager.get(level).fluidGridAt(level, n);
            return grid == null ? 0 : grid.push(level, from, offer);
        }
        BlockEntity be = level.getBlockEntity(n);
        FluidTarget target = be instanceof FluidNode node ? node.fluidInput(side.getOpposite())
                : Services.FLUID.findTarget(level, n, side.getOpposite());
        return target == null ? 0 : target.insert(offer, false);
    }

    /**
     * Füll- und Leerslot (4.1): Behälter werden ganz und sofort gefüllt bzw. geleert und bleiben im Slot liegen.
     * Ein leerer Behälter im Füllslot nimmt aus dem im GUI gewählten Tank ({@link #fillSources()}, Standard Tank 1);
     * der Leerslot gibt in den ersten passenden Eingangstank.
     */
    protected void handleContainers() {
        MachineLayout l = layout();
        List<IhFluidTank> sources = fillSources();
        for (int i = l.fillStart(); i < l.drainStart() && i < items.size() && !sources.isEmpty(); i++) {
            ItemStack empty = items.get(i);
            IhFluidTank tank = sources.get(Math.min(selectedTank, sources.size() - 1));
            // Behälter mit Komponente (Sauerstoffflaschen, ab M3): sofort, so viel wie passt
            if (ComponentContainers.canFill(empty, tank.stack().fluid()) && !tank.stack().isEmpty()) {
                long want = ComponentContainers.spec(empty).capacity() - ComponentContainers.content(empty).amount();
                try (Transaction tx = Transaction.open()) {
                    IhFluidStack got = tank.extract(tank.stack().fluid(), want, tx);
                    if (!got.isEmpty()) {
                        ComponentContainers.fill(empty, got.fluid(), got.amount());
                        tx.commit();
                        setChanged();
                    }
                }
                continue;
            }
            if (empty.getCount() != 1 || !FluidContainers.isEmptyContainer(empty)) {
                continue;
            }
            FluidContainers.Entry e = FluidContainers.fillable(empty, tank.stack().fluid());
            if (e != null && tank.stack().amount() >= e.amount()) {
                try (Transaction tx = Transaction.open()) {
                    tank.extract(e.fluid(), e.amount(), tx);
                    tx.commit();
                }
                items.set(i, FluidContainers.stackOf(e.full()));
                setChanged();
            }
        }
        for (int i = l.drainStart(); i < l.total() && i < items.size(); i++) {
            ItemStack full = items.get(i);
            if (ComponentContainers.hasContent(full)) {
                IhFluidStack c = ComponentContainers.content(full);
                for (IhFluidTank tank : inputTanks()) {
                    if (!tank.accepts(c.fluid()) || tank.space() <= 0) {
                        continue;
                    }
                    try (Transaction tx = Transaction.open()) {
                        long moved = tank.insert(IhFluidStack.of(c.fluid(), Math.min(c.amount(), tank.space())), tx);
                        if (moved > 0) {
                            ComponentContainers.drain(full, moved);
                            tx.commit();
                            setChanged();
                        }
                    }
                    break;
                }
                continue;
            }
            FluidContainers.Entry e = full.getCount() == 1 ? FluidContainers.ofFull(full) : null;
            if (e == null) {
                continue;
            }
            for (IhFluidTank tank : inputTanks()) {
                if (tank.accepts(e.fluid()) && tank.space() >= e.amount()) {
                    try (Transaction tx = Transaction.open()) {
                        tank.insert(IhFluidStack.of(e.fluid(), e.amount()), tx);
                        tx.commit();
                    }
                    items.set(i, FluidContainers.stackOf(e.empty()));
                    setChanged();
                    break;
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
        boolean generator = providesEnergyTo(null) && !acceptsEnergyFrom(null);
        return new MachineMenu.OpenData(worldPosition, layout(), max, Math.min(MAX_TANKS, fluidTanks().size()), generator);
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
        if (outputSide != null) {
            output.putString("output_side", outputSide.getSerializedName());
        }
        if (selectedTank > 0) {
            output.putInt("selected_tank", selectedTank);
        }
        List<IhFluidTank> tanks = fluidTanks();
        for (int t = 0; t < tanks.size(); t++) {
            if (!tanks.get(t).stack().isEmpty()) {
                output.store("tank" + t, IhFluidStack.CODEC, tanks.get(t).stack());
            }
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ensureLayout();
        ContainerHelper.loadAllItems(input, items);
        energy.setAmountUnchecked(input.getLongOr("energy", 0L));
        outputSide = input.getString("output_side").map(Direction::byName).orElse(null);
        selectedTank = input.getIntOr("selected_tank", 0);
        List<IhFluidTank> tanks = fluidTanks();
        for (int t = 0; t < tanks.size(); t++) {
            IhFluidTank tank = tanks.get(t);
            tank.setStackUnchecked(input.read("tank" + t, IhFluidStack.CODEC).orElse(IhFluidStack.EMPTY));
        }
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
    public IhEnergyStorage energyStorage() {
        return energy;
    }

    @Override
    public void onEnergyChanged() {
        setChanged();
    }

    /**
     * Markiert nur den Chunk als geändert. Vanilla meldet zusätzlich Komparatoren in der Nachbarschaft; Maschinen geben
     * kein Signal aus, und das kostet bei vielen Maschinen jeden Tick Zeit (Leistungstest 27.2). Blöcke mit
     * Komparator-Signal überschreiben diese Methode und rufen {@code super.setChanged()} des BlockEntity auf.
     */
    @Override
    public void setChanged() {
        if (level != null) {
            level.blockEntityChanged(worldPosition);
        }
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
            case FILL -> FluidContainers.isEmptyContainer(stack) || ComponentContainers.canFill(stack, null);
            case DRAIN -> FluidContainers.ofFull(stack) != null || ComponentContainers.hasContent(stack);
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
        return switch (layout().role(slot)) {
            case OUTPUT -> true;
            // Trichter ziehen aus dem Füllslot nur volle, aus dem Leerslot nur leere Behälter (4.1)
            case FILL -> FluidContainers.ofFull(stack) != null || ComponentContainers.isFull(stack);
            case DRAIN -> FluidContainers.isEmptyContainer(stack)
                    || (ComponentContainers.spec(stack) != null && !ComponentContainers.hasContent(stack));
            default -> false;
        };
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
