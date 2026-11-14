package de.infinitehorizon.dome;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.EnvironmentCalculator;
import de.infinitehorizon.environment.EnvironmentInput;
import de.infinitehorizon.environment.EnvironmentRules;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Kuppelgenerator (10.1): Energiepuffer nach 4.1, O₂-Tank (nur {@code oxygen}, von allen Seiten, keine Ausgangsseite),
 * Füll- und Leerslot, Modulslots (nehmen in M3 nichts an). Zustände: {@code running} (Puffer ≥ Verbrauch eines Ticks,
 * Tank ≥ 1 mB, Außentemperatur im Bereich) → {@code failing} (schützt weiter, warnt alle 100 t, nach
 * {@code failure_ticks} aus) → aus. Werte aus domes.json.
 */
public class DomeGeneratorBlockEntity extends MachineBlockEntity {

    private static final String FILE = "domes";
    /** Bezugsdauer von {@code oxygen_per_1200} in Ticks (Name des Felds in domes.json). */
    private static final long PER = 1200;

    private final IhFluidTank tank = new IhFluidTank(0, id -> false, this::setChanged);
    private long oxygenCounter;
    private boolean everActive;

    public DomeGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IhFluidTank tank() {
        return tank;
    }

    /** Werte aus domes.json (Stationskern: eingebaute Kuppel, 14.1). */
    protected JsonObject json() {
        return BalanceManager.get().entry(FILE, machineId()).orElse(new JsonObject());
    }

