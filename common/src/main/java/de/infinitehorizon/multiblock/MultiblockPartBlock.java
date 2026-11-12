package de.infinitehorizon.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gehäuseblock eines Multiblocks (z. B. {@code refinery_casing}, 4.3): meldet Setzen und Entfernen an die Controller in
 * der Nähe ({@link MultiblockRegistry}). Addons: {@code new MultiblockPartBlock(properties)} genügt.
 */
public class MultiblockPartBlock extends Block {

    public MultiblockPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) {
            MultiblockRegistry.notifyChange(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MultiblockRegistry.notifyChange(level, pos);
    }
}
