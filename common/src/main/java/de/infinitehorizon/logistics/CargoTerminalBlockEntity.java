package de.infinitehorizon.logistics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.FluidIds;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code cargo_terminal} (20.1): Mittelpunkt eines Außenpostens. Lager mit {@code item_slots.storage} Slots, Frachttanks
 * und ein Treibstofftank (nur die Fluide aus {@code fluid_tanks}, Rolle {@code fuel}), Füll- und Leerslot. Eingehendes
 * Methan füllt zuerst den Treibstofftank, erst der Rest geht in die Frachttanks. Kein Strom.
 */
public class CargoTerminalBlockEntity extends MachineBlockEntity implements CargoSink {

    private final IhFluidTank fuel = new IhFluidTank(0, null, this::setChanged);
    /** Für Partner freigegeben (23.3): Partner-Raketen dürfen hierher liefern. */
    private boolean shared;
    private final List<IhFluidTank> cargo = new ArrayList<>();
    private final ContainerData terminalData = new ContainerData() {
        @Override
        public int get(int index) {
            long v = terminalValue(index / 4);
            return (int) ((v >>> (16 * (index % 4))) & 0xFFFF);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return CargoTerminalMenu.dataCount(fluidTanks().size());
        }
    };

    public CargoTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject stats() {
        return BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
    }

    /** Zahl der Lagerslots ({@code item_slots.storage}). */
    public int storageSlots() {
        JsonObject s = stats();
        return s.has("item_slots") ? s.getAsJsonObject("item_slots").get("storage").getAsInt() : 0;
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        JsonObject c = stats().has("container_slots") ? stats().getAsJsonObject("container_slots") : new JsonObject();
        int fill = c.has("fill") ? c.get("fill").getAsInt() : 0;
        int drain = c.has("drain") ? c.get("drain").getAsInt() : 0;
        // Lager als Eingaben: Trichter und Rohre dürfen hinein und heraus (siehe canTakeItemThroughFace);
        // die Hangar-Slots (20.3) liegen auf der Rolle „Formslot“ direkt hinter dem Lager
        return new MachineLayout(storageSlots(), hangarSlots(), 0, 0, 0, fill, drain);
    }

    /** Zahl der Hangar-Slots ({@code hangar_slots}, 20.3). */
    public int hangarSlots() {
        JsonObject s = stats();
        return s.has("hangar_slots") ? s.get("hangar_slots").getAsInt() : 0;
    }

    /** Rakete im Hangar-Slot {@code slot} (0-basiert) oder leer. */
    public ItemStack hangar(int slot) {
        int i = layout().moldStart() + slot;
        return slot >= 0 && slot < hangarSlots() && i < items.size() ? items.get(i) : ItemStack.EMPTY;
    }

