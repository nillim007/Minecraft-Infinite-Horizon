package de.sternenwerft.multiblock;

import de.sternenwerft.core.grid.GridManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Port eines Multiblocks (4.3): ersetzt einen Gehäuseblock und leitet Items, Fluide oder Energie an den Controller
 * weiter. Blockstate {@code mode} = {@code input} (Standard) oder {@code output}; Rechtsklick mit leerer Hand schaltet um.
 * Ein Port ohne gültigen Controller tut nichts.
 */
public class PortBlock extends MultiblockPartBlock implements EntityBlock {

    /** Was der Port weiterleitet. */
    public enum PortType { ITEM, FLUID, ENERGY }

    public enum Mode implements StringRepresentable {
        INPUT("input"), OUTPUT("output");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    private final PortType portType;
    private final Supplier<? extends BlockEntityType<? extends PortBlockEntity>> type;

    public PortBlock(Properties properties, PortType portType, Supplier<? extends BlockEntityType<? extends PortBlockEntity>> type) {
        super(properties);
        this.portType = portType;
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.INPUT));
    }

    public PortType portType() {
        return portType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    /** Rechtsklick mit leerer Hand: Modus umschalten und in der Aktionsleiste anzeigen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockState next = state.cycle(MODE);
            level.setBlock(pos, next, Block.UPDATE_ALL);
            GridManager.invalidate(level);
            player.sendOverlayMessage(Component.translatable("gui.sternenwerft.port." + next.getValue(MODE).getSerializedName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((PortBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
