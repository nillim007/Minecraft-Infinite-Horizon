package de.sternenwerft.block;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.core.grid.GridManager;
import de.sternenwerft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Energiekabel (4.4). Der Durchsatz steht per Block-ID in {@code transport.json}; eine neue Kabelsorte ist damit
 * nur ein {@code new CableBlock(properties)} plus ein Balance-Eintrag. Gleiche Sorten bilden ein Netz.
 */
public class CableBlock extends Block {

    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    // Geometrie in Pixeln (1/16 Block)
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11),
            Direction.UP, Block.box(5, 11, 5, 11, 16, 11),
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11),
            Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));

    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public CableBlock(Properties properties) {
        super(properties);
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : CONNECTIONS.values()) {
            def = def.setValue(p, false);
        }
        registerDefaultState(def);
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape shape = CORE;
            for (Map.Entry<Direction, BooleanProperty> e : CONNECTIONS.entrySet()) {
                if (s.getValue(e.getValue())) {
                    shape = Shapes.or(shape, ARMS.get(e.getKey()));
                }
            }
            shapes.put(s, shape);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.getOrDefault(state, CORE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState s = defaultBlockState();
        for (Direction d : Direction.values()) {
            s = s.setValue(CONNECTIONS.get(d), connects(level, pos.relative(d), d));
        }
        return s;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        boolean connected = connects(level, neighborPos, direction);
        BooleanProperty prop = CONNECTIONS.get(direction);
        if (state.getValue(prop) != connected && level instanceof LevelAccessor accessor) {
            GridManager.invalidate(accessor);
        }
        return state.setValue(prop, connected);
    }

    /** Verbindet sich mit gleichen Kabeln und mit Blöcken, die Energie annehmen oder abgeben. */
    protected boolean connects(LevelReader level, BlockPos neighbor, Direction fromCable) {
        BlockState ns = level.getBlockState(neighbor);
        if (ns.getBlock() == this) {
            return true;
        }
        if (ns.getBlock() instanceof CableBlock || ns.isAir()) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(neighbor);
        Direction side = fromCable.getOpposite();
        if (be instanceof EnergyNode node) {
            return node.acceptsEnergyFrom(side) || node.providesEnergyTo(side);
        }
        return level instanceof Level l && Services.ENERGY.findTarget(l, neighbor, side) != null;
    }
}
