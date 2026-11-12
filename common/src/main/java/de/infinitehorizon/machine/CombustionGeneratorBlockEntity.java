package de.infinitehorizon.machine;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.GeneratorStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Generator mit Item-Brennstoff (generators.json): verbrennt Items mit Vanilla-Brennwert und schiebt Energie
 * in alle Nachbarn. Bei vollem Puffer ruht die Verbrennung.
 */
public class CombustionGeneratorBlockEntity extends MachineBlockEntity {

    private int burnRemaining;
    private int burnTotal;

    public CombustionGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public GeneratorStats stats() {
        return BalanceManager.get().generator(machineId());
    }

    public int burnRemaining() {
        return burnRemaining;
    }

    public int burnTotal() {
        return burnTotal;
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        return new MachineLayout(0, 0, 0, 0, stats().fuelSlots());
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        GeneratorStats s = stats();
        storage.reconfigure(s.buffer(), 0, s.buffer());
    }


    @Override
    protected long syncValue(int index) {
        return switch (index) {
            case 2 -> burnRemaining;
            case 3 -> burnTotal;
            default -> super.syncValue(index);
        };
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return true;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        GeneratorStats s = stats();
        boolean running = false;
        if (energy.getAmount() < energy.getCapacity()) {
            if (burnRemaining <= 0) {
                for (int i = 0; i < items.size() && burnRemaining <= 0; i++) {
                    ItemStack fuel = items.get(i);
                    int time = FuelHelper.burnTime(level, this, fuel);
                    if (time > 0) {
                        burnRemaining = time;
                        burnTotal = time;
                        // Behälter-Brennstoffe (Lavaeimer) lassen den leeren Behälter im Slot, wie der Vanilla-Ofen (6.3)
                        ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
                        if (fuel.getCount() == 1 && remainder != null) {
                            items.set(i, remainder.create());
                        } else {
                            fuel.shrink(1);
                        }
                    }
                }
            }
            if (burnRemaining > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.produceInternal(s.outputPerTick(), tx);
                    tx.commit();
                }
                burnRemaining--;
                running = true;
                setChanged();
            }
        }
        pushEnergy(level);
        setStatus(running ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_remaining", burnRemaining);
        output.putInt("burn_total", burnTotal);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnRemaining = input.getIntOr("burn_remaining", 0);
        burnTotal = input.getIntOr("burn_total", 0);
    }
}