    private static int num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : 0;
    }

    /** Wirksamer Radius: Grundwert × {@code radius_multiplier} der eingebauten Module (10.1). */
    public int radius() {
        return (int) Math.floor(num(json(), "radius") * DomeModules.factor(modules(), "radius_multiplier"));
    }

    /** Eingebaute Kuppelmodule (IDs aus den Modulslots). */
    public List<Identifier> modules() {
        List<Identifier> out = new java.util.ArrayList<>();
        MachineLayout l = layout();
        for (int i = l.upgradeStart(); i < l.fuelStart() && i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                out.add(BuiltInRegistries.ITEM.getKey(items.get(i).getItem()));
            }
        }
        return out;
    }

    public int tier() {
        return num(json(), "tier");
    }

    public long energyPerTick() {
        return Math.round(num(json(), "energy_per_tick") * DomeModules.factor(modules(), "energy_multiplier"));
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        JsonObject j = json();
        JsonObject slots = j.has("container_slots") ? j.getAsJsonObject("container_slots") : new JsonObject();
        // Modulslots (10.1) als Upgrade-Slots im GUI; sie nehmen in M3 nichts an (canPlaceItem)
        return new MachineLayout(0, 0, 0, num(j, "module_slots"), 0, num(slots, "fill"), num(slots, "drain"));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (layout().role(slot) == MachineLayout.Role.UPGRADE) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            // SPEC-GAP #95: jedes Modul höchstens einmal je Generator
            return DomeModules.isModule(id) && !modules().contains(id);
        }
        return super.canPlaceItem(slot, stack);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        JsonObject j = json();
        long buffer = j.has("buffer") && !j.get("buffer").isJsonNull() ? j.get("buffer").getAsLong()
                : BalanceManager.get().bufferFormula().apply(energyPerTick(), null);
        storage.reconfigure(buffer, buffer, 0);
        Set<Identifier> fluids = new HashSet<>();
        if (j.has("o2_fluids")) {
            j.getAsJsonArray("o2_fluids").forEach(f -> fluids.add(Identifier.parse(f.getAsString())));
        }
        tank.reconfigure(num(j, "o2_tank"), fluids::contains);
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
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

    /** Außentemperatur am Generator (10.1), ohne Kuppel und ohne Schutz gerechnet. */
    public double outsideTemperature(ServerLevel level) {
        EnvironmentRules rules = HazardTracker.rules();
        PlanetResolver.Resolved res = PlanetResolver.at(level, worldPosition);
        return EnvironmentCalculator.compute(new EnvironmentInput(res.planet(), res.zones(), res.biome(),
                level.getOverworldClockTime(), 0, false, null, rules.noSuit()), rules).temperature();
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        handleContainers();
        JsonObject j = json();
        JsonObject rules = DomeManager.rules();
        double temp = outsideTemperature(level);
        boolean inRange = temp >= num(j, "temp_min") && temp <= num(j, "temp_max");
        long perTick = energyPerTick();
        boolean energyOk = energy.getAmount() >= perTick;
        boolean oxygenOk = tank.stack().amount() >= 1;
        DomeData data = DomeData.get(level);
        DomeData.Entry entry = data.at(worldPosition);

        if (energyOk && oxygenOk && inRange) {
            consumeEnergy(perTick);
            consumeOxygen((int) Math.round(num(j, "oxygen_per_1200") * DomeModules.factor(modules(), "oxygen_multiplier")));
            everActive = true;
            if (entry == null || entry.failing() || entry.radius() != radius()) {
                data.put(new DomeData.Entry(worldPosition, radius(), tier(), Optional.ofNullable(owner()), -1));
                DomeManager.sync(level);
                // Erfolg „Eigene Luft“ (24.3) an alle Mitglieder der Besitzer-Agentur, offline beim nächsten Login
                if (entry == null && owner() != null) {
                    de.infinitehorizon.agency.AgencyAdvancements.awardToAgency(level.getServer(), owner(),
                            de.infinitehorizon.agency.AgencyAdvancements.OWN_AIR);
                }
            }
            setStatus(MachineStatus.RUNNING);
            return;
        }
        if (entry == null) {
            // aus: error bei falscher Temperatur (auch wenn nie aktiv), sonst idle
            setStatus(!inRange ? MachineStatus.ERROR : MachineStatus.IDLE);
            return;
        }
        if (energyOk) {
            consumeEnergy(perTick);
        }
        int failing = entry.failingTicks() + 1;
        int limit = num(rules, "failure_ticks");
        if (failing >= limit) {
            data.remove(worldPosition);
            DomeManager.sync(level);
            setStatus(!inRange ? MachineStatus.ERROR : MachineStatus.IDLE);
            return;
        }
        data.put(new DomeData.Entry(entry.center(), entry.radius(), entry.tier(), entry.agency(), failing));
        if (failing == 0 || entry.failingTicks() < 0) {
            DomeManager.sync(level);
        }
        warn(level, entry, failing, limit, rules);
        setStatus(MachineStatus.ERROR);
    }

    private void consumeEnergy(long amount) {
        try (Transaction tx = Transaction.open()) {
            if (energy.consumeInternal(amount, tx)) {
                tx.commit();
            }
        }
    }

    /**
     * Je Tick {@code oxygen_per_1200} / {@link #PER} mB, ganze mB werden abgezogen (10.1). Der Zähler rechnet in
     * 1/1.200 mB statt 1/1.000 mB, damit T1 genau alle 24 t 1 mB verbraucht (SPEC-GAP #59).
     */
    private void consumeOxygen(int per1200) {
        oxygenCounter += per1200;
        long whole = oxygenCounter / PER;
        if (whole > 0) {
            try (Transaction tx = Transaction.open()) {
                tank.extract(tank.stack().fluid(), whole, tx);
                tx.commit();
            }
            oxygenCounter -= whole * PER;
        }
        setChanged();
    }

    /** Alle {@code warning.interval_ticks}: Meldung in der Aktionsleiste mit Restsekunden und Warnton (10.1). */
    private void warn(ServerLevel level, DomeData.Entry entry, int failing, int limit, JsonObject rules) {
        JsonObject w = rules.has("warning") ? rules.getAsJsonObject("warning") : new JsonObject();
        int interval = num(w, "interval_ticks");
        if (interval <= 0 || failing % interval != 0) {
            return;
        }
        int seconds = (limit - failing) / 20;
        Component msg = Component.translatable(w.has("key") ? w.get("key").getAsString()
                : "message." + InfiniteHorizon.MOD_ID + ".dome.failing", seconds);
        var sound = w.has("sound") ? BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.parse(w.get("sound").getAsString()))
                : Optional.<net.minecraft.sounds.SoundEvent>empty();
        int below = rules.has("below_center_offset") ? rules.get("below_center_offset").getAsInt() : 0;
        for (ServerPlayer p : level.players()) {
            if (entry.contains(p.blockPosition(), below)) {
                p.sendOverlayMessage(msg);
                sound.ifPresent(s -> level.playSound(null, p.blockPosition(), s, SoundSource.BLOCKS, 1.0F, 1.0F));
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            DomeData data = DomeData.get(sl);
            if (data.at(worldPosition) != null) {
                data.remove(worldPosition);
                DomeManager.sync(sl);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("oxygen_counter", oxygenCounter);
        output.putBoolean("ever_active", everActive);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        oxygenCounter = input.getLongOr("oxygen_counter", 0L);
        everActive = input.getBooleanOr("ever_active", false);
    }
}
