package de.infinitehorizon.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Erloschene Fackel (8.5): dekorativ, entsteht aus Fackeln außerhalb aktiver Kuppeln. Blockstate {@code facing}:
 * {@code up} = stehend, sonst an der Wand (wie Vanilla-Fackeln). Droppt sich selbst, kein Rezept.
 */
public class ExtinguishedTorchBlock extends Block {

    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class,
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private static final VoxelShape STANDING = Block.box(6, 0, 6, 10, 10, 10);
    private static final VoxelShape NORTH = Block.box(5.5, 3, 11, 10.5, 13, 16);
    private static final VoxelShape SOUTH = Block.box(5.5, 3, 0, 10.5, 13, 5);
    private static final VoxelShape WEST = Block.box(11, 3, 5.5, 16, 13, 10.5);
    private static final VoxelShape EAST = Block.box(0, 3, 5.5, 5, 13, 10.5);

    public ExtinguishedTorchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> STANDING;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : context.getNearestLookingDirections()) {
            Direction facing = dir == Direction.DOWN ? Direction.UP : dir.getAxis().isHorizontal() ? dir.getOpposite() : null;
            if (facing == null) {
                continue;
            }
            BlockState state = defaultBlockState().setValue(FACING, facing);
            if (state.canSurvive(level, pos)) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.UP) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
}
