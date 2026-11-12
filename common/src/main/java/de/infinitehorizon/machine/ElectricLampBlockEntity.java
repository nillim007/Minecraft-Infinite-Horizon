package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * Elektrische Lampe (8.5): Energiepuffer nach 4.1, verbraucht {@code energy_per_tick} und leuchtet dann
 * ({@code lit}, Lichtstufe aus machines.json), ohne Strom Licht 0. Funktioniert überall; kein GUI, keine Statuslampe.
 */
public class ElectricLampBlockEntity extends MachineBlockEntity {

    public ElectricLampBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject json() {
        return BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
    }

    private long energyPerTick() {
        JsonObject j = json();
        return j.has("energy_per_tick") ? j.get("energy_per_tick").getAsLong() : 0;
    }

    @Override
    public MachineLayout layout() {
        return MachineLayout.EMPTY;
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        JsonObject j = json();
        long buffer = j.has("buffer") && !j.get("buffer").isJsonNull() ? j.get("buffer").getAsLong()
                : BalanceManager.get().bufferFormula().apply(energyPerTick(), null);
        storage.reconfigure(buffer, buffer, 0);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        boolean lit = false;
        try (Transaction tx = Transaction.open()) {
            if (energyPerTick() > 0 && energy.consumeInternal(energyPerTick(), tx)) {
                tx.commit();
                lit = true;
            }
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
    }
}
