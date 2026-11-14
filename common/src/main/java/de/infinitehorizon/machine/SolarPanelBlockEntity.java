package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Solarpanel (6.3, generators.json, SPEC-GAP #69): Leistung = {@code output_per_tick} × Solarfaktor (Planet bzw. Zone, 25.1) ×
 * Tageslicht ({@code daylight_curve}, 8.1) × Wetterfaktor, nur bei sichtbarem Himmel ({@code canSeeSky}). In Orbits
 * gilt {@code orbit} ohne Tageslicht (ab M6). Bruchteile von J werden über Ticks gesammelt. Schiebt in alle Nachbarn.
 */
public class SolarPanelBlockEntity extends MachineBlockEntity {

    /** Sync-Werte für das GUI in Hundertsteln J/t (aktuelle und höchste Leistung). */
    private static final int HUNDRED = 100;
    private static final String RAIN = "minecraft:rain";

    private double carry;
    private double lastOutput;

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject json() {
        return BalanceManager.get().entry("generators", machineId()).orElse(new JsonObject());
    }

    @Override
    public MachineLayout layout() {
        return BalanceManager.get().isEmpty() ? MachineLayout.EMPTY : new MachineLayout(0, 0, 0, 0, 0);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        JsonObject j = json();
        long buffer = j.has("buffer") ? j.get("buffer").getAsLong() : 0;
        storage.reconfigure(buffer, 0, buffer);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return true;
    }

    /** Leistung in J/t an dieser Stelle (reine Rechnung über die geladenen Daten). */
    public double output(ServerLevel level) {
        if (!level.canSeeSky(worldPosition.above())) {
            return 0;
        }
        JsonObject j = json();
        PlanetResolver.Resolved res = PlanetResolver.at(level, worldPosition);
        Planet planet = res.planet();
        boolean orbit = planet != null && planet.isOrbit();
        JsonObject base = j.has("output_per_tick") ? j.getAsJsonObject("output_per_tick") : new JsonObject();
        String kind = orbit ? "orbit" : "surface";
        double out = base.has(kind) ? base.get(kind).getAsDouble() : 0;
        out *= solarFactor(res);
        if (!orbit) {
            out *= HazardTracker.rules().daylight(level.getOverworldClockTime());
        }
        // Gewitter zählt wie Regen (6.3); Wetter-Ereignisse aus Kapitel 19 folgen später
        if (level.isRainingAt(worldPosition.above()) && j.has("weather_multipliers")) {
            JsonObject w = j.getAsJsonObject("weather_multipliers");
            if (w.has(RAIN)) {
                out *= w.get(RAIN).getAsDouble();
            }
        }
        return Math.max(0, out);
    }

    /** Solarfaktor des Planeten, überschrieben von der zuletzt passenden Zone (25.1). */
    static double solarFactor(PlanetResolver.Resolved res) {
        double f = res.planet() == null ? 1.0 : res.planet().solarFactor();
        for (Planet.Zone z : res.zones()) {
            if (z.solarFactor() != null) {
                f = z.solarFactor();
            }
        }
        return f;
    }

    /** Höchste Leistung an der Oberfläche (für das GUI). */
    private double maxOutput() {
        JsonObject j = json();
        JsonObject base = j.has("output_per_tick") ? j.getAsJsonObject("output_per_tick") : new JsonObject();
        return base.has("surface") ? base.get("surface").getAsDouble() : 0;
    }

    /** Leistung im letzten Tick in J/t. */
    public double lastOutput() {
        return lastOutput;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        lastOutput = output(level) * de.infinitehorizon.event.EventService.solarMultiplier(level, getBlockPos());
        if (lastOutput > 0 && energy.getAmount() < energy.getCapacity()) {
            carry += lastOutput;
            long whole = (long) carry;
            carry -= whole;
            if (whole > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.produceInternal(whole, tx);
                    tx.commit();
                }
                setChanged();
            }
        }
        pushEnergy(level);
        setStatus(lastOutput > 0 ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    @Override
    protected long syncValue(int index) {
        return switch (index) {
            case 2 -> Math.round(lastOutput * HUNDRED);
            case 3 -> Math.round(maxOutput() * HUNDRED);
            default -> super.syncValue(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("carry", carry);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        carry = input.getDoubleOr("carry", 0);
    }
}
