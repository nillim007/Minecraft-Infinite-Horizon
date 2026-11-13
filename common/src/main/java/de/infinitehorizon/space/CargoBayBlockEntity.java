package de.infinitehorizon.space;

import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.data.RocketPartRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Frachtraum als Block (12.4): {@code cargo_slots} Slots (27) wie eine Truhe. Belegte Slots zählen zur Masse
 * ({@code mass_per_slot}).
 */
public class CargoBayBlockEntity extends IhBlockEntity implements Container, MenuProvider {

    /** Vanilla-Truhenmenü mit drei Reihen. */
    private static final int ROWS = 3;
    private static final int PER_ROW = 9;

    private NonNullList<ItemStack> items = NonNullList.withSize(ROWS * PER_ROW, ItemStack.EMPTY);

    public CargoBayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Belegte Slots (für die Masse, 25.5). */
    public int usedSlots() {
        int n = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private int slots() {
        int fromData = RocketPartRegistry.get().part(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()))
                .map(RocketPartRegistry.Part::cargoSlots).orElse(ROWS * PER_ROW);
        return Math.min(fromData, ROWS * PER_ROW);
    }

    @Override
    public int getContainerSize() {
        return slots();
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
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(ROWS * PER_ROW, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }
}
