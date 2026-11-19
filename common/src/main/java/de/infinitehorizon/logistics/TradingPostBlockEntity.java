package de.infinitehorizon.logistics;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code trading_post} (23.3): bis zu {@code offers_max} Angebote „gib X, erhalte Y“ und ein Lager mit
 * {@code storage_slots} Slots (beides aus {@code logistics.json}). Tauschen darf jeder (Ausnahme von den Gast-Rechten),
 * Lager und Angebote verwalten nur Mitglieder der Besitzer-Agentur. Frachtraketen der eigenen und der Partner-Agenturen
 * liefern Items ins Lager; Fluidfracht nimmt der Posten nicht an.
 */
public class TradingPostBlockEntity extends IhBlockEntity implements Container, MenuProvider, CargoSink {

    public static final String ID = "infinite_horizon:trading_post";

    private NonNullList<ItemStack> items = NonNullList.create();
    /** Muster je Angebot: Index {@code 2i} = X (gibt der Händler), {@code 2i + 1} = Y (erhält er). */
    private NonNullList<ItemStack> offers = NonNullList.create();
    private final Container offerView = new OfferView();

    public TradingPostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("logistics", ID).orElse(new JsonObject());
    }

    public static int storageSlots() {
        JsonObject r = rules();
        return r.has("storage_slots") ? r.get("storage_slots").getAsInt() : 0;
    }

    public static int maxOffers() {
        JsonObject r = rules();
        return r.has("offers_max") ? r.get("offers_max").getAsInt() : 0;
    }

    /** Listen auf die Größen aus den Balance-Daten bringen (Inhalt bleibt, soweit er passt). */
    private void ensureSize() {
        items = resize(items, storageSlots());
        offers = resize(offers, 2 * maxOffers());
    }

    private static NonNullList<ItemStack> resize(NonNullList<ItemStack> list, int size) {
        if (list.size() == size) {
            return list;
        }
        NonNullList<ItemStack> out = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            out.set(i, list.get(i));
        }
        return out;
    }

    // ------------------------------------------------------------------ Angebote

    public Container offerView() {
        ensureSize();
        return offerView;
    }

    /** Muster X ({@code give = true}) bzw. Y eines Angebots. */
    public ItemStack offer(int index, boolean give) {
        ensureSize();
        int i = 2 * index + (give ? 0 : 1);
        return i >= 0 && i < offers.size() ? offers.get(i) : ItemStack.EMPTY;
    }

    /** Muster setzen (Menge auf 1 … Stapelgröße begrenzt, leer = löschen); nur über {@code TradingPostEditPayload}. */
    public void setOffer(int slot, ItemStack pattern) {
        ensureSize();
        if (slot < 0 || slot >= offers.size()) {
            return;
        }
        ItemStack p = pattern.isEmpty() ? ItemStack.EMPTY : pattern.copyWithCount(
                Math.max(1, Math.min(pattern.getCount(), pattern.getMaxStackSize())));
        offers.set(slot, p);
        setChanged();
    }

    private long count(ItemStack pattern) {
        long n = 0;
        for (ItemStack s : items) {
            if (ItemStack.isSameItemSameComponents(s, pattern)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Wie oft das Angebot aus dem Lager noch bedient werden kann. */
    public long available(int index) {
        ItemStack get = offer(index, false);
        return offer(index, true).isEmpty() || get.isEmpty() ? 0 : TradingRules.available(count(get), get.getCount());
    }

    /** Tauscht einmal nach Angebot {@code index} (23.3); jeder Spieler darf. */
    public TradingRules.Trade trade(Player player, int index) {
        ItemStack give = offer(index, true);
        ItemStack get = offer(index, false);
        if (give.isEmpty() || get.isEmpty()) {
            return TradingRules.Trade.NO_OFFER;
        }
        NonNullList<ItemStack> inv = player.getInventory().getNonEquipmentItems();
        long has = 0;
        for (ItemStack s : inv) {
            if (ItemStack.isSameItemSameComponents(s, give)) {
                has += s.getCount();
            }
        }
        // Probe auf einer Kopie des Lagers: Y heraus, X hinein
        List<ItemStack> copy = new ArrayList<>();
        items.forEach(s -> copy.add(s.copy()));
        long stock = count(get);
        boolean fits = stock >= get.getCount() && take(copy, get, get.getCount()) && insert(copy, give.copy(), true).isEmpty();
        TradingRules.Trade r = TradingRules.check(give.getCount(), get.getCount(), has, stock, fits);
        if (r != TradingRules.Trade.OK) {
            return r;
        }
        int need = give.getCount();
        for (int i = 0; i < inv.size() && need > 0; i++) {
            ItemStack s = inv.get(i);
            if (ItemStack.isSameItemSameComponents(s, give)) {
                int t = Math.min(need, s.getCount());
                s.shrink(t);
                need -= t;
            }
        }
        take(items, get, get.getCount());
        insert(items, give.copy(), false);
        // ins Inventar, Rest fällt vor dem Spieler zu Boden
        player.getInventory().placeItemBackInInventory(get.copy(), net.minecraft.util.Prediction.SERVER_ONLY);
        player.getInventory().setChanged();
        setChanged();
        return r;
    }

    /** Nimmt {@code amount} Stück nach Muster aus {@code list}; false, wenn nicht genug da war. */
    private static boolean take(List<ItemStack> list, ItemStack pattern, int amount) {
        int need = amount;
        for (int i = list.size() - 1; i >= 0 && need > 0; i--) {
            ItemStack s = list.get(i);
            if (ItemStack.isSameItemSameComponents(s, pattern)) {
                int t = Math.min(need, s.getCount());
                s.shrink(t);
                need -= t;
            }
        }
        return need == 0;
    }

    /** Legt {@code stack} in {@code list} (erst auf gleiche Stapel, dann in leere Slots); gibt den Rest zurück. */
    private static ItemStack insert(List<ItemStack> list, ItemStack stack, boolean simulate) {
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
                ItemStack s = list.get(i);
                if (pass == 0 && !s.isEmpty() && ItemStack.isSameItemSameComponents(s, rest)) {
                    int t = Math.min(rest.getCount(), s.getMaxStackSize() - s.getCount());
                    if (t > 0) {
                        if (!simulate) {
                            s.grow(t);
                        }
                        rest.shrink(t);
                    }
                } else if (pass == 1 && s.isEmpty()) {
                    int t = Math.min(rest.getCount(), rest.getMaxStackSize());
                    if (!simulate) {
                        list.set(i, rest.copyWithCount(t));
                    }
                    rest.shrink(t);
                }
            }
        }
        return rest;
    }

    // ------------------------------------------------------------------ Fracht (20.3)

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        ensureSize();
        ItemStack rest = insert(items, stack, simulate);
        if (!simulate && rest.getCount() != stack.getCount()) {
            setChanged();
        }
        return rest;
    }

    @Override
    public long insertFluid(IhFluidStack offer, boolean simulate) {
        return 0;
    }

    @Override
    public boolean acceptsFluidCargo() {
        JsonObject r = rules();
        return r.has("accepts_fluid_cargo") && r.get("accepts_fluid_cargo").getAsBoolean();
    }

    @Override
    public long amountOf(String id) {
        long n = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(id)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ Register und Abbau

    public void serverTick(ServerLevel level) {
        ensureSize();
        var data = OutpostManager.data(level);
        if (Math.floorMod(level.getGameTime() + getBlockPos().asLong(), OutpostManager.SNAPSHOT_INTERVAL) == 0
                || !data.isTradingPost(level.dimension().identifier(), getBlockPos())) {
            data.addTradingPost(level.dimension().identifier(), getBlockPos(), storedOwner());
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, this);
        }
        if (level instanceof ServerLevel sl) {
            OutpostManager.onRemoved(sl, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ------------------------------------------------------------------ Container (Lager)

    @Override
    public int getContainerSize() {
        ensureSize();
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
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
        ensureSize();
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

    // ------------------------------------------------------------------ GUI

    public TradingPostMenu.Data menuData() {
        ensureSize();
        return new TradingPostMenu.Data(getBlockPos(), items.size(), offers.size() / 2);
    }

    public void openMenu(ServerPlayer player) {
        Services.REGISTRY.openMenu(player, this, menuData(), TradingPostMenu.Data.STREAM_CODEC);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TradingPostMenu(id, inventory, this);
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("offers", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(offers));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(storageSlots(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        offers = NonNullList.withSize(2 * maxOffers(), ItemStack.EMPTY);
        List<ItemStack> stored = input.read("offers", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(stored.size(), offers.size()); i++) {
            offers.set(i, stored.get(i));
        }
    }

    /** Sicht auf die Muster für die Angebots-Slots des Menüs (nur Anzeige; Änderungen per Payload). */
    private final class OfferView implements Container {
        @Override
        public int getContainerSize() {
            return offers.size();
        }

        @Override
        public boolean isEmpty() {
            return offers.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < offers.size() ? offers.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
        }

        @Override
        public void setChanged() {
            TradingPostBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return TradingPostBlockEntity.this.stillValid(player);
        }

        @Override
        public void clearContent() {
        }
    }
}
