package de.infinitehorizon.shipyard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.logistics.CargoSink;
import de.infinitehorizon.logistics.OutpostManager;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.MachineLayout;
import de.infinitehorizon.machine.MachineLocation;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code shipyard_dock} (21.1, 20.3): Landeziel für Frachtraketen und Eingang des virtuellen Werftlagers derselben
 * Station. Angenommen wird nur, was die Werft noch braucht; Rohre und Trichter liefern ebenso. Eingehendes Methan füllt
 * zuerst den Treibstofftank (Rückflug der Raketen), erst der Rest geht ins Werftlager. Sternenkarten nur der eigenen
 * Agentur (18).
 */
public class ShipyardDockBlockEntity extends MachineBlockEntity implements CargoSink {

    /** Takt für Werft-Zuordnung und Anmeldung beim Logistik-Computer (technisch, 1 s). */
    private static final int INTERVAL = 20;

    private final IhFluidTank fuel = new IhFluidTank(0, null, this::setChanged);
    private @Nullable BlockPos yard;
    /** Zeitpunkt der letzten Suche nach der Werft; -1 = noch nie. */
    private long checkedAt = -1;

    public ShipyardDockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private JsonObject stats() {
        return BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
    }

    /** Ein Eingangsslot für Trichter; sein Inhalt geht sofort ins Werftlager. */
    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        return new MachineLayout(1, 0, 0, 0, 0);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        storage.reconfigure(0, 0, 0);
        JsonObject s = stats();
        if (s.has("fluid_tanks")) {
            for (JsonElement e : s.getAsJsonArray("fluid_tanks")) {
                JsonObject t = e.getAsJsonObject();
                if ("fuel".equals(t.get("role").getAsString())) {
                    Set<String> fluids = new HashSet<>();
                    if (t.has("fluids") && t.get("fluids").isJsonArray()) {
                        t.getAsJsonArray("fluids").forEach(f -> fluids.add(f.getAsString()));
                    }
                    fuel.reconfigure(t.get("capacity").getAsLong(), id -> fluids.isEmpty() || fluids.contains(id.toString()));
                }
            }
        }
    }

    @Override
    public IhFluidTank fuelTank() {
        return fuel;
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
        return List.of(fuel);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    /** Arbeitet das Dock hier (Orbit, Stationskern im Umkreis)? */
    public boolean valid(ServerLevel level) {
        int r = stats().has("requires_station_core_radius") ? stats().get("requires_station_core_radius").getAsInt() : 0;
        boolean station = r <= 0 || de.infinitehorizon.station.StationData.get(level).nearest(worldPosition, r) != null;
        return MachineLocation.allowed(stats(), level, worldPosition) && station;
    }

    /** Werft derselben Station oder null. */
    public ShipyardState.@Nullable Yard yard() {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        long now = sl.getGameTime();
        if (checkedAt < 0 || now < checkedAt || now - checkedAt >= INTERVAL) {
            checkedAt = now;
            ShipyardState.Yard y = valid(sl) ? ShipyardService.yardFor(sl, worldPosition) : null;
            yard = y == null ? null : y.pos();
        }
        return yard == null ? null : ShipyardState.get(sl.getServer()).yard(sl.dimension().identifier(), yard);
    }

    private static String id(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    @Override
    public long demand(String id) {
        ShipyardState.Yard y = yard();
        return y == null ? 0 : ShipyardState.demand(y, id);
    }

    @Override
    public long totalDemand() {
        ShipyardState.Yard y = yard();
        return y == null ? 0 : ShipyardState.totalDemand(y);
    }

    @Override
    public long amountOf(String id) {
        ShipyardState.Yard y = yard();
        return y == null || id == null || id.isEmpty() ? 0 : ShipyardState.delivered(y, id);
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        ShipyardState.Yard y = yard();
        if (stack.isEmpty() || y == null || !(level instanceof ServerLevel sl)) {
            return stack;
        }
        if (stack.has(ModDataComponents.CHART_OWNER.get())) {
            var owner = stack.get(ModDataComponents.CHART_OWNER.get());
            if (!ShipyardService.chartAllowed(sl, y, owner == null ? null : owner.agency())) {
                return stack;
            }
        }
        long took = ShipyardState.get(sl.getServer()).deliver(y, id(stack), stack.getCount(),
                sl.getServer().overworld().getGameTime(), simulate);
        if (!simulate && took > 0) {
            setChanged();
        }
        return took >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount((int) (stack.getCount() - took));
    }

    @Override
    public long insertFluid(IhFluidStack offer, boolean simulate) {
        if (offer.isEmpty()) {
            return 0;
        }
        long done = 0;
        // 20.1: Methan zuerst in den Treibstofftank
        try (Transaction tx = Transaction.open()) {
            if (fuel.accepts(offer.fluid())) {
                done = fuel.insert(offer, tx);
            }
            if (!simulate && done > 0) {
                tx.commit();
            }
        }
        ShipyardState.Yard y = yard();
        if (done < offer.amount() && y != null && level instanceof ServerLevel sl) {
            done += ShipyardState.get(sl.getServer()).deliver(y, offer.fluid().toString(), offer.amount() - done,
                    sl.getServer().overworld().getGameTime(), simulate);
        }
        return done;
    }

    @Override
    public @Nullable FluidTarget fluidInput(@Nullable Direction side) {
        return this::insertFluid;
    }

    /** Trichter: nur, was die Werft noch braucht. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return insertItem(stack, true).getCount() < stack.getCount();
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), INTERVAL) == 0) {
            OutpostManager.data(level).addDock(level.dimension().identifier(), worldPosition, storedOwner());
        }
        ItemStack in = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        if (!in.isEmpty()) {
            items.set(0, insertItem(in, false));
        }
        setStatus(yard() == null ? MachineStatus.ERROR : MachineStatus.IDLE);
    }

    /** Rechtsklick: kein eigenes GUI, sondern das der Werft (21.1). */
    @Override
    public void openMenu(ServerPlayer player) {
        ShipyardState.Yard y = yard();
        if (y != null) {
            ShipyardService.send(player, y.pos());
        } else {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.infinite_horizon.shipyard.no_yard"));
        }
    }
}
