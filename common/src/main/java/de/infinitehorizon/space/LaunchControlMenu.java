package de.infinitehorizon.space;

import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Menü des Raketen-Computers (12.4) ohne Slots; den Zustand schickt der Server ({@code LaunchStatePayload}). */
public class LaunchControlMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public LaunchControlMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.LAUNCH_CONTROL.get(), containerId);
        this.pos = pos;
        this.access = inventory.player.level().isClientSide() ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inventory.player.level(), pos);
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == ContainerLevelAccess.NULL || stillValid(access, player, ModBlocks.LAUNCH_CONTROL.get());
    }
}
