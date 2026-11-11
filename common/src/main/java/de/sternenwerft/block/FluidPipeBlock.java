package de.sternenwerft.block;

import de.sternenwerft.api.fluid.FluidNode;
import de.sternenwerft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Rohr für Fluide und Gase (4.4). Eine neue Rohrsorte ist nur {@code new FluidPipeBlock(properties)} plus ein Eintrag
 * mit {@code throughput} (mB/t) in {@code transport.json}. Rohre ziehen nie selbst; ein Netz führt ein Fluid zugleich.
 */
public class FluidPipeBlock extends ConduitBlock {

    public FluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean connectsToEndpoint(LevelReader level, BlockPos neighbor, Direction side) {
        BlockEntity be = level.getBlockEntity(neighbor);
        if (be instanceof FluidNode node) {
            return node.fluidInput(side) != null || node.providesFluidTo(side);
        }
        return level instanceof Level l && Services.FLUID.findTarget(l, neighbor, side) != null;
    }
}
