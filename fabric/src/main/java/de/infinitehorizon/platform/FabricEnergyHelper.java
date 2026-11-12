package de.infinitehorizon.platform;

import de.infinitehorizon.api.energy.EnergyNode;
import de.infinitehorizon.api.energy.EnergyTarget;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.platform.services.IEnergyHelper;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            // je Seite ein Adapter: Speicher mit Ausgangsseite (Akku) nehmen dort nichts an (4.4)
            Adapter[] bySide = (Adapter[]) node.platformEnergyHandle(n -> new Adapter[Direction.values().length + 1]);
            int i = side == null ? Direction.values().length : side.ordinal();
            if (bySide[i] == null) {
                bySide[i] = new Adapter(node, side);
            }
            return bySide[i];
        }, t);
    }

    @Override
    public long testConsumerAmount(Level level, BlockPos pos) {
        SimpleEnergyStorage s = TEST_CONSUMERS.get(pos);
        return s == null ? -1 : s.amount;
    }

    @Override
    public boolean isPlatformChargeable(ItemStack stack) {
        EnergyStorage s = EnergyStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        return s != null && s.supportsInsertion();
    }

    @Override
    public long chargeItem(Container container, int slot, long max) {
        ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(ContainerStorage.of(container, null).getSlot(slot));
        EnergyStorage s = EnergyStorage.ITEM.find(container.getItem(slot), ctx);
        if (s == null || max <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = s.insert(max, tx);
            tx.commit();
            return inserted;
        }
    }

    @Override
    public long testItemAmount() {
        return System.getProperty("fabric-api.gametest") == null ? -1 : TEST_ITEM.amount;
    }

    /** Test-Item für den Ladeslot (nur GameTests): eine gemeinsame Energie für alle Redstone-Blöcke. */
    private static final SimpleEnergyStorage TEST_ITEM = new SimpleEnergyStorage(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2, 0);

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
        // SPEC-GAP: Test-Item für den Ladeslot (6.3) ist minecraft:redstone_block, nur im GameTest-Betrieb
        EnergyStorage.ITEM.registerForItems((stack, ctx) -> TEST_ITEM, Items.REDSTONE_BLOCK);
    }

    /**
     * Bietet einen {@link IhEnergyStorage} über eine Seite als Team-Reborn-{@link EnergyStorage} an. Grenzen pro Tick
     * (Akku) gelten über {@link IhEnergyStorage#remainingInsert()}.
     */
    static final class Adapter extends SnapshotParticipant<IhEnergyStorage.State> implements EnergyStorage {
        private final EnergyNode node;
        private final @Nullable Direction side;

        Adapter(EnergyNode node, @Nullable Direction side) {
            this.node = node;
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() {
            return node.acceptsEnergyFrom(side) && node.energyStorage().getMaxInsert() > 0;
        }

        @Override
        public boolean supportsExtraction() {
            return node.providesEnergyTo(side) && node.energyStorage().getMaxExtract() > 0;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            IhEnergyStorage s = node.energyStorage();
            if (maxAmount <= 0 || !node.acceptsEnergyFrom(side)) {
                return 0;
            }
            long accepted = Math.min(maxAmount, s.remainingInsert());
            if (accepted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.applyExternal(accepted);
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            IhEnergyStorage s = node.energyStorage();
            if (maxAmount <= 0 || !node.providesEnergyTo(side)) {
                return 0;
            }
            long taken = Math.min(maxAmount, s.remainingExtract());
            if (taken <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            s.applyExternal(-taken);
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
        protected IhEnergyStorage.State createSnapshot() {
            return node.energyStorage().snapshot();
        }

        @Override
        protected void readSnapshot(IhEnergyStorage.State snapshot) {
            node.energyStorage().restore(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            node.onEnergyChanged();
        }
    }
}
