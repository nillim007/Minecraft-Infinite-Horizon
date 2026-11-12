package de.infinitehorizon.block;

import de.infinitehorizon.api.energy.EnergyNode;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Energiekabel (4.4). Eine neue Kabelsorte ist nur {@code new CableBlock(properties)} plus ein Eintrag mit
 * {@code throughput} (J/t) in {@code transport.json}.
 */
public class CableBlock extends ConduitBlock {

    public CableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean connectsToEndpoint(LevelReader level, BlockPos neighbor, Direction side) {
        BlockEntity be = level.getBlockEntity(neighbor);
        if (be instanceof EnergyNode node) {
            return node.acceptsEnergyFrom(side) || node.providesEnergyTo(side);
        }
        return level instanceof Level l && Services.ENERGY.findTarget(l, neighbor, side) != null;
    }
}
