package de.sternenwerft.platform;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.platform.services.IEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** NeoForge-Energie: {@code Capabilities.Energy.BLOCK} (Transfer-API mit Transaktionen). */
public class NeoForgeEnergyHelper implements IEnergyHelper {

    private static final List<Supplier<? extends BlockEntityType<? extends EnergyNode>>> EXPOSED = new ArrayList<>();
    private static final Map<BlockPos, SimpleEnergyHandler> TEST_CONSUMERS = new ConcurrentHashMap<>();

    @Override
    public @Nullable EnergyTarget findTarget(Level level, BlockPos pos, Direction side) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        if (handler == null) {
            return null;
        }
        return (amount, simulate) -> {
            int request = (int) Math.min(amount, Integer.MAX_VALUE);
            if (request <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(request, tx);
                if (!simulate) {
                    tx.commit();
                }
                return inserted;
            }
        };
    }

    @Override
    public void expose(Supplier<? extends BlockEntityType<? extends EnergyNode>> type) {
        EXPOSED.add(type);
    }

    @Override
    public long testConsumerAmount(Level level, BlockPos pos) {
        SimpleEnergyHandler h = TEST_CONSUMERS.get(pos);
        return h == null ? -1 : h.getAmountAsLong();
    }

    @Override
    public boolean isPlatformChargeable(ItemStack stack) {
        EnergyHandler h = stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack.copy()));
        return h != null && h.getCapacityAsLong() > 0;
    }

    @Override
    public long chargeItem(Container container, int slot, long max) {
        ItemStack stack = container.getItem(slot);
        ItemAccess access = ItemAccess.forStack(stack.copy());
        EnergyHandler h = stack.getCapability(Capabilities.Energy.ITEM, access);
        int request = (int) Math.min(max, Integer.MAX_VALUE);
        if (h == null || request <= 0) {
            return 0;
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = h.insert(request, tx);
            tx.commit();
        }
        if (inserted > 0) {
            // die Kopie trägt die neuen Item-Daten; zurück in den Slot
            container.setItem(slot, access.getResource().toStack(access.getAmount()));
        }
        return inserted;
    }

    @Override
    public long testItemAmount() {
        return GameTestHooks.isGametestEnabled() ? TEST_ITEM.getAmountAsLong() : -1;
    }

    /** Test-Item für den Ladeslot (nur GameTests): eine gemeinsame Energie für alle Redstone-Blöcke. */
    private static final SimpleEnergyHandler TEST_ITEM = new SimpleEnergyHandler(Integer.MAX_VALUE);

    @SuppressWarnings("unchecked")
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends EnergyNode>> s : EXPOSED) {
            BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) (Object) s.get();
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> {
                EnergyNode node = (EnergyNode) be;
                if (!node.acceptsEnergyFrom(side) && !node.providesEnergyTo(side)) {
                    return null;
                }
                // je Seite ein Adapter: Speicher mit Ausgangsseite (Akku) nehmen dort nichts an (4.4)
                Adapter[] bySide = (Adapter[]) node.platformEnergyHandle(n -> new Adapter[Direction.values().length + 1]);
                int i = side == null ? Direction.values().length : side.ordinal();
                if (bySide[i] == null) {
                    bySide[i] = new Adapter(node, side);
                }
                return bySide[i];
            });
        }
        // Test-Verbraucher (Kapitel 27, M1): nutzt nur die NeoForge-API, nur im GameTest-Betrieb.
        // SPEC-GAP: als Capability am Vanilla-Block minecraft:target, damit kein eigener Testblock registriert wird.
        if (GameTestHooks.isGametestEnabled()) {
            event.registerBlock(Capabilities.Energy.BLOCK,
                    (level, pos, state, be, side) -> TEST_CONSUMERS.computeIfAbsent(pos.immutable(),
                            p -> new SimpleEnergyHandler(Integer.MAX_VALUE)),
                    Blocks.TARGET);
            // SPEC-GAP: Test-Item für den Ladeslot (6.3) ist minecraft:redstone_block, nur im GameTest-Betrieb
            event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> TEST_ITEM, Items.REDSTONE_BLOCK);
        }
    }

    /**
     * Bietet einen {@link SwEnergyStorage} über eine Seite als NeoForge-{@link EnergyHandler} an (Snapshots je
     * Transaktion). Grenzen pro Tick (Akku) gelten über {@link SwEnergyStorage#remainingInsert()}.
     */
    static final class Adapter extends SnapshotJournal<SwEnergyStorage.State> implements EnergyHandler {
        private final EnergyNode node;
        private final @Nullable Direction side;

        Adapter(EnergyNode node, @Nullable Direction side) {
            this.node = node;
            this.side = side;
        }

        @Override
        public long getAmountAsLong() {
            return node.energyStorage().getAmount();
        }

        @Override
        public long getCapacityAsLong() {
            return node.energyStorage().getCapacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            SwEnergyStorage s = node.energyStorage();
            if (amount <= 0 || !node.acceptsEnergyFrom(side)) {
                return 0;
            }
            long accepted = Math.min(amount, s.remainingInsert());
            if (accepted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.applyExternal(accepted);
            return (int) accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            SwEnergyStorage s = node.energyStorage();
            if (amount <= 0 || !node.providesEnergyTo(side)) {
                return 0;
            }
            long taken = Math.min(amount, s.remainingExtract());
            if (taken <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.applyExternal(-taken);
            return (int) taken;
        }

        @Override
        protected SwEnergyStorage.State createSnapshot() {
            return node.energyStorage().snapshot();
        }

        @Override
        protected void revertToSnapshot(SwEnergyStorage.State snapshot) {
            node.energyStorage().restore(snapshot);
        }

        @Override
        protected void onRootCommit(SwEnergyStorage.State originalState) {
            node.onEnergyChanged();
        }
    }
}
