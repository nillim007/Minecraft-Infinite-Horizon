package de.sternenwerft.platform;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.platform.services.IEnergyHelper;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Fabric-Energie über Team Reborn Energy ({@code EnergyStorage.SIDED}). */
public class FabricEnergyHelper implements IEnergyHelper {

    private static final Map<BlockPos, SimpleEnergyStorage> TEST_CONSUMERS = new ConcurrentHashMap<>();

    @Override
    public @Nullable EnergyTarget findTarget(Level level, BlockPos pos, Direction side) {
        EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
        if (storage == null || !storage.supportsInsertion()) {
            return null;
        }
        return (amount, simulate) -> {
            if (amount <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = storage.insert(amount, tx);
                if (!simulate) {
                    tx.commit();
                }
                return inserted;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void expose(Supplier<? extends BlockEntityType<? extends EnergyNode>> type) {
        BlockEntityType<BlockEntity> t = (BlockEntityType<BlockEntity>) (Object) type.get();
        EnergyStorage.SIDED.registerForBlockEntity((be, side) -> {
            EnergyNode node = (EnergyNode) be;
            if (!node.acceptsEnergyFrom(side) && !node.providesEnergyTo(side)) {
                return null;
            }
            return (EnergyStorage) node.platformEnergyHandle(Adapter::new);
        }, t);
    }

    @Override
    public long testConsumerAmount(Level level, BlockPos pos) {
        SimpleEnergyStorage s = TEST_CONSUMERS.get(pos);
        return s == null ? -1 : s.amount;
    }

    /**
     * Test-Verbraucher (Kapitel 27, M1): nutzt nur Team Reborn Energy, nur bei {@code -Dfabric-api.gametest}.
     * SPEC-GAP: als API-Anbieter am Vanilla-Block minecraft:target, damit kein eigener Testblock registriert wird.
     */
    public static void registerTestConsumers() {
        if (System.getProperty("fabric-api.gametest") == null) {
            return;
        }
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, be, side) ->
                TEST_CONSUMERS.computeIfAbsent(pos.immutable(),
                        p -> new SimpleEnergyStorage(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, 0)), Blocks.TARGET);
    }

    /** Bietet einen {@link SwEnergyStorage} als Team-Reborn-{@link EnergyStorage} an. */
    static final class Adapter extends SnapshotParticipant<Long> implements EnergyStorage {
        private final EnergyNode node;

        Adapter(EnergyNode node) {
            this.node = node;
        }

        @Override
        public boolean supportsInsertion() {
            return node.energyStorage().getMaxInsert() > 0;
        }

        @Override
        public boolean supportsExtraction() {
            return node.energyStorage().getMaxExtract() > 0;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            SwEnergyStorage s = node.energyStorage();
            if (maxAmount <= 0 || !node.acceptsEnergyFrom(null)) {
                return 0;
            }
            long accepted = Math.min(Math.min(maxAmount, s.getMaxInsert()), s.getCapacity() - s.getAmount());
            if (accepted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.setAmountUnchecked(s.getAmount() + accepted);
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            SwEnergyStorage s = node.energyStorage();
            if (maxAmount <= 0 || !node.providesEnergyTo(null)) {
                return 0;
            }
            long taken = Math.min(Math.min(maxAmount, s.getMaxExtract()), s.getAmount());
            if (taken <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.setAmountUnchecked(s.getAmount() - taken);
            return taken;
        }

        @Override
        public long getAmount() {
            return node.energyStorage().getAmount();
        }

        @Override
        public long getCapacity() {
            return node.energyStorage().getCapacity();
        }

        @Override
        protected Long createSnapshot() {
            return node.energyStorage().getAmount();
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            node.energyStorage().setAmountUnchecked(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            node.onEnergyChanged();
        }
    }
}
