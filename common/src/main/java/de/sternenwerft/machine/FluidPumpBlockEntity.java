package de.sternenwerft.machine;

import com.google.gson.JsonObject;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.fluid.FluidIds;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.data.BalanceManager.MachineStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Pumpe (6.1): saugt alle {@code ticks} einen Fluid-Quellblock unterhalb ab (Suchquader laut machines.json →
 * {@code process}), füllt den Ausgangstank und schiebt über die Ausgangsseite. Ob der Quellblock verschwindet,
 * entscheidet {@code source_consume_chance} je Fluid (Erdöl 10 %, Ä25).
 */
public class FluidPumpBlockEntity extends MachineBlockEntity {

    private final SwFluidTank tank = new SwFluidTank(0, id -> false, this::setChanged);
    private double progress;
    private @Nullable BlockPos target;

    public FluidPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SwFluidTank tank() {
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
    protected void configureEnergy(SwEnergyStorage storage) {
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
    protected List<SwFluidTank> outputTanks() {
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

    /** Nächste Quelle im Suchquader (4.1/6.1): bevorzugt das Fluid im Tank, sonst die nächste überhaupt. */
    public @Nullable BlockPos findSource(ServerLevel level) {
        JsonObject p = process();
        int radius = p.has("search_radius") ? p.get("search_radius").getAsInt() : 0;
        Identifier want = tank.stack().fluid();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dy = 1; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos pos = worldPosition.offset(dx, -dy, dz);
                    FluidState fs = level.getFluidState(pos);
                    if (fs.isEmpty() || !fs.isSource()) {
                        continue;
                    }
                    if (want != null && !want.equals(FluidIds.sourceId(fs.getType()))) {
                        continue;
                    }
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d < bestDist || (d == bestDist && best != null && compare(pos, best) < 0)) {
                        best = pos;
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }

    private static int compare(BlockPos a, BlockPos b) {
        if (a.getX() != b.getX()) {
            return Integer.compare(a.getX(), b.getX());
        }
        if (a.getY() != b.getY()) {
            return Integer.compare(a.getY(), b.getY());
        }
        return Integer.compare(a.getZ(), b.getZ());
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        handleContainers();
        pushFluids(level);
        long perCycle = process().has("output_per_cycle") ? process().get("output_per_cycle").getAsLong() : 0;
        if (target == null || !level.getFluidState(target).isSource()) {
            target = findSource(level);
        }
        if (target == null || perCycle <= 0 || tank.space() < perCycle) {
            progress = 0;
            setStatus(target == null ? MachineStatus.IDLE : MachineStatus.ERROR);
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
        FluidState fs = level.getFluidState(target);
        if (!fs.isSource()) {
            target = null;
            return;
        }
        Identifier fluid = FluidIds.sourceId(fs.getType());
        try (Transaction tx = Transaction.open()) {
            tank.fill(SwFluidStack.of(fluid, perCycle), tx);
            tx.commit();
        }
        if (level.getRandom().nextDouble() < consumeChance(fluid)) {
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            target = null;
        }
        setChanged();
    }

    /** {@code source_consume_chance} je Fluid aus machines.json; ohne Eintrag 1 (Quelle wird entfernt). */
    private double consumeChance(Identifier fluid) {
        JsonObject p = process();
        if (p.has("source_consume_chance") && p.getAsJsonObject("source_consume_chance").has(fluid.toString())) {
            return p.getAsJsonObject("source_consume_chance").get(fluid.toString()).getAsDouble();
        }
        return p.has("consumes_source") && !p.get("consumes_source").getAsBoolean() ? 0.0 : 1.0;
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
