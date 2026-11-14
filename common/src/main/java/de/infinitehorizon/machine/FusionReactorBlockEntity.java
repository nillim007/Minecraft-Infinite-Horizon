package de.infinitehorizon.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.MachineStats;
import de.infinitehorizon.multiblock.MultiblockControllerBlockEntity;
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
 * Fusionsreaktor (6.3, 4.3, 15): Controller eines 5×5×5-Multiblocks. Verbrennt {@code fuel} und braucht {@code coolant}
 * (Werte in generators.json); das zurückgegebene Helium geht in den Ausgabetank. Ohne Kühlung schaltet er nach
 * {@code shutdown_after_ticks_without_cooling} ab ({@link ReactorSafety}), ohne Explosion.
 */
public class FusionReactorBlockEntity extends MultiblockControllerBlockEntity {

    private ReactorSafety safety = ReactorSafety.START;
    private int cycleTicks;

    public FusionReactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private JsonObject cfg() {
        return BalanceManager.get().entry("generators", machineId()).orElse(new JsonObject());
    }

    public ReactorSafety safety() {
        return safety;
    }

    @Override
    public MachineStats stats() {
        long buffer = cfg().has("buffer") ? cfg().get("buffer").getAsLong() : 0;
        return new MachineStats(machineId(), 0, 0, buffer, 0, 0, 0, 0);
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        int[] containers = BalanceManager.get().containerSlots("generators", machineId());
        return new MachineLayout(0, 0, 0, 0, 0, containers[0], containers[1]);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        long buffer = stats().buffer();
        storage.reconfigure(buffer, 0, buffer);
        configureTanks(BalanceManager.get().tanks("generators", machineId()));
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return true;
    }

    private static Identifier fluid(JsonObject o) {
        return Identifier.parse(o.get("fluid").getAsString());
    }

    /** Menge eines Fluids in den Eingangstanks. */
    private long amount(Identifier fluid) {
        long sum = 0;
        for (IhFluidTank t : inputTanks()) {
            if (t.stack().is(fluid)) {
                sum += t.stack().amount();
            }
        }
        return sum;
    }

    private boolean available(@Nullable JsonElement list) {
        if (list == null || !list.isJsonArray()) {
            return true;
        }
        for (JsonElement e : list.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if (amount(fluid(o)) < o.get("amount").getAsLong()) {
                return false;
            }
        }
        return true;
    }

    /** Zieht je Eintrag {@code amount} ab; Kühlmittel geben {@code returns} in den Ausgabetank. */
    private void consume(@Nullable JsonElement list) {
        if (list == null || !list.isJsonArray()) {
            return;
        }
        try (Transaction tx = Transaction.open()) {
            for (JsonElement e : list.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                long left = o.get("amount").getAsLong();
                for (IhFluidTank t : inputTanks()) {
                    if (left > 0 && t.stack().is(fluid(o))) {
                        left -= t.extract(fluid(o), left, tx).amount();
                    }
                }
                if (o.has("returns")) {
                    JsonObject r = o.getAsJsonObject("returns");
                    IhFluidStack back = IhFluidStack.of(fluid(r), r.get("amount").getAsLong());
                    for (IhFluidTank t : outputTanks()) {
                        if (t.fill(back, tx) > 0) {
                            break;
                        }
                    }
                }
            }
            tx.commit();
        }
    }

    private int perTicks(@Nullable JsonElement list) {
        if (list == null || !list.isJsonArray() || list.getAsJsonArray().isEmpty()) {
            return 1;
        }
        return Math.max(1, list.getAsJsonArray().get(0).getAsJsonObject().get("per_ticks").getAsInt());
    }

    @Override
    protected void process(ServerLevel level) {
        JsonObject c = cfg();
        JsonObject s = c.has("safety") ? c.getAsJsonObject("safety") : new JsonObject();
        JsonElement fuel = c.get("fuel");
        JsonElement coolant = c.get("coolant");
        boolean fuelled = available(fuel);
        boolean wantsPower = energy.getAmount() < energy.getCapacity();
        // Kühlung läuft, solange Kühlmittel da ist; sie wird gebraucht, wenn er Strom erzeugen will oder abgeschaltet ist
        boolean active = fuelled && wantsPower || safety.shutdown();
        boolean cooled = available(coolant);
        if (active) {
            safety = safety.step(cooled, s.get("shutdown_after_ticks_without_cooling").getAsInt(),
                    s.get("restart_after_ticks_with_cooling").getAsInt());
        }
        boolean running = active && !safety.shutdown() && cooled && fuelled && wantsPower;
        if (active && cooled) {
            if (++cycleTicks >= perTicks(coolant)) {
                cycleTicks = 0;
                consume(coolant);
                if (running) {
                    consume(fuel);
                }
            }
        }
        if (running) {
            try (Transaction tx = Transaction.open()) {
                energy.produceInternal(c.get("output_per_tick").getAsLong(), tx);
                tx.commit();
            }
        }
        pushEnergy(level);
        setStatus(safety.shutdown() ? MachineStatus.ERROR : running ? MachineStatus.RUNNING : MachineStatus.IDLE);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("without_cooling", safety.withoutCooling());
        output.putInt("with_cooling", safety.withCooling());
        output.putBoolean("shutdown", safety.shutdown());
        output.putInt("cycle", cycleTicks);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        safety = new ReactorSafety(input.getIntOr("without_cooling", 0), input.getIntOr("with_cooling", 0),
                input.getBooleanOr("shutdown", false));
        cycleTicks = input.getIntOr("cycle", 0);
    }
}
