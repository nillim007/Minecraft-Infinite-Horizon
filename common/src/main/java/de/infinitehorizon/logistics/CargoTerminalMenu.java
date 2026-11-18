package de.infinitehorizon.logistics;

import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.registry.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Frachtterminal-GUI (20.1): 6 sichtbare Zeilen des Lagers, mit der Bildlaufleiste verschiebbar (Knopf-ID = erste
 * Zeile), Fluidleisten aller Tanks (Klick wählt den Tank für den Füllslot, Knopf-ID {@link #SELECT_TANK} + Tank),
 * Füll- und Leerslot, Spielerinventar. Der Server zeigt die gewählte Zeile und schickt danach alle Slots neu.
 */
public class CargoTerminalMenu extends AbstractContainerMenu {

    /** Öffnungsdaten: Position, Slot-Aufbau, Zahl der Tanks. */
    public record Data(BlockPos pos, MachineLayout layout, int tanks) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Data::pos,
                MachineLayout.STREAM_CODEC, Data::layout,
                ByteBufCodecs.VAR_INT, Data::tanks,
                Data::new);

        public int rows() {
            return (layout.inputs() + COLUMNS - 1) / COLUMNS;
        }
    }

    public static final int SLOT = 18;
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 6;
    public static final int VIEW = COLUMNS * VISIBLE_ROWS;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int FILL_X = 196;
    public static final int DRAIN_X = 226;
    public static final int CONTAINER_Y = 100;
    public static final int INVENTORY_Y = 140;
    public static final int SELECT_TANK = 1000;

    private final Data data;
    private final @Nullable CargoTerminalBlockEntity terminal;
    private final View view;
    private final ContainerData values;
    private final int menuSlots;

    /** Client: leere Hülle, Inhalte kommen per Slot-Sync. */
    public CargoTerminalMenu(int id, Inventory inventory, Data data) {
        this(id, inventory, data, null, new SimpleContainer(data.layout().total()), new SimpleContainerData(dataCount(data.tanks())));
    }

    /** Server: echtes Terminal. */
    public CargoTerminalMenu(int id, Inventory inventory, CargoTerminalBlockEntity terminal) {
        this(id, inventory, terminal.menuData(), terminal, terminal, terminal.terminalData());
    }

    private CargoTerminalMenu(int id, Inventory inventory, Data data, @Nullable CargoTerminalBlockEntity terminal,
                              Container storage, ContainerData values) {
        super(ModMenus.CARGO_TERMINAL.get(), id);
        this.data = data;
        this.terminal = terminal;
        this.values = values;
        MachineLayout l = data.layout();
        // Client: die Sicht-Slots zeigen einfach, was der Server schickt (Index 0 … VIEW-1 des Behälters)
        this.view = new View(storage, l.inputs(), terminal != null);
        for (int i = 0; i < VIEW; i++) {
            addSlot(new Slot(view, i, GRID_X + (i % COLUMNS) * SLOT, GRID_Y + (i / COLUMNS) * SLOT));
        }
        for (int i = 0; i < l.fill(); i++) {
            addSlot(new ContainerSlot(storage, l.fillStart() + i, FILL_X, CONTAINER_Y + i * SLOT));
        }
        for (int i = 0; i < l.drain(); i++) {
            addSlot(new ContainerSlot(storage, l.drainStart() + i, DRAIN_X, CONTAINER_Y + i * SLOT));
        }
        menuSlots = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inventory, col + row * COLUMNS + COLUMNS, GRID_X + col * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inventory, col, GRID_X + col * SLOT, INVENTORY_Y + 58));
        }
        addDataSlots(values);
    }

    /** Zahl der Sync-Werte (je Wert 4 × 16 bit): 3 je Tank und der gewählte Tank. */
    public static int dataCount(int tanks) {
        return (tanks * 3 + 1) * 4;
    }

    public Data data() {
        return data;
    }

    /** Synchronisierter Wert ({@code 3t} Fluid, {@code 3t+1} Menge, {@code 3t+2} Kapazität, danach gewählter Tank). */
    public long value(int index) {
        long v = 0;
        for (int k = 0; k < 4; k++) {
            v |= ((long) (values.get(index * 4 + k) & 0xFFFF)) << (16 * k);
        }
        return v;
    }

    public int selectedTank() {
        return (int) value(data.tanks() * 3);
    }

    /** Erste sichtbare Zeile (Server: echte Sicht; Client: zuletzt gewählte). */
    public int firstRow() {
        return view.row;
    }

    public int maxRow() {
        return Math.max(0, data.rows() - VISIBLE_ROWS);
    }

    /** Client: Zeile merken (der Server bekommt sie per Knopf-ID). */
    public void setFirstRowClient(int row) {
        view.row = Math.max(0, Math.min(row, maxRow()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= maxRow()) {
            view.row = id;
            sendAllDataToRemote();
            return true;
        }
        int tank = id - SELECT_TANK;
        if (terminal != null && tank >= 0 && tank < data.tanks()
                && Permissions.can(player, terminal.storedOwner(), Permissions.Action.USE_SLOTS)) {
            terminal.setSelectedTank(tank);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (!player.level().isClientSide() && terminal != null
                && !Permissions.can(player, terminal.storedOwner(), Permissions.Action.USE_SLOTS)) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < menuSlots) {
            if (!moveItemStackTo(stack, menuSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (terminal != null) {
            // ins ganze Lager, nicht nur in die sichtbaren Zeilen
            ItemStack rest = terminal.insertItem(stack, false);
            if (rest.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            stack.setCount(rest.getCount());
        } else if (!moveItemStackTo(stack, 0, VIEW, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return terminal == null || terminal.stillValid(player);
    }

    /** Sicht auf {@link #VIEW} Lagerslots ab Zeile {@code row} (nur Server; der Client zeigt die Slots 0 … VIEW-1). */
    private static final class View implements Container {
        private final Container storage;
        private final int size;
        private final boolean scrolling;
        int row;

        View(Container storage, int size, boolean scrolling) {
            this.storage = storage;
            this.size = size;
            this.scrolling = scrolling;
        }

        private int map(int i) {
            int k = (scrolling ? row * COLUMNS : 0) + i;
            return k < size ? k : -1;
        }

        @Override
        public int getContainerSize() {
            return VIEW;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < VIEW; i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int i) {
            int k = map(i);
            return k < 0 ? ItemStack.EMPTY : storage.getItem(k);
        }

        @Override
        public ItemStack removeItem(int i, int count) {
            int k = map(i);
            return k < 0 ? ItemStack.EMPTY : storage.removeItem(k, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            int k = map(i);
            return k < 0 ? ItemStack.EMPTY : storage.removeItemNoUpdate(k);
        }

        @Override
        public void setItem(int i, ItemStack stack) {
            int k = map(i);
            if (k >= 0) {
                storage.setItem(k, stack);
            }
        }

        @Override
        public boolean canPlaceItem(int i, ItemStack stack) {
            int k = map(i);
            return k >= 0 && storage.canPlaceItem(k, stack);
        }

        @Override
        public void setChanged() {
            storage.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return storage.stillValid(player);
        }

        @Override
        public void clearContent() {
        }
    }

    /** Füll- und Leerslot mit der Platzierungsregel des Terminals. */
    private static final class ContainerSlot extends Slot {
        ContainerSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
