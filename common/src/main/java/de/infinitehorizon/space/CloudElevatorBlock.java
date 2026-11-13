package de.infinitehorizon.space;

import de.infinitehorizon.block.OwnedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** {@code cloud_elevator} (12.8): Rechtsklick startet die Fahrt zur verknüpften Station ({@link CloudLayer}). */
public class CloudElevatorBlock extends BaseEntityBlock implements OwnedBlock {

    private final Supplier<? extends BlockEntityType<? extends CloudElevatorBlockEntity>> type;

    public CloudElevatorBlock(Properties properties, Supplier<? extends BlockEntityType<? extends CloudElevatorBlockEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        OwnedBlock.claim(level, pos, placer);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            CloudLayer.requestRide(sl, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }
}
