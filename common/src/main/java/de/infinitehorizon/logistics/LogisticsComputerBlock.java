package de.infinitehorizon.logistics;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * {@code logistics_computer} (20.4): Rechtsklick zeigt alle Raketen und Routen der eigenen Agentur. Die Routen gehören
 * der Agentur, nicht dem Block; jeder Logistik-Computer zeigt dieselben.
 */
public class LogisticsComputerBlock extends Block {

    public LogisticsComputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MachineBlock.FACING, Direction.NORTH)
                .setValue(MachineBlock.STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineBlock.FACING, MachineBlock.STATUS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            LogisticsService.send(sp, pos, "");
        }
        return InteractionResult.SUCCESS;
    }
}
