package de.sternenwerft.platform;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.api.fluid.FluidNode;
import de.sternenwerft.api.fluid.FluidTarget;
import de.sternenwerft.core.fluid.FluidDef;
import de.sternenwerft.core.fluid.SwFlowingFluid;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.platform.services.IFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/** Fabric-Fluide: eigene {@link SwFlowingFluid}-Klassen, Schnittstelle {@code FluidStorage.SIDED} (Droplets). */
public class FabricFluidHelper implements IFluidHelper {

    /** 1 mB = 81 Droplets (FluidConstants.BUCKET = 81000). */
    private static final long DROPLETS_PER_MB = 81;

    @Override
    public FluidDef.Handle registerFluid(FluidDef def) {
        SwFlowingFluid.Parts parts = new SwFlowingFluid.Parts(def);
        SwFlowingFluid.Source source = Registry.register(BuiltInRegistries.FLUID, Sternenwerft.id(def.id()), new SwFlowingFluid.Source(parts));
        SwFlowingFluid.Flowing flowing = Registry.register(BuiltInRegistries.FLUID, Sternenwerft.id(def.id() + "_flowing"),
                new SwFlowingFluid.Flowing(parts));
        parts.source = () -> source;
        parts.flowing = () -> flowing;
        if (def.sourceBlock()) {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Sternenwerft.id(def.id()));
            LiquidBlock block = Registry.register(BuiltInRegistries.BLOCK, key,
                    new LiquidBlock(source, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(key)) { });
            parts.block = () -> block;
        }
        if (def.bucket() != null) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Sternenwerft.id(def.bucket()));
            BucketItem bucket = Registry.register(BuiltInRegistries.ITEM, key,
                    new BucketItem(source, new Item.Properties().setId(key).craftRemainder(Items.BUCKET).stacksTo(1)));
            parts.bucket = () -> bucket;
        }
        return new FluidDef.Handle(parts.source, parts.flowing, parts.block, parts.bucket);
    }

    @Override
    public @Nullable FluidTarget findTarget(Level level, BlockPos pos, Direction side) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        if (storage == null || !storage.supportsInsertion()) {
            return null;
        }
        return (offer, simulate) -> {
            Fluid fluid = fluidOf(offer.fluid());
            if (fluid == null || offer.amount() <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long mb = storage.insert(FluidVariant.of(fluid), offer.amount() * DROPLETS_PER_MB, tx) / DROPLETS_PER_MB;
                if (!simulate && mb > 0) {
                    tx.commit();
                }
                return mb;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void expose(Supplier<? extends BlockEntityType<? extends FluidNode>> type) {
        BlockEntityType<BlockEntity> t = (BlockEntityType<BlockEntity>) (Object) type.get();
        FluidStorage.SIDED.registerForBlockEntity((be, side) -> {
            FluidNode node = (FluidNode) be;
            Adapter[] bySide = (Adapter[]) node.platformFluidHandle(n -> new Adapter[Direction.values().length + 1]);
            int idx = side == null ? Direction.values().length : side.ordinal();
            if (bySide[idx] == null) {
                bySide[idx] = new Adapter(node, side);
            }
            return bySide[idx];
        }, t);
    }

    static @Nullable Fluid fluidOf(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        Fluid f = BuiltInRegistries.FLUID.getValue(id);
        return f == null || f == Fluids.EMPTY ? null : f;
    }

    /** Bietet die Tanks eines {@link FluidNode} als {@code Storage<FluidVariant>} an (Snapshots je Transaktion). */
    static final class Adapter extends SnapshotParticipant<List<SwFluidStack>> implements Storage<FluidVariant> {
        private final FluidNode node;
        private final @Nullable Direction side;

        Adapter(FluidNode node, @Nullable Direction side) {
            this.node = node;
            this.side = side;
        }

        @Override
        public boolean supportsInsertion() {
            return node.fluidInput(side) != null;
        }

        @Override
        public boolean supportsExtraction() {
            return node.providesFluidTo(side);
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            FluidTarget target = node.fluidInput(side);
            long mb = maxAmount / DROPLETS_PER_MB;
            if (target == null || resource.isBlank() || mb <= 0) {
                return 0;
            }
            SwFluidStack offer = SwFluidStack.of(BuiltInRegistries.FLUID.getKey(resource.getFluid()), mb);
            long can = target.insert(offer, true);
            if (can <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            return target.insert(offer.withAmount(can), false) * DROPLETS_PER_MB;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            long mb = maxAmount / DROPLETS_PER_MB;
            if (resource.isBlank() || mb <= 0 || !node.providesFluidTo(side)) {
                return 0;
            }
            Identifier id = BuiltInRegistries.FLUID.getKey(resource.getFluid());
            SwFluidStack can = node.extractFluid(side, id, mb, true);
            if (can.isEmpty()) {
                return 0;
            }
            updateSnapshots(transaction);
            return node.extractFluid(side, id, can.amount(), false).amount() * DROPLETS_PER_MB;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (SwFluidTank tank : node.fluidTanks()) {
                views.add(new View(this, tank));
            }
            return views.iterator();
        }

        @Override
        protected List<SwFluidStack> createSnapshot() {
            List<SwFluidStack> s = new ArrayList<>();
            for (SwFluidTank t : node.fluidTanks()) {
                s.add(t.stack());
            }
            return s;
        }

        @Override
        protected void readSnapshot(List<SwFluidStack> snapshot) {
            List<SwFluidTank> tanks = node.fluidTanks();
            for (int i = 0; i < tanks.size() && i < snapshot.size(); i++) {
                tanks.get(i).setStackUnchecked(snapshot.get(i));
            }
        }

        @Override
        protected void onFinalCommit() {
            node.onFluidChanged();
        }
    }

    record View(Adapter adapter, SwFluidTank tank) implements StorageView<FluidVariant> {
        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return adapter.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return tank.stack().isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            Fluid f = fluidOf(tank.stack().fluid());
            return f == null ? FluidVariant.blank() : FluidVariant.of(f);
        }

        @Override
        public long getAmount() {
            return tank.stack().amount() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return tank.capacity() * DROPLETS_PER_MB;
        }
    }
}
