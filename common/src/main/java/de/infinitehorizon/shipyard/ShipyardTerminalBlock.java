package de.infinitehorizon.shipyard;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.block.OwnedBlock;
import de.infinitehorizon.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * {@code shipyard_terminal} (21.1): nur in einer Station im Erdorbit. Rechtsklick zeigt die 8 Bauabschnitte; das
 * Gerüstmodell wächst mit {@link #STAGE} (fertige Abschnitte). Statuslampe: läuft = im Bau, Fehler = falscher Ort.
 */
public class ShipyardTerminalBlock extends BaseEntityBlock implements OwnedBlock {

    /** Fertige Abschnitte (Ausbaustufe des Gerüsts). */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 8);

    private final Supplier<? extends BlockEntityType<? extends ShipyardTerminalBlockEntity>> type;

    public ShipyardTerminalBlock(Properties properties,
                                 Supplier<? extends BlockEntityType<? extends ShipyardTerminalBlockEntity>> type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(MachineBlock.FACING, Direction.NORTH)
                .setValue(MachineBlock.STATUS, MachineStatus.IDLE).setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineBlock.FACING, MachineBlock.STATUS, STAGE);
    }

    /** Setzen nur in einer Station im Erdorbit (21.1); der Client kennt die Stationen nicht und fragt den Server. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel level && !ShipyardService.terminalAllowed(level, context.getClickedPos())) {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.shipyard.location"));
            }
            return null;
        }
        return defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        OwnedBlock.claim(level, pos, placer);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            OwnedBlock.claimOnOpen(level, pos, sp);
            ShipyardService.send(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide() || t != type.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ShipyardTerminalBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