    /** Ist die Rakete im Hangar-Slot gerade unterwegs (gesperrt, 20.3)? */
    public boolean hangarLocked(int slot) {
        return level instanceof ServerLevel sl && CargoFlights.isFlying(sl, getBlockPos(), slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        MachineLayout l = layout();
        if (l.role(slot) == MachineLayout.Role.MOLD) {
            return CargoRockets.isRocket(stack) && !hangarLocked(slot - l.moldStart());
        }
        return super.canPlaceItem(slot, stack);
    }

    /** Nimmt {@code count} Stück von {@code item} aus dem Lager (alles oder nichts). */
    public boolean takeItem(net.minecraft.world.item.Item item, int count) {
        if (count(item) < count) {
            return false;
        }
        int left = count;
        for (int i = 0; i < Math.min(storageSlots(), items.size()) && left > 0; i++) {
            ItemStack s = items.get(i);
            if (s.is(item)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        setChanged();
        return true;
    }

    /** Fracht laden (20.3): ganze Stapel aus dem Lager, die zum Filter passen, höchstens {@code maxStacks}. */
    public List<ItemStack> takeCargo(java.util.function.Predicate<ItemStack> filter, int maxStacks) {
        return takeCargo(filter, maxStacks, id -> Long.MAX_VALUE);
    }

    /** Wie oben, je Item-ID höchstens {@code limit} Stück (Bedarf der Werft). */
    public List<ItemStack> takeCargo(java.util.function.Predicate<ItemStack> filter, int maxStacks,
                                     java.util.function.ToLongFunction<String> limit) {
        List<ItemStack> out = new ArrayList<>();
        java.util.Map<String, Long> taken = new java.util.HashMap<>();
        for (int i = 0; i < Math.min(storageSlots(), items.size()) && out.size() < maxStacks; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty() || !filter.test(s)) {
                continue;
            }
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            long allowed = limit.applyAsLong(id) - taken.getOrDefault(id, 0L);
            int take = (int) Math.min(s.getCount(), Math.max(0, allowed));
            if (take <= 0) {
                continue;
            }
            out.add(s.copyWithCount(take));
            s.shrink(take);
            taken.merge(id, (long) take, Long::sum);
        }
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    /** Passende Item-Stapel im Lager (Startbedingung „wenn voll“). */
    public int matchingStacks(java.util.function.Predicate<String> ok) {
        int n = 0;
        for (int i = 0; i < Math.min(storageSlots(), items.size()); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ok.test(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) {
                n++;
            }
        }
        return n;
    }

    /** Gefüllte Frachttanks mit passendem Fluid. */
    public int matchingFluidHolds(java.util.function.Predicate<String> ok) {
        int n = 0;
        for (IhFluidTank t : cargo) {
            if (!t.stack().isEmpty() && ok.test(t.stack().fluid().toString())) {
                n++;
            }
        }
        return n;
    }

    @Override
    public long amountOf(String id) {
        if (id == null || id.isEmpty()) {
            return 0;
        }
        long n = 0;
        for (int i = 0; i < Math.min(storageSlots(), items.size()); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id)) {
                n += s.getCount();
            }
        }
        for (IhFluidTank t : fluidTanks()) {
            if (!t.stack().isEmpty() && t.stack().fluid().toString().equals(id)) {
                n += t.stack().amount();
            }
        }
        return n;
    }

    /** Fluidfracht laden (20.3): aus den Frachttanks, je Laderaum ein Fluid bis {@code capacity} mB. */
    public List<IhFluidStack> takeFluidCargo(java.util.function.Predicate<Identifier> filter, int holds, long capacity) {
        List<IhFluidStack> out = new ArrayList<>();
        for (IhFluidTank t : cargo) {
            if (out.size() >= holds) {
                break;
            }
            if (t.stack().isEmpty() || !filter.test(t.stack().fluid())) {
                continue;
            }
            try (Transaction tx = Transaction.open()) {
                IhFluidStack got = t.extract(t.stack().fluid(), capacity, tx);
                tx.commit();
                if (!got.isEmpty()) {
                    out.add(got);
                }
            }
        }
        return out;
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        storage.reconfigure(0, 0, 0);
        int cargoCount = 0;
        long cargoCapacity = 0;
        Set<String> cargoFluids = null;
        JsonObject s = stats();
        if (s.has("fluid_tanks")) {
            for (JsonElement e : s.getAsJsonArray("fluid_tanks")) {
                JsonObject t = e.getAsJsonObject();
                Set<String> fluids = fluids(t);
                if ("fuel".equals(t.get("role").getAsString())) {
                    fuel.reconfigure(t.get("capacity").getAsLong(), id -> fluids == null || fluids.contains(id.toString()));
                } else {
                    cargoCount += t.has("count") ? t.get("count").getAsInt() : 1;
                    cargoCapacity = t.get("capacity").getAsLong();
                    cargoFluids = fluids;
                }
            }
        }
        while (cargo.size() < cargoCount) {
            cargo.add(new IhFluidTank(0, null, this::setChanged));
        }
        while (cargo.size() > cargoCount) {
            cargo.removeLast();
        }
        Set<String> f = cargoFluids;
        for (IhFluidTank t : cargo) {
            t.reconfigure(cargoCapacity, id -> f == null || f.contains(id.toString()));
        }
    }

    private static @Nullable Set<String> fluids(JsonObject tank) {
        if (!tank.has("fluids") || !tank.get("fluids").isJsonArray()) {
            return null;
        }
        Set<String> out = new HashSet<>();
        tank.getAsJsonArray("fluids").forEach(e -> out.add(e.getAsString()));
        return out;
    }

    public IhFluidTank fuelTank() {
        return fuel;
    }

    public List<IhFluidTank> cargoTanks() {
        return List.copyOf(cargo);
    }

    /** Reihenfolge im GUI und beim Speichern: Treibstofftank, dann die Frachttanks. */
    @Override
    public List<IhFluidTank> fluidTanks() {
        List<IhFluidTank> all = new ArrayList<>();
        all.add(fuel);
        all.addAll(cargo);
        return all;
    }

    /** Leerslot und Rohre: zuerst der Treibstofftank (nur Methan), dann die Frachttanks. */
    @Override
    protected List<IhFluidTank> inputTanks() {
        return fluidTanks();
    }

    /** Über die Ausgangsseite gibt das Terminal nur Fracht ab, nicht den Treibstoff. */
    @Override
    protected List<IhFluidTank> outputTanks() {
        return List.copyOf(cargo);
    }

    /** Der Füllslot nimmt aus dem im GUI gewählten Tank, auch aus dem Treibstofftank. */
    @Override
    protected List<IhFluidTank> fillSources() {
        return fluidTanks();
    }

    @Override
    public de.infinitehorizon.api.fluid.@Nullable FluidTarget fluidInput(@Nullable Direction side) {
        if (side != null && side == outputSide()) {
            return null;
        }
        return this::insertFluid;
    }

    @Override
    public long insertFluid(IhFluidStack offer, boolean simulate) {
        if (offer.isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.open()) {
            long done = 0;
            // 20.1: Methan zuerst in den Treibstofftank
            if (fuel.accepts(offer.fluid())) {
                done += fuel.insert(offer, tx);
            }
            // dann Frachttanks mit demselben Fluid, danach leere
            for (int pass = 0; pass < 2 && done < offer.amount(); pass++) {
                for (IhFluidTank t : cargo) {
                    if (done >= offer.amount()) {
                        break;
                    }
                    boolean match = pass == 0 ? !t.stack().isEmpty() && t.stack().is(offer.fluid()) : t.stack().isEmpty();
                    if (match) {
                        done += t.insert(offer.withAmount(offer.amount() - done), tx);
                    }
                }
            }
            if (!simulate && done > 0) {
                tx.commit();
            }
            return done;
        }
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !ensureLayout()) {
            return stack;
        }
        int n = Math.min(storageSlots(), items.size());
        int left = stack.getCount();
        int max = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        // zuerst auf gleiche Stapel, dann in leere Slots
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (int i = 0; i < n && left > 0; i++) {
                ItemStack slot = items.get(i);
                if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < max) {
                    int move = Math.min(left, max - slot.getCount());
                    if (!simulate) {
                        slot.grow(move);
                    }
                    left -= move;
                } else if (pass == 1 && slot.isEmpty()) {
                    int move = Math.min(left, max);
                    if (!simulate) {
                        items.set(i, stack.copyWithCount(move));
                    }
                    left -= move;
                }
            }
        }
        if (!simulate && left != stack.getCount()) {
            setChanged();
        }
        return left == stack.getCount() ? stack : stack.copyWithCount(left);
    }

    /** Anzahl eines Items im Lager. */
    public int count(net.minecraft.world.item.Item item) {
        int c = 0;
        for (int i = 0; i < Math.min(storageSlots(), items.size()); i++) {
            if (items.get(i).is(item)) {
                c += items.get(i).getCount();
            }
        }
        return c;
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        MachineLayout l = layout();
        if (l.role(slot) == MachineLayout.Role.MOLD) {
            return !hangarLocked(slot - l.moldStart());
        }
        return l.role(slot) == MachineLayout.Role.INPUT || super.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + getBlockPos().asLong(), OutpostManager.SNAPSHOT_INTERVAL) == 0
                || !OutpostManager.data(level).hasTerminal(level.dimension().identifier(), getBlockPos())) {
            OutpostManager.registerTerminal(level, getBlockPos(), storedOwner(), shared);
        }
        handleContainers();
        pushFluids(level);
        setStatus(MachineStatus.IDLE);
    }

    // ------------------------------------------------------------------ GUI

    /** Werte für das GUI: je Tank Fluid, Menge, Kapazität; danach der gewählte Tank. */
    private long terminalValue(int index) {
        List<IhFluidTank> tanks = fluidTanks();
        int t = index / 3;
        if (t >= tanks.size()) {
            if (index == tanks.size() * 3) {
                return selectedTank();
            }
            // gesperrte Hangar-Slots als Bitmaske, Bit SHARED_BIT = für Partner freigegeben
            long mask = shared ? 1L << CargoTerminalMenu.SHARED_BIT : 0;
            for (int h = 0; h < hangarSlots(); h++) {
                if (hangarLocked(h)) {
                    mask |= 1L << h;
                }
            }
            return mask;
        }
        IhFluidTank tank = tanks.get(t);
        return switch (index % 3) {
            case 0 -> FluidIds.toNetworkId(tank.stack().fluid());
            case 1 -> tank.stack().amount();
            default -> tank.capacity();
        };
    }

    public boolean shared() {
        return shared;
    }

    /** Freigabe für Partner setzen (23.3) und sofort im Register vermerken. */
    public void setShared(boolean value) {
        shared = value;
        setChanged();
        if (level instanceof ServerLevel sl) {
            OutpostManager.registerTerminal(sl, getBlockPos(), storedOwner(), shared);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        if (shared) {
            output.putBoolean("shared", true);
        }
    }

    @Override
    public void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        shared = input.getBooleanOr("shared", false);
    }

    public ContainerData terminalData() {
        return terminalData;
    }

    public CargoTerminalMenu.Data menuData() {
        ensureLayout();
        return new CargoTerminalMenu.Data(getBlockPos(), layout(), fluidTanks().size());
    }

    @Override
    public void openMenu(ServerPlayer player) {
        Services.REGISTRY.openMenu(player, this, menuData(), CargoTerminalMenu.Data.STREAM_CODEC);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ensureLayout();
        return new CargoTerminalMenu(containerId, inventory, this);
    }

    /** Für Tests: Fluid-ID eines Tanks. */
    public @Nullable Identifier tankFluid(int t) {
        return fluidTanks().get(t).stack().fluid();
    }
}
