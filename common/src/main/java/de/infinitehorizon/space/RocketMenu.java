package de.infinitehorizon.space;

import de.infinitehorizon.core.fluid.ComponentContainers;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Raketen-GUI einer gelandeten Rakete (12.5): Frachtraum-Slots, Treibstoff (aus den synchronisierten Entity-Daten),
 * Knöpfe „Start“ (Navigationskarte) und „Nachtanken“. SPEC-GAP #76: das Slotpaar für Behälter kommt später;
 * nachgetankt wird über den Knopf aus {@code transport_tank} im Frachtraum.
 */
public class RocketMenu extends AbstractContainerMenu {

    /** Öffnungsdaten: Entity-ID und Zahl der Frachtslots. */
    public record Data(int entityId, int cargoSlots) {
        public static final StreamCodec<ByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::entityId,
                ByteBufCodecs.VAR_INT, Data::cargoSlots,
                Data::new);
    }

    public static final int SLOT = 18;
    public static final int CARGO_X = 8;
    public static final int CARGO_Y = 58;
    public static final int COLUMNS = 9;

    private final Data data;
    private final Container cargo;

    public RocketMenu(int id, Inventory inventory, Data data) {
        this(id, inventory, data, new SimpleContainer(data.cargoSlots()));
    }

    private RocketMenu(int id, Inventory inventory, Data data, Container cargo) {
        super(ModMenus.ROCKET.get(), id);
        this.data = data;
        this.cargo = cargo;
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            addSlot(new Slot(cargo, i, CARGO_X + (i % COLUMNS) * SLOT, CARGO_Y + (i / COLUMNS) * SLOT));
        }
        int rows = (cargo.getContainerSize() + COLUMNS - 1) / COLUMNS;
        int invY = CARGO_Y + rows * SLOT + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                addSlot(new Slot(inventory, c + r * COLUMNS + COLUMNS, CARGO_X + c * SLOT, invY + r * SLOT));
            }
        }
        for (int c = 0; c < COLUMNS; c++) {
            addSlot(new Slot(inventory, c, CARGO_X + c * SLOT, invY + 3 * SLOT + 4));
        }
    }

    public Data data() {
        return data;
    }

    /** Öffnet das GUI für den Spieler (Permissions: Gäste sehen, verwerfen aber Slot-Klicks, 23.1). */
    public static void open(ServerPlayer player, RocketEntity rocket) {
        Data data = new Data(rocket.getId(), rocket.cargo().getContainerSize());
        Services.REGISTRY.openMenu(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("entity.infinite_horizon.rocket");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new RocketMenu(id, inventory, data, rocket.cargo());
            }
        }, data, Data.CODEC);
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (!player.level().isClientSide() && player.level().getEntity(data.entityId()) instanceof RocketEntity r
                && !de.infinitehorizon.agency.Permissions.can(player, r.owner(), de.infinitehorizon.agency.Permissions.Action.USE_SLOTS)) {
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
        ItemStack copy = stack.copy();
        int cargoSize = cargo.getContainerSize();
        if (index < cargoSize ? !moveItemStackTo(stack, cargoSize, slots.size(), true) : !moveItemStackTo(stack, 0, cargoSize, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide()) {
            return true;
        }
        return player.level().getEntity(data.entityId()) instanceof RocketEntity r && !r.isRemoved()
                && (player.getVehicle() == r || player.distanceToSqr(r) <= 64);
    }

    /**
     * „Nachtanken“ (12.2): leert die {@code transport_tank} im Frachtraum, die eine Treibstoffkomponente der
     * Stufe enthalten, in Slot-Reihenfolge, bis die Raketentanks voll sind.
     */
    public static void refuelFromCargo(RocketEntity rocket) {
        Container c = rocket.cargo();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (ComponentContainers.spec(s) == null) {
                continue;
            }
            IhFluidStack content = ComponentContainers.content(s);
            int component = rocket.fuelIds().indexOf(content.fluid());
            if (content.isEmpty() || component < 0) {
                continue;
            }
            long taken = rocket.refuel(component, content.amount());
            if (taken > 0) {
                long left = content.amount() - taken;
                if (left <= 0) {
                    s.remove(ModDataComponents.FLUID.get());
                } else {
                    s.set(ModDataComponents.FLUID.get(), IhFluidStack.of(content.fluid(), left));
                }
                c.setChanged();
            }
        }
    }
}
