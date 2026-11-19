package de.infinitehorizon.logistics;

import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.registry.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Menü des Handelspostens (23.3): links die Angebote (je Zeile Muster X und Y, Knopf „Tauschen“ = Knopf-ID des
 * Angebots, Vorrat), rechts das Lager und darunter das Inventar. Tauschen darf jeder; Lager-Slots nutzen nur
 * Mitglieder mit {@code USE_SLOTS} (sonst verwirft der Server den Klick und das GUI blendet das Lager aus). Muster
 * ändert der Client per {@code TradingPostEditPayload}; Klicks auf Muster-Slots verwirft das Menü.
 */
public class TradingPostMenu extends AbstractContainerMenu {

    /** Zusatzdaten beim Öffnen: Position, Zahl der Lagerslots und Angebote. */
    public record Data(BlockPos pos, int storage, int offers) {
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Data::pos,
                ByteBufCodecs.VAR_INT, Data::storage,
                ByteBufCodecs.VAR_INT, Data::offers,
                Data::new);
    }

    public static final int SLOT = 18;
    public static final int COLUMNS = 9;
    public static final int OFFER_X = 8;
    public static final int OFFER_Y = 18;
    public static final int OFFER_ROW = 22;
    /** Abstand Muster X → Muster Y (Pfeil dazwischen). */
    public static final int OFFER_GET_DX = 32;
    public static final int TRADE_X = 62;
    public static final int TRADE_W = 48;
    public static final int STORAGE_X = 146;
    public static final int STORAGE_Y = 18;
    public static final int INVENTORY_Y = 144;
    public static final int WIDTH = STORAGE_X + COLUMNS * SLOT + 8;
    public static final int HEIGHT = INVENTORY_Y + 3 * SLOT + 4 + SLOT + 8;
    /** Höchstwert der angezeigten Vorräte je Angebot (Sync über 16 bit). */
    public static final int MAX_SHOWN = 9999;

    private final Data data;
    private final @Nullable TradingPostBlockEntity post;
    private final ContainerData stock;
    private final DataSlot manage;
    private final int storageEnd;
    private final int offerEnd;

    /** Client: leere Hülle, Inhalte kommen per Slot-Sync. */
    public TradingPostMenu(int id, Inventory inventory, Data data) {
        this(id, inventory, data, null, new SimpleContainer(data.storage()), new SimpleContainer(2 * data.offers()),
                new SimpleContainerData(data.offers()));
    }

    /** Server: echter Handelsposten. */
    public TradingPostMenu(int id, Inventory inventory, TradingPostBlockEntity post) {
        this(id, inventory, post.menuData(), post, post, post.offerView(), new ContainerData() {
            @Override
            public int get(int index) {
                return (int) Math.min(MAX_SHOWN, post.available(index));
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return post.menuData().offers();
            }
        });
    }

    private TradingPostMenu(int id, Inventory inventory, Data data, @Nullable TradingPostBlockEntity post, Container storage,
                            Container offers, ContainerData stock) {
        super(ModMenus.TRADING_POST.get(), id);
        this.data = data;
        this.post = post;
        this.stock = stock;
        Player player = inventory.player;
        this.manage = post == null ? DataSlot.standalone() : new DataSlot() {
            @Override
            public int get() {
                return Permissions.can(player, post.storedOwner(), Permissions.Action.USE_SLOTS) ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        };
        for (int i = 0; i < data.storage(); i++) {
            addSlot(new StorageSlot(this, storage, i, STORAGE_X + (i % COLUMNS) * SLOT, STORAGE_Y + (i / COLUMNS) * SLOT));
        }
        storageEnd = slots.size();
        for (int i = 0; i < 2 * data.offers(); i++) {
            addSlot(new PatternSlot(offers, i, OFFER_X + (i % 2) * OFFER_GET_DX, OFFER_Y + (i / 2) * OFFER_ROW));
        }
        offerEnd = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inventory, col + row * COLUMNS + COLUMNS, STORAGE_X + col * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inventory, col, STORAGE_X + col * SLOT, INVENTORY_Y + 3 * SLOT + 4));
        }
        addDataSlots(stock);
        addDataSlot(manage);
    }

    public Data data() {
        return data;
    }

    public BlockPos pos() {
        return data.pos();
    }

    /** Darf der Spieler das Lager nutzen und Angebote ändern (Mitglied)? */
    public boolean canManage() {
        return manage.get() != 0;
    }

    /** Angezeigter Vorrat eines Angebots (wie oft noch getauscht werden kann). */
    public int available(int offer) {
        return offer >= 0 && offer < stock.getCount() ? stock.get(offer) : 0;
    }

    /** Ist {@code slotId} ein Muster-Slot? Dann Angebot {@code (slotId - storage) / 2}. */
    public boolean isPattern(int slotId) {
        return slotId >= storageEnd && slotId < offerEnd;
    }

    public int patternIndex(int slotId) {
        return slotId - storageEnd;
    }

    /** Knopf-ID = Angebot: tauschen (jeder Spieler, 23.3). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (post == null || id < 0 || id >= data.offers()) {
            return false;
        }
        TradingRules.Trade r = post.trade(player, id);
        if (r != TradingRules.Trade.OK && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.trading_post."
                    + r.name().toLowerCase(java.util.Locale.ROOT)));
        }
        return r == TradingRules.Trade.OK;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
        if (isPattern(slotId)) {
            return;
        }
        if (!player.level().isClientSide() && post != null
                && !Permissions.can(player, post.storedOwner(), Permissions.Action.USE_SLOTS)) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || isPattern(index)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index < storageEnd ? moveItemStackTo(stack, offerEnd, slots.size(), true)
                : moveItemStackTo(stack, 0, storageEnd, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return post == null || post.stillValid(player);
    }

    /** Lager-Slot: nur für Mitglieder sichtbar. */
    private static final class StorageSlot extends Slot {
        private final TradingPostMenu menu;

        StorageSlot(TradingPostMenu menu, Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return menu.canManage();
        }
    }

    /** Muster-Slot: zeigt X bzw. Y, nimmt nichts an und gibt nichts her. */
    private static final class PatternSlot extends Slot {
        PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
