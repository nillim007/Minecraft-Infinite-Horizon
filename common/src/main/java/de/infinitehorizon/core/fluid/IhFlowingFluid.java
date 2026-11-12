package de.infinitehorizon.core.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Vanilla-Fluid (Quelle + fließend) nach einer {@link FluidDef} – genutzt auf Fabric. NeoForge nutzt
 * {@code BaseFlowingFluid}, weil Mod-Fluide dort einen {@code FluidType} brauchen.
 */
public abstract class IhFlowingFluid extends FlowingFluid {

    /** Gemeinsame Verweise beider Varianten (werden nach der Registrierung gesetzt). */
    public static final class Parts {
        public final FluidDef def;
        public Supplier<? extends Fluid> source;
        public Supplier<? extends Fluid> flowing;
        public @Nullable Supplier<? extends Block> block;
        public @Nullable Supplier<? extends Item> bucket;

        public Parts(FluidDef def) {
            this.def = def;
        }
    }

    protected final Parts parts;

    protected IhFlowingFluid(Parts parts) {
        this.parts = parts;
    }

    @Override
    public Fluid getFlowing() {
        return parts.flowing.get();
    }

    @Override
    public Fluid getSource() {
        return parts.source.get();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    @Override
    public Item getBucket() {
        return parts.bucket == null ? Items.AIR : parts.bucket.get();
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        // wie Vanilla-Wasser: der verdrängte Block droppt seine Items
        Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return parts.def.slopeFindDistance();
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return parts.def.dropOff();
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return parts.def.tickDelay();
    }

    @Override
    protected float getExplosionResistance() {
        return parts.def.explosionResistance();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        if (parts.block == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return parts.block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    public static final class Source extends IhFlowingFluid {
        public Source(Parts parts) {
            super(parts);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends IhFlowingFluid {
        public Flowing(Parts parts) {
            super(parts);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
