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

    @SuppressWarnings("unchecked")
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends EnergyNode>> s : EXPOSED) {
            BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) (Object) s.get();
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> {
                EnergyNode node = (EnergyNode) be;
                if (!node.acceptsEnergyFrom(side) && !node.providesEnergyTo(side)) {
                    return null;
                }
                return (EnergyHandler) node.platformEnergyHandle(Adapter::new);
            });
        }
        // Test-Verbraucher (Kapitel 27, M1): nutzt nur die NeoForge-API, nur im GameTest-Betrieb.
        // SPEC-GAP: als Capability am Vanilla-Block minecraft:target, damit kein eigener Testblock registriert wird.
        if (GameTestHooks.isGametestEnabled()) {
            event.registerBlock(Capabilities.Energy.BLOCK,
                    (level, pos, state, be, side) -> TEST_CONSUMERS.computeIfAbsent(pos.immutable(),
                            p -> new SimpleEnergyHandler(Integer.MAX_VALUE)),
                    Blocks.TARGET);
        }
    }

    /** Bietet einen {@link SwEnergyStorage} als NeoForge-{@link EnergyHandler} an (Snapshots je Transaktion). */
    static final class Adapter extends SnapshotJournal<Long> implements EnergyHandler {
        private final EnergyNode node;

        Adapter(EnergyNode node) {
            this.node = node;
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
            if (amount <= 0 || !node.acceptsEnergyFrom(null)) {
                return 0;
            }
            long accepted = Math.min(Math.min(amount, s.getMaxInsert()), s.getCapacity() - s.getAmount());
            if (accepted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.setAmountUnchecked(s.getAmount() + accepted);
            return (int) accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            SwEnergyStorage s = node.energyStorage();
            if (amount <= 0 || !node.providesEnergyTo(null)) {
                return 0;
            }
            long taken = Math.min(Math.min(amount, s.getMaxExtract()), s.getAmount());
            if (taken <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.setAmountUnchecked(s.getAmount() - taken);
            return (int) taken;
        }

        @Override
        protected Long createSnapshot() {
            return node.energyStorage().getAmount();
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            node.energyStorage().setAmountUnchecked(snapshot);
        }

        @Override
        protected void onRootCommit(Long originalState) {
            node.onEnergyChanged();
        }
    }
}
