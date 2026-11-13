package de.infinitehorizon.space;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.block.OwnedBlock;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Block des Raketen-Computers (4.3): {@code facing} und Statuslampe {@code status} wie eine Maschine. */
public class LaunchControlBlock extends BaseEntityBlock implements OwnedBlock {

    private final Supplier<? extends BlockEntityType<? extends LaunchControlBlockEntity>> type;

    public LaunchControlBlock(Properties properties, Supplier<? extends BlockEntityType<? extends LaunchControlBlockEntity>> type) {
        super(properties);
        this.type = type;
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        OwnedBlock.claim(level, pos, placer);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof LaunchControlBlockEntity be) {
            OwnedBlock.claimOnOpen(level, pos, sp);
            be.evaluate((ServerLevel) level);
            Services.REGISTRY.openMenu(sp, be, pos, BlockPos.STREAM_CODEC);
            be.sendState(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((LaunchControlBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
