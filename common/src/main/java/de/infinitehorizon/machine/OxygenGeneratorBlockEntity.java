package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.MachineStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Sauerstoffgenerator (10.2): erzeugt alle {@code ticks} {@code output_per_cycle} mB des Fluids aus {@code process}
 * (Sauerstoff) bei {@code energy_per_tick}, Upgrades wie Maschinen, funktioniert überall. Interner Puffer = Ausgangstank;
 * Abgabe über die Ausgangsseite (4.1), der Füllslot füllt Sauerstoffflaschen. Werte aus machines.json.
 */
public class OxygenGeneratorBlockEntity extends MachineBlockEntity {

    private final IhFluidTank tank = new IhFluidTank(0, id -> false, this::setChanged);
    private double progress;

    public OxygenGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IhFluidTank tank() {
        return tank;
    }

    public MachineStats stats() {
        return BalanceManager.get().machine(machineId());
    }

    private JsonObject json() {
        return BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
    }

    private JsonObject process() {
        JsonObject j = json();
        return j.has("process") ? j.getAsJsonObject("process") : new JsonObject();
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        JsonObject j = json();
        JsonObject slots = j.has("container_slots") ? j.getAsJsonObject("container_slots") : new JsonObject();
        return new MachineLayout(0, 0, 0, stats().upgradeSlots(), 0,
                slots.has("fill") ? slots.get("fill").getAsInt() : 0, slots.has("drain") ? slots.get("drain").getAsInt() : 0);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        MachineStats s = stats();
        storage.reconfigure(s.buffer(), s.buffer(), 0);
        long capacity = 0;
        JsonObject j = json();
        if (j.has("fluid_tanks") && !j.getAsJsonArray("fluid_tanks").isEmpty()) {
            capacity = j.getAsJsonArray("fluid_tanks").get(0).getAsJsonObject().get("capacity").getAsLong();
        }
        // Ausgangstank: nimmt von außen nichts an (4.1)
        tank.reconfigure(capacity, id -> false);
    }

    @Override
    protected List<IhFluidTank> outputTanks() {
        return List.of(tank);
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
    protected long syncValue(int index) {
        int ticks = ProcessingMath.effective(stats(), installedUpgrades()).ticks();
        return switch (index) {
            case 2 -> Math.round(progress * ticks);
            case 3 -> ticks;
            default -> super.syncValue(index);
        };
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        handleContainers();
        pushFluids(level);
        JsonObject p = process();
        long perCycle = p.has("output_per_cycle") ? p.get("output_per_cycle").getAsLong() : 0;
        Identifier fluid = p.has("fluid") ? Identifier.parse(p.get("fluid").getAsString()) : null;
        if (fluid == null || perCycle <= 0 || tank.space() < perCycle) {
            progress = 0;
            setStatus(MachineStatus.IDLE);
            return;
        }
        ProcessingMath.Effective eff = ProcessingMath.effective(stats(), installedUpgrades());
        try (Transaction tx = Transaction.open()) {
            if (!energy.consumeInternal(eff.energyPerTick(), tx)) {
                setStatus(MachineStatus.IDLE);
                return;
            }
            tx.commit();
        }
        progress += 1.0 / eff.ticks();
        setStatus(MachineStatus.RUNNING);
        if (progress < 1.0 - 1e-9) {
            setChanged();
            return;
        }
        progress = 0;
        try (Transaction tx = Transaction.open()) {
            tank.fill(IhFluidStack.of(fluid, perCycle), tx);
            tx.commit();
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getDoubleOr("progress", 0.0);
    }
}
