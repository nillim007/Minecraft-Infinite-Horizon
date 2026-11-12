package de.infinitehorizon.research;

import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Menü des Forschungsterminals (11.3) ohne Slots. Die Daten (FP, Forschung, Stufen) kommen über
 * {@code AgencyStatePayload}; Klicks gehen als {@code ResearchUnlockPayload} an den Server, der prüft, dass dieses Menü
 * offen und noch gültig ist (Vanilla-Reichweite wie bei Containern, SPEC-GAP #63).
 */
public class ResearchTerminalMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public ResearchTerminalMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.RESEARCH_TERMINAL.get(), containerId);
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
        return access == ContainerLevelAccess.NULL || stillValid(access, player, ModBlocks.RESEARCH_TERMINAL.get());
    }
}
