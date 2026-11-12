package de.infinitehorizon.platform;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.api.fluid.FluidNode;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.core.fluid.FluidDef;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.platform.services.IFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** NeoForge-Fluide: FluidType + BaseFlowingFluid, Schnittstelle {@code Capabilities.Fluid.BLOCK} (mB). */
public class NeoForgeFluidHelper implements IFluidHelper {

    private static final List<Supplier<? extends BlockEntityType<? extends FluidNode>>> EXPOSED = new ArrayList<>();

    @Override
    public FluidDef.Handle registerFluid(FluidDef def) {
        String id = def.id();
        Supplier<FluidType> type = Services.REGISTRY.register(NeoForgeRegistries.FLUID_TYPES, id,
                () -> new FluidType(FluidType.Properties.create()
                        .descriptionId("block." + InfiniteHorizon.MOD_ID + "." + id)
                        .canConvertToSource(false)));
        AtomicReference<Supplier<? extends Fluid>> source = new AtomicReference<>();
        AtomicReference<Supplier<? extends Fluid>> flowing = new AtomicReference<>();
        AtomicReference<Supplier<? extends LiquidBlock>> block = new AtomicReference<>();
        AtomicReference<Supplier<? extends Item>> bucket = new AtomicReference<>();
        Supplier<BaseFlowingFluid.Properties> props = () -> {
            BaseFlowingFluid.Properties p = new BaseFlowingFluid.Properties(type, () -> source.get().get(), () -> flowing.get().get())
                    .slopeFindDistance(def.slopeFindDistance())
                    .levelDecreasePerBlock(def.dropOff())
                    .tickRate(def.tickDelay())
                    .explosionResistance(def.explosionResistance());
            if (block.get() != null) {
                p.block(() -> block.get().get());
            }
            if (bucket.get() != null) {
                p.bucket(() -> bucket.get().get());
            }
            return p;
        };
        source.set(Services.REGISTRY.register(BuiltInRegistries.FLUID, id, () -> new BaseFlowingFluid.Source(props.get())));
        flowing.set(Services.REGISTRY.register(BuiltInRegistries.FLUID, id + "_flowing", () -> new BaseFlowingFluid.Flowing(props.get())));
        if (def.sourceBlock()) {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, InfiniteHorizon.id(id));
            block.set(Services.REGISTRY.register(BuiltInRegistries.BLOCK, id,
                    () -> new LiquidBlock((FlowingFluid) source.get().get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(key))));
        }
        if (def.bucket() != null) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, InfiniteHorizon.id(def.bucket()));
            bucket.set(Services.REGISTRY.register(BuiltInRegistries.ITEM, def.bucket(),
                    () -> new BucketItem(source.get().get(), new Item.Properties().setId(key).craftRemainder(Items.BUCKET).stacksTo(1))));
        }
        return new FluidDef.Handle(source.get(), flowing.get(), block.get(), bucket.get());
    }

    @Override
    public @Nullable FluidTarget findTarget(Level level, BlockPos pos, Direction side) {
        ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        if (handler == null) {
            return null;
        }
        return (offer, simulate) -> {
            Fluid fluid = fluidOf(offer.fluid());
            int amount = (int) Math.min(offer.amount(), Integer.MAX_VALUE);
            if (fluid == null || amount <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(FluidResource.of(fluid), amount, tx);
                if (!simulate && inserted > 0) {
                    tx.commit();
                }
                return inserted;
            }
        };
    }

    @Override
    public void expose(Supplier<? extends BlockEntityType<? extends FluidNode>> type) {
        EXPOSED.add(type);
    }

    @SuppressWarnings("unchecked")
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Supplier<? extends BlockEntityType<? extends FluidNode>> s : EXPOSED) {
            BlockEntityType<BlockEntity> t = (BlockEntityType<BlockEntity>) (Object) s.get();
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, t, (be, side) -> {
                FluidNode node = (FluidNode) be;
                if (node.fluidInput(side) == null && !node.providesFluidTo(side)) {
                    return null;
                }
                Adapter[] bySide = (Adapter[]) node.platformFluidHandle(n -> new Adapter[Direction.values().length + 1]);
                int idx = side == null ? Direction.values().length : side.ordinal();
                if (bySide[idx] == null) {
                    bySide[idx] = new Adapter(node, side);
                }
                return bySide[idx];
            });
        }
    }

    static @Nullable Fluid fluidOf(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        Fluid f = BuiltInRegistries.FLUID.getValue(id);
        return f == null || f == Fluids.EMPTY ? null : f;
    }

    /** Bietet die Tanks eines {@link FluidNode} als {@code ResourceHandler<FluidResource>} an. */
    static final class Adapter extends SnapshotJournal<List<IhFluidStack>> implements ResourceHandler<FluidResource> {
        private final FluidNode node;
        private final @Nullable Direction side;

        Adapter(FluidNode node, @Nullable Direction side) {
            this.node = node;
            this.side = side;
        }

        private @Nullable IhFluidTank tank(int index) {
            List<IhFluidTank> tanks = node.fluidTanks();
            return index >= 0 && index < tanks.size() ? tanks.get(index) : null;
        }

        @Override
        public int size() {
            return node.fluidTanks().size();
        }

        @Override
        public FluidResource getResource(int index) {
            IhFluidTank t = tank(index);
            Fluid f = t == null ? null : fluidOf(t.stack().fluid());
            return f == null ? FluidResource.EMPTY : FluidResource.of(f);
        }

        @Override
        public long getAmountAsLong(int index) {
            IhFluidTank t = tank(index);
            return t == null ? 0 : t.stack().amount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            IhFluidTank t = tank(index);
            return t == null ? 0 : t.capacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            IhFluidTank t = tank(index);
            return t != null && !resource.isEmpty() && t.accepts(BuiltInRegistries.FLUID.getKey(resource.getFluid()));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            FluidTarget target = node.fluidInput(side);
            if (target == null || resource.isEmpty() || amount <= 0) {
                return 0;
            }
            IhFluidStack offer = IhFluidStack.of(BuiltInRegistries.FLUID.getKey(resource.getFluid()), amount);
            long can = target.insert(offer, true);
            if (can <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            return (int) target.insert(offer.withAmount(can), false);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !node.providesFluidTo(side)) {
                return 0;
            }
            Identifier id = BuiltInRegistries.FLUID.getKey(resource.getFluid());
            IhFluidStack can = node.extractFluid(side, id, amount, true);
            if (can.isEmpty()) {
                return 0;
            }
            updateSnapshots(transaction);
            return (int) node.extractFluid(side, id, can.amount(), false).amount();
        }

        @Override
        protected List<IhFluidStack> createSnapshot() {
            List<IhFluidStack> s = new ArrayList<>();
            for (IhFluidTank t : node.fluidTanks()) {
                s.add(t.stack());
            }
            return s;
        }

        @Override
        protected void revertToSnapshot(List<IhFluidStack> snapshot) {
            List<IhFluidTank> tanks = node.fluidTanks();
            for (int i = 0; i < tanks.size() && i < snapshot.size(); i++) {
                tanks.get(i).setStackUnchecked(snapshot.get(i));
            }
        }

        @Override
        protected void onRootCommit(List<IhFluidStack> originalState) {
            node.onFluidChanged();
        }
    }
}
