package de.sternenwerft.multiblock;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.fluid.FluidNode;
import de.sternenwerft.api.fluid.FluidTarget;
import de.sternenwerft.core.energy.SwEnergyStorage;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.core.fluid.SwFluidTank;
import de.sternenwerft.core.transaction.Transaction;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Block-Entity der Ports (4.3). Je nach {@link PortBlock.PortType} leitet sie Items (Trichter), Fluide (Rohre,
 * Loader-API) oder Energie (Kabel, Loader-API) an den verknüpften Controller weiter:
 * <ul>
 *   <li>{@code input}: nimmt an und gibt in die Eingänge des Controllers.</li>
 *   <li>{@code output}: gibt nur Ausgaben ab; Fluide und Energie schiebt der Port selbst in Nachbarn außerhalb des
 *   Aufbaus (Push-Prinzip 4.4), Items ziehen Trichter heraus.</li>
 * </ul>
 * Ohne gültigen Controller (Aufbau unvollständig) tut der Port nichts.
 */
public class PortBlockEntity extends BlockEntity implements EnergyNode, FluidNode, WorldlyContainer {

    private static final int[] NO_SLOTS = new int[0];

    private final SwEnergyStorage none = new SwEnergyStorage(0, 0, 0, () -> { });
    private @Nullable BlockPos controllerPos;
    private @Nullable Object platformEnergy;
    private @Nullable Object platformFluid;

    public PortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Vom Controller nach erfolgreicher Prüfung gesetzt. */
    public void link(BlockPos controller) {
        controllerPos = controller.immutable();
    }

    public PortBlock.PortType portType() {
        return getBlockState().getBlock() instanceof PortBlock b ? b.portType() : PortBlock.PortType.ITEM;
    }

    public PortBlock.Mode mode() {
        BlockState s = getBlockState();
        return s.hasProperty(PortBlock.MODE) ? s.getValue(PortBlock.MODE) : PortBlock.Mode.INPUT;
    }

    private boolean is(PortBlock.PortType type, PortBlock.Mode mode) {
        return portType() == type && mode() == mode;
    }

    /** Der verknüpfte Controller, wenn sein Aufbau vollständig ist und diesen Port enthält; sonst null. */
    public @Nullable MachineBlockEntity controller() {
        if (controllerPos == null || level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof MultiblockController c && be instanceof MachineBlockEntity m && c.ownsPart(worldPosition)) {
            return m;
        }
        return null;
    }

    /** Liegt {@code pos} im Aufbau (Gehäuse, Port oder Controller)? Dorthin schiebt der Port nicht. */
    private boolean inStructure(BlockPos pos) {
        return pos.equals(controllerPos) || (level != null && controllerPos != null
                && level.getBlockEntity(controllerPos) instanceof MultiblockController c && c.ownsPart(pos));
    }

