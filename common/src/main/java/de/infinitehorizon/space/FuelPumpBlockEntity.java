package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Treibstoffpumpe (12.4, machines.json): Maschine ohne Strom mit 2 Eingangstanks; Tank 1 nimmt die erste, Tank 2 die
 * zweite Komponente aus {@code fuel} der Stufen an (T1: Kerosin bzw. Sauerstoff). Annahme von allen Seiten, Füll- und
 * Leerslot. Je Tick gibt sie bis {@code transfer_per_tick} je Komponente ab, gleichmäßig verteilt auf die
 * {@code rocket_tank_t1} ihrer Rampe (4.3). Gelandete Raketen kommen mit M4e dazu.
 */
public class FuelPumpBlockEntity extends MachineBlockEntity {

    private final List<IhFluidTank> tanks = new ArrayList<>();
    /** Raketentanks der Rampe; neu gesucht im Prüftakt der Multiblöcke. */
    private List<BlockPos> targets = List.of();
    private int untilScan;
    private boolean transferred;

    public FuelPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject stats() {
        return BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        int[] c = BalanceManager.get().containerSlots("machines", machineId());
        return new MachineLayout(0, 0, 0, 0, 0, c[0], c[1]);
    }

    /** Erlaubte Fluide der Komponente {@code index} über alle Stufen (rocket_parts.json → rules[].fuel). */
    private static Set<Identifier> component(int index) {
        Set<Identifier> out = new HashSet<>();
        for (RocketPartRegistry.Rule r : RocketPartRegistry.get().rules()) {
            if (index < r.fuel().size()) {
                out.add(r.fuel().get(index));
            }
        }
        return out;
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        storage.reconfigure(0, 0, 0);
        var specs = BalanceManager.get().tanks("machines", machineId());
        for (int i = 0; i < specs.size(); i++) {
            if (tanks.size() <= i) {
                tanks.add(new IhFluidTank(0, id -> false, this::setChanged));
            }
            final int index = i;
            tanks.get(i).reconfigure(specs.get(i).capacity(), id -> component(index).contains(id));
        }
        while (tanks.size() > specs.size()) {
            tanks.removeLast();
        }
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
        return tanks;
    }

    @Override
    protected List<IhFluidTank> outputTanks() {
        return List.of();
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    private long transferPerTick() {
        JsonObject s = stats();
        return s.has("transfer_per_tick") ? s.get("transfer_per_tick").getAsLong() : 0;
    }

    private static int scanInterval() {
        return BalanceManager.get().entry("misc", "multiblock_rules")
                .filter(o -> o.has("structure_check_interval_ticks"))
                .map(o -> o.get("structure_check_interval_ticks").getAsInt()).orElse(1);
    }

    /** Raketentanks der eigenen Rampe (für Tests). */
    public List<BlockPos> targets() {
        return targets;
    }

    /** Sucht die Rampe neu (z. B. nach dem Bau einer Rakete). */
    public void rescan(ServerLevel level) {
        LaunchPad.Check pad = LaunchPad.forPump(level, worldPosition);
        List<BlockPos> found = new ArrayList<>();
        if (pad != null && pad.load() == LaunchPad.Load.ROCKET) {
            for (BlockPos p : pad.stack()) {
                if (level.getBlockEntity(p) instanceof RocketTankBlockEntity) {
                    found.add(p);
                }
            }
        }
        targets = List.copyOf(found);
        untilScan = scanInterval();
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        handleContainers();
        if (--untilScan <= 0) {
            rescan(level);
        }
        transferred = false;
        for (int c = 0; c < tanks.size() && c < RocketTankBlockEntity.COMPONENTS; c++) {
            distribute(level, c);
        }
        setStatus(transferred ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    /** Gibt bis {@code transfer_per_tick} der Komponente {@code c} gleichmäßig an alle Raketentanks mit Platz ab. */
    private void distribute(ServerLevel level, int c) {
        IhFluidTank tank = tanks.get(c);
        IhFluidStack stack = tank.stack();
        if (stack.isEmpty() || targets.isEmpty()) {
            return;
        }
        long budget = Math.min(transferPerTick(), stack.amount());
        List<RocketTankBlockEntity> open = new ArrayList<>();
        for (BlockPos p : targets) {
            if (level.getBlockEntity(p) instanceof RocketTankBlockEntity t && t.fuel(c) < t.capacity()) {
                open.add(t);
            }
        }
        long given = 0;
        while (budget - given > 0 && !open.isEmpty()) {
            // Rest der Teilung zuerst an die leersten Tanks, damit alle gleich voll bleiben
            open.sort(java.util.Comparator.comparingLong(t -> t.fuel(c)));
            long share = Math.max(1, (budget - given) / open.size());
            List<RocketTankBlockEntity> full = new ArrayList<>();
            for (RocketTankBlockEntity t : open) {
                long want = Math.min(share, budget - given);
                if (want <= 0) {
                    break;
                }
                long took = t.fill(c, want);
                given += took;
                if (t.fuel(c) >= t.capacity()) {
                    full.add(t);
                }
            }
            open.removeAll(full);
            if (full.isEmpty() && share == 1 && given >= budget) {
                break;
            }
        }
        if (given > 0) {
            try (Transaction tx = Transaction.open()) {
                tank.extract(stack.fluid(), given, tx);
                tx.commit();
            }
            transferred = true;
            setChanged();
        }
    }
}
