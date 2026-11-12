package de.infinitehorizon.research;

import de.infinitehorizon.block.IhBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Block-Entity des Forschungsterminals (11.3): nur Besitz ({@code owner_agency}) und das Menü. */
public class ResearchTerminalBlockEntity extends IhBlockEntity implements MenuProvider {

    public ResearchTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ResearchTerminalMenu(containerId, inventory, getBlockPos());
    }
}
