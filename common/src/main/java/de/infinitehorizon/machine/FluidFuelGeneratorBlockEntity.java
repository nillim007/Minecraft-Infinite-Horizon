package de.infinitehorizon.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.GeneratorStats;
import de.infinitehorizon.data.BalanceManager.TankSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator mit Fluid-Brennstoff (Treibstoffgenerator, 6.3): verbraucht laut generators.json {@code amount} mB je
 * {@code per_ticks} Ticks aus seinem Brennstofftank und erzeugt dabei {@code output_per_tick}. Füll- und Leerslot
 * (4.1) füllen den Tank aus Eimern; Energie geht in alle Seiten. Bei vollem Puffer ruht die Verbrennung.
 *
 * <p>Addons: eigener Eintrag in generators.json mit {@code fuel: [{"fluid", "amount", "per_ticks"}]} und
 * {@code fluid_tanks}; die Block-Entity ist dieselbe Klasse.
 */
public class FluidFuelGeneratorBlockEntity extends MachineBlockEntity {

    /** Ein Brennstoff: {@code amount} mB reichen {@code perTicks} Ticks. */
    public record Fuel(Identifier fluid, long amount, int perTicks) {
    }

    private final List<IhFluidTank> tanks = new ArrayList<>();
    private int burnRemaining;
    private int burnTotal;

    public FluidFuelGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public GeneratorStats stats() {
        return BalanceManager.get().generator(machineId());
    }

    public int burnRemaining() {
        return burnRemaining;
    }

    /** Brennstoffe laut generators.json ({@code fuel} mit {@code fluid}). */
    public List<Fuel> fuels() {
        List<Fuel> out = new ArrayList<>();
        JsonObject e = BalanceManager.get().entry("generators", machineId()).orElse(new JsonObject());
        if (e.has("fuel") && e.get("fuel").isJsonArray()) {
            for (JsonElement f : e.getAsJsonArray("fuel")) {
                JsonObject o = f.getAsJsonObject();
                if (o.has("fluid") && o.get("fluid").isJsonPrimitive()) {
                    out.add(new Fuel(Identifier.parse(o.get("fluid").getAsString()), o.get("amount").getAsLong(),
                            o.get("per_ticks").getAsInt()));
                }
            }
        }
        return out;
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
        GeneratorStats s = stats();
        storage.reconfigure(s.buffer(), 0, s.buffer());
        List<TankSpec> specs = BalanceManager.get().tanks("generators", machineId());
        while (tanks.size() < specs.size()) {
            tanks.add(new IhFluidTank(0, id -> false, this::setChanged));
        }
        while (tanks.size() > specs.size()) {
            tanks.removeLast();
        }
        for (int i = 0; i < specs.size(); i++) {
            List<String> allowed = specs.get(i).fluids();
            // Brennstofftank: nimmt nur die Fluide aus den Daten an (sonst alle Brennstoffe)
            tanks.get(i).reconfigure(specs.get(i).capacity(), allowed != null ? id -> allowed.contains(id.toString())
                    : id -> fuels().stream().anyMatch(f -> f.fluid().equals(id)));
        }
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
        return tanks;
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
        handleContainers();
        GeneratorStats s = stats();
        boolean running = false;
        if (energy.getAmount() < energy.getCapacity()) {
            if (burnRemaining <= 0) {
                burnNext();
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

    /** Entnimmt die nächste Brennstoffportion aus einem Tank. */
    private void burnNext() {
        for (Fuel fuel : fuels()) {
            for (IhFluidTank tank : tanks) {
                if (tank.stack().is(fuel.fluid()) && tank.stack().amount() >= fuel.amount()) {
                    try (Transaction tx = Transaction.open()) {
                        tank.extract(fuel.fluid(), fuel.amount(), tx);
                        tx.commit();
                    }
                    burnRemaining = fuel.perTicks();
                    burnTotal = fuel.perTicks();
                    return;
                }
            }
        }
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
