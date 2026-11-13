package de.infinitehorizon.menu;

import de.infinitehorizon.machine.MachineBlockEntity;
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

/**
 * Gemeinsames Menü aller Maschinen und Generatoren (4.1). Slots folgen dem {@link MachineLayout}; Energie und
 * Fortschritt kommen über {@link ContainerData} als 16-Bit-Teile (Fabric überträgt nur 16 Bit je Wert).
 */
public class MachineMenu extends AbstractContainerMenu {

    /**
     * Was der Client beim Öffnen braucht (er kennt keine Balance-Werte); {@code tanks} = Zahl der Fluidleisten,
     * {@code generator} = Block erzeugt Energie (GUI zeigt die Brennzeit statt des Fortschritts).
     */
    public record OpenData(BlockPos pos, MachineLayout layout, int upgradeMax, int tanks, boolean generator) {
        public static final StreamCodec<ByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenData::pos,
                MachineLayout.STREAM_CODEC, OpenData::layout,
                ByteBufCodecs.VAR_INT, OpenData::upgradeMax,
                ByteBufCodecs.VAR_INT, OpenData::tanks,
                ByteBufCodecs.BOOL, OpenData::generator,
                OpenData::new);

        public MachineGeometry geometry() {
            return MachineGeometry.of(layout, tanks);
        }
    }

    // Geometrie in Pixeln (4.1): Breite 176, Spieler-Inventar unten mit Vanilla-Abständen
    public static final int SLOT = 18;
    public static final int INPUT_X = 38;
    public static final int MOLD_X = 56;
    public static final int ARROW_X = 79;
    public static final int OUTPUT_X = 110;
    public static final int ROW_Y = 35;
    public static final int UPGRADE_Y = 62;
    public static final int UPGRADE_X = 152;
    public static final int FILL_Y = 17;
    public static final int DRAIN_Y = 53;
    public static final int INVENTORY_Y = 84;

    /** Vorrang beim Shift-Klick aus dem Spielerinventar (4.1). */
    private static final MachineLayout.Role[] SHIFT_CLICK_ORDER = {MachineLayout.Role.UPGRADE, MachineLayout.Role.MOLD,
            MachineLayout.Role.FUEL, MachineLayout.Role.DRAIN, MachineLayout.Role.FILL, MachineLayout.Role.INPUT};

    private final Container container;
    private final ContainerData data;
    private final OpenData openData;
    private final int machineSlots;

    /** Server: echte Block-Entity. */
    public MachineMenu(int id, Inventory inventory, MachineBlockEntity machine) {
        this(id, inventory, machine, machine.dataAccess(), machine.openData());
    }

    /** Client: leere Hülle, Inhalte kommen per Slot-Sync. */
    public MachineMenu(int id, Inventory inventory, OpenData openData) {
        this(id, inventory, new SimpleContainer(openData.layout().total()),
                new SimpleContainerData(MachineBlockEntity.SYNC_VALUES * 4), openData);
    }

    private MachineMenu(int id, Inventory inventory, Container container, ContainerData data, OpenData openData) {
        super(ModMenus.MACHINE.get(), id);
        this.container = container;
        this.data = data;
        this.openData = openData;
        MachineLayout l = openData.layout();
        MachineGeometry g = openData.geometry();
        this.machineSlots = l.total();

        if (l.inputs() > MachineGeometry.GRID_COLUMNS) {
            // Raster 3 × n (Montagetisch: 9 Eingaben)
            int rows = (l.inputs() + MachineGeometry.GRID_COLUMNS - 1) / MachineGeometry.GRID_COLUMNS;
            for (int i = 0; i < l.inputs(); i++) {
                addSlot(new RoleSlot(container, i, g.inputX() + (i % MachineGeometry.GRID_COLUMNS) * SLOT,
                        ROW_Y - (rows - 1) * SLOT / 2 + (i / MachineGeometry.GRID_COLUMNS) * SLOT, true, 0));
            }
        } else {
            centered(l.inputs(), g.inputX(), 0);
        }
        for (int i = 0; i < l.molds(); i++) {
            addSlot(new RoleSlot(container, l.moldStart() + i, MOLD_X, ROW_Y + i * SLOT, true, 0));
        }
        int cols = l.outputs() > 3 ? 2 : 1;
        int rows = (l.outputs() + cols - 1) / Math.max(1, cols);
        for (int i = 0; i < l.outputs(); i++) {
            int x = g.outputX() + (i % cols) * SLOT;
            int y = ROW_Y - (rows - 1) * SLOT / 2 + (i / cols) * SLOT;
            addSlot(new RoleSlot(container, l.outputStart() + i, x, y, false, 0));
        }
        for (int i = 0; i < l.upgrades(); i++) {
            addSlot(new RoleSlot(container, l.upgradeStart() + i, g.upgradeX() - (l.upgrades() - 1 - i) * SLOT, g.upgradeY(),
                    true, openData.upgradeMax()));
        }
        centered(l.fuel(), INPUT_X, l.fuelStart());
        for (int i = 0; i < l.fill(); i++) {
            addSlot(new RoleSlot(container, l.fillStart() + i, g.containerX(), FILL_Y + i * SLOT, true, 1));
        }
        for (int i = 0; i < l.drain(); i++) {
            addSlot(new RoleSlot(container, l.drainStart() + i, g.containerX(), DRAIN_Y + i * SLOT, true, 1));
        }

        int invY = g.inventoryY();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * SLOT, invY + row * SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * SLOT, invY + 58));
        }
        addDataSlots(data);
    }

    private void centered(int count, int x, int start) {
        for (int i = 0; i < count; i++) {
            addSlot(new RoleSlot(container, start + i, x, ROW_Y - (count - 1) * SLOT / 2 + i * SLOT, true, 0));
        }
    }

    public OpenData openData() {
        return openData;
    }

    /** Synchronisierter Wert (0 Energie, 1 Kapazität, 2 Fortschritt/Brennzeit, 3 Gesamtdauer). */
    public long value(int index) {
        long v = 0;
        for (int k = 0; k < 4; k++) {
            v |= ((long) (data.get(index * 4 + k) & 0xFFFF)) << (16 * k);
        }
        return v;
    }

    /**
     * Gast-Rechte (23.1): Slot-Klicks von Nicht-Mitgliedern an fremden Block-Entities verwirft der Server; die
     * Vanilla-Synchronisierung setzt die Vorhersage des Clients zurück.
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (!player.level().isClientSide() && container instanceof de.infinitehorizon.block.IhBlockEntity be
                && !de.infinitehorizon.agency.Permissions.can(player, be.storedOwner(),
                de.infinitehorizon.agency.Permissions.Action.USE_SLOTS)) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = machineSlots;
        int playerEnd = slots.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Upgrade → Upgrade-Slot, Pressform → Formslot, sonst Eingabe (4.1): Ziel-Slots nach Vorrang der Rolle
            boolean moved = false;
            MachineLayout l = openData.layout();
            for (MachineLayout.Role role : SHIFT_CLICK_ORDER) {
                for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                    Slot target = slots.get(i);
                    if (l.role(target.getContainerSlot()) == role && target.mayPlace(stack)) {
                        moved |= moveItemStackTo(stack, i, i + 1, false);
                    }
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Slot mit Platzierungsregel des Containers (Server: Block-Entity, Client: frei). */
    private static final class RoleSlot extends Slot {
        private final boolean insertable;
        private final int max;

        RoleSlot(Container container, int index, int x, int y, boolean insertable, int max) {
            super(container, index, x, y);
            this.insertable = insertable;
            this.max = max;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return insertable && container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return max > 0 ? max : super.getMaxStackSize();
        }
    }
}
