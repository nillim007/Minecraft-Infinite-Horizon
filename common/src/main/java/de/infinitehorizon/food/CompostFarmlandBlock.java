package de.infinitehorizon.food;

import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/** {@code compost_farmland} (17): verhält sich wie Vanilla-Ackerland, wird beim Austrocknen wieder {@code compost_soil}. */
public class CompostFarmlandBlock extends FarmlandBlock {

    public CompostFarmlandBlock(Properties properties) {
        // Basisblock wird unten ersetzt (compost_soil ist beim Bau dieses Blocks evtl. noch nicht registriert)
        super(Blocks.DIRT, properties);
    }

    public static BlockState soil() {
        return ModBlocks.simple("compost_soil").get().defaultBlockState();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        return s.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? s : soil();
    }

    @Override
    public void turnToBaseBlock(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState soil = pushEntitiesUp(state, soil(), level, pos);
        level.setBlockAndUpdate(pos, soil);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, soil));
    }
}