    /** Ausgabe-Ports schieben Fluide und Energie des Controllers in Nachbarn außerhalb des Aufbaus. */
    public void serverTick(ServerLevel level) {
        if (mode() != PortBlock.Mode.OUTPUT) {
            return;
        }
        MachineBlockEntity c = controller();
        if (c == null) {
            return;
        }
        if (portType() == PortBlock.PortType.FLUID) {
            for (SwFluidTank tank : c.fluidTanks()) {
                SwFluidStack offer = c.extractFluid(null, tank.stack().fluid(), tank.stack().amount(), true);
                for (Direction d : Direction.values()) {
                    if (offer.isEmpty() || inStructure(worldPosition.relative(d))) {
                        continue;
                    }
                    long moved = MachineBlockEntity.pushFluidTo(level, worldPosition, d, offer);
                    if (moved > 0) {
                        c.extractFluid(null, offer.fluid(), moved, false);
                        offer = offer.withAmount(offer.amount() - moved);
                    }
                }
            }
        } else if (portType() == PortBlock.PortType.ENERGY && c.providesEnergyTo(null)) {
            SwEnergyStorage s = c.energyStorage();
            for (Direction d : Direction.values()) {
                long offer = Math.min(s.getAmount(), s.getMaxExtract());
                if (offer <= 0 || inStructure(worldPosition.relative(d))) {
                    continue;
                }
                long moved = MachineBlockEntity.pushEnergyTo(level, worldPosition, d, offer);
                if (moved > 0) {
                    try (Transaction tx = Transaction.open()) {
                        s.extract(moved, tx);
                        tx.commit();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Energie

    @Override
    public SwEnergyStorage energyStorage() {
        MachineBlockEntity c = portType() == PortBlock.PortType.ENERGY ? controller() : null;
        return c == null ? none : c.energyStorage();
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return is(PortBlock.PortType.ENERGY, PortBlock.Mode.INPUT);
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return is(PortBlock.PortType.ENERGY, PortBlock.Mode.OUTPUT);
    }

    @Override
    public void onEnergyChanged() {
        MachineBlockEntity c = controller();
        if (c != null) {
            c.onEnergyChanged();
        }
    }

    @Override
    public Object platformEnergyHandle(Function<EnergyNode, Object> factory) {
        if (platformEnergy == null) {
            platformEnergy = factory.apply(this);
        }
        return platformEnergy;
    }

    // ------------------------------------------------------------------ Fluide

    @Override
    public List<SwFluidTank> fluidTanks() {
        MachineBlockEntity c = portType() == PortBlock.PortType.FLUID ? controller() : null;
        return c == null ? List.of() : c.fluidTanks();
    }

    @Override
    public @Nullable FluidTarget fluidInput(@Nullable Direction side) {
        if (!is(PortBlock.PortType.FLUID, PortBlock.Mode.INPUT)) {
            return null;
        }
        return (offer, simulate) -> {
            MachineBlockEntity c = controller();
            FluidTarget target = c == null ? null : c.fluidInput(null);
            return target == null ? 0 : target.insert(offer, simulate);
        };
    }

    @Override
    public SwFluidStack extractFluid(@Nullable Direction side, @Nullable Identifier fluid, long max, boolean simulate) {
        MachineBlockEntity c = is(PortBlock.PortType.FLUID, PortBlock.Mode.OUTPUT) ? controller() : null;
        return c == null ? SwFluidStack.EMPTY : c.extractFluid(null, fluid, max, simulate);
    }

    @Override
    public boolean providesFluidTo(@Nullable Direction side) {
        return is(PortBlock.PortType.FLUID, PortBlock.Mode.OUTPUT);
    }

    @Override
    public void onFluidChanged() {
        MachineBlockEntity c = controller();
        if (c != null) {
            c.onFluidChanged();
        }
    }

    @Override
    public Object platformFluidHandle(Function<FluidNode, Object> factory) {
        if (platformFluid == null) {
            platformFluid = factory.apply(this);
        }
        return platformFluid;
    }

    // ------------------------------------------------------------------ Items (Trichter)

    private @Nullable MachineBlockEntity itemController() {
        return portType() == PortBlock.PortType.ITEM ? controller() : null;
    }

    @Override
    // SPEC-GAP: angebotene Slots je Modus (spec-gaps #29)
    public int[] getSlotsForFace(Direction side) {
        MachineBlockEntity c = itemController();
        if (c == null) {
            return NO_SLOTS;
        }
        MachineLayout l = c.layout();
        boolean output = mode() == PortBlock.Mode.OUTPUT;
        return java.util.stream.IntStream.range(0, Math.min(l.total(), c.getContainerSize())).filter(i -> switch (l.role(i)) {
            case OUTPUT -> output;
            case FILL, DRAIN -> true;
            case UPGRADE, NONE -> false;
            default -> !output;
        }).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        MachineBlockEntity c = itemController();
        return c != null && mode() == PortBlock.Mode.INPUT && c.canPlaceItemThroughFace(slot, stack, null);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        MachineBlockEntity c = itemController();
        return c != null && mode() == PortBlock.Mode.OUTPUT && c.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        MachineBlockEntity c = itemController();
        return c != null && mode() == PortBlock.Mode.INPUT && c.canPlaceItem(slot, stack);
    }

    @Override
    public int getContainerSize() {
        MachineBlockEntity c = itemController();
        return c == null ? 0 : c.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        MachineBlockEntity c = itemController();
        return c == null || c.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        MachineBlockEntity c = itemController();
        return c == null ? ItemStack.EMPTY : c.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        MachineBlockEntity c = itemController();
        return c == null ? ItemStack.EMPTY : c.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        MachineBlockEntity c = itemController();
        return c == null ? ItemStack.EMPTY : c.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        MachineBlockEntity c = itemController();
        if (c != null) {
            c.setItem(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        MachineBlockEntity c = level == null ? null : itemController();
        if (c != null) {
            c.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        // der Inhalt gehört dem Controller
    }
}
