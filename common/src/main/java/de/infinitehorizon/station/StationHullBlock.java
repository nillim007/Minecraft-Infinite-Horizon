package de.infinitehorizon.station;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * {@code station_hull} (14.1, 14.2): Bauteil der Station. Die Plattform um einen neuen Stationskern trägt
 * {@code generated=true} und droppt nichts (Loot-Table mit Bedingung auf den Blockstate).
 */
public class StationHullBlock extends Block {

    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");

    public StationHullBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GENERATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GENERATED);
    }
}
