package de.infinitehorizon.shipyard;

import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Werft-Terminal (21.1): meldet die Werft im {@link ShipyardState} an, führt den Baufortschritt nach (Overworld-Zeit) und
 * zeigt ihn am Block (Gerüststufe, Statuslampe).
 */
public class ShipyardTerminalBlockEntity extends IhBlockEntity {

    /** Takt für Anmeldung und Bauzustand (technisch, 1 s). */
    private static final int INTERVAL = 20;

    public ShipyardTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void serverTick(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), INTERVAL) != 0) {
            return;
        }
        update(level);
    }

    /** Anmelden, Bau nachführen, Block anpassen (auch für Tests). */
    public ShipyardState.Yard update(ServerLevel level) {
        ShipyardState data = ShipyardState.get(level.getServer());
        ShipyardState.Yard y = data.register(level.dimension().identifier(), worldPosition, storedOwner());
        y = data.advance(y, level.getServer().overworld().getGameTime());
        boolean valid = ShipyardService.terminalAllowed(level, worldPosition);
        MachineStatus status = !valid ? MachineStatus.ERROR : y.buildEnd() > 0 ? MachineStatus.RUNNING : MachineStatus.IDLE;
        int stage = Math.min(8, y.built());
        BlockState s = getBlockState();
        if (s.getValue(MachineBlock.STATUS) != status || s.getValue(ShipyardTerminalBlock.STAGE) != stage) {
            level.setBlock(worldPosition, s.setValue(MachineBlock.STATUS, status).setValue(ShipyardTerminalBlock.STAGE, stage),
                    Block.UPDATE_ALL);
        }
        return y;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel sl) {
            ShipyardState.get(sl.getServer()).remove(sl.dimension().identifier(), pos);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
