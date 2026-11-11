package de.sternenwerft.machine;

import com.google.gson.JsonObject;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Energiespeicher (Akku, 6.3, 4.4): nimmt über alle Seiten außer der Ausgangsseite an und schiebt nur über die
 * Ausgangsseite (Standard {@code facing}, im GUI wählbar). Ein- und Ausgang zusammen je höchstens {@code io_per_tick}
 * pro Tick. Der Ladeslot lädt Items mit Energie-Schnittstelle ({@link EnergyItems}). Beim Abbauen bleibt die Energie in
 * der Komponente {@code sternenwerft:energy} (4.1). Werte aus storage.json per Block-ID.
 */
public class BatteryBlockEntity extends MachineBlockEntity {

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        energy.setPerTickLimit(true);
    }

    private JsonObject stats() {
        return BalanceManager.get().entry("storage", machineId()).orElse(new JsonObject());
    }

    private long io() {
        JsonObject s = stats();
        return s.has("io_per_tick") && !s.get("io_per_tick").isJsonNull() ? s.get("io_per_tick").getAsLong() : 0;
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        JsonObject s = stats();
        int charge = s.has("charge_slots") ? s.get("charge_slots").getAsInt() : 0;
        return new MachineLayout(charge, 0, 0, 0, 0);
    }

    @Override
    protected void configureEnergy(SwEnergyStorage storage) {
        JsonObject s = stats();
        long io = io();
        storage.reconfigure(s.has("capacity") ? s.get("capacity").getAsLong() : 0, io, io);
    }

    @Override
    protected boolean hasOutputSide() {
        return true;
    }

    /** Der Ladeslot nimmt nur ladbare Items (6.3). */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return EnergyItems.isChargeable(stack);
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return side == null || side != outputSide();
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return side == null || side == outputSide();
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        // neue Tick-Grenze für Ein- und Ausgang (512 J/t)
        energy.newTick();
        boolean charging = false;
        MachineLayout l = layout();
        for (int i = 0; i < l.inputs() && i < items.size(); i++) {
            long offer = Math.min(io(), energy.getAmount());
            long done = EnergyItems.charge(this, i, offer);
            if (done > 0) {
                try (Transaction tx = Transaction.open()) {
                    energy.consumeInternal(done, tx);
                    tx.commit();
                }
                charging = true;
            }
        }
        long before = energy.getAmount();
        pushEnergy(level);
        setStatus(charging || energy.getAmount() < before ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    /** Beim Abbauen: Energie als {@code sternenwerft:energy} ans Item (Loot kopiert die Komponente, 4.1). */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.getAmount() > 0) {
            components.set(ModDataComponents.ENERGY.get(), energy.getAmount());
        }
    }

    /** Beim Platzieren eines geladenen Akkus. */
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Long stored = components.get(ModDataComponents.ENERGY.get());
        if (stored != null) {
            ensureLayout();
            energy.setAmountUnchecked(stored);
        }
    }
}
