package de.infinitehorizon.machine;

import de.infinitehorizon.block.MachineStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import de.infinitehorizon.menu.MachineMenu;
import de.infinitehorizon.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Block aller Maschinen und Generatoren (4.1): Blockstate {@code facing} (zum Spieler) und {@code status}.
 * Addons: {@code new MachineBlock(properties, () -> MEIN_BE_TYP)} genügt; das Verhalten steckt in der Block-Entity.
 */
public class MachineBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MachineStatus> STATUS = EnumProperty.create("status", MachineStatus.class);

    private final Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type;

    public MachineBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATUS, MachineStatus.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Rechtsklick öffnet das GUI (4.1); der Server entscheidet. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Sneak-Rechtsklick mit leerer Hand auf einen Multiblock-Controller: Geisterprojektion (4.1), rein clientseitig
        if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty()
                && level.getBlockEntity(pos) instanceof de.infinitehorizon.multiblock.MultiblockController) {
            if (level.isClientSide()) {
                de.infinitehorizon.multiblock.GhostPreview.show(level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            Services.REGISTRY.openMenu(sp, be, be.openData(), MachineMenu.OpenData.STREAM_CODEC);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    // ohne @Override: in 26.3 ist MODEL ohnehin Standard; schadet nicht, falls die Methode noch existiert
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != type.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> {
                if (be instanceof de.infinitehorizon.multiblock.MultiblockController) {
                    de.infinitehorizon.multiblock.GhostPreview.clientTick(lvl, pos, st);
                }
            };
        }
        return (lvl, pos, st, be) -> ((MachineBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
