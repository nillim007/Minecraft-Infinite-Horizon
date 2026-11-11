package de.sternenwerft.multiblock;

import com.google.gson.JsonObject;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.machine.ProcessingMachineBlockEntity;
import de.sternenwerft.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Controller-Maschine eines Multiblocks (Raffinerie, 4.3): verarbeitet nur, wenn der Aufbau vollständig ist; sonst
 * Statuslampe {@code error} und der erste fehlende Block im Tooltip. Geprüft wird beim Setzen, bei Änderungen von
 * Gehäuse/Ports in der Nähe ({@link MultiblockRegistry}) und alle {@code structure_check_interval_ticks}
 * (misc.json → {@code multiblock_rules}). Bei Erfolg werden die Ports verknüpft.
 *
 * <p>Addons: {@code new MultiblockControllerBlockEntity(TYP, pos, state, () -> List.of(KIND))} und in machines.json
 * beim Controller {@code "multiblock": "<musterEintrag>"}.
 */
public class MultiblockControllerBlockEntity extends ProcessingMachineBlockEntity implements MultiblockController {

    private boolean dirty = true;
    private boolean complete;
    private @Nullable BlockPos missing;
    private long lastCheck;
    private Set<BlockPos> parts = Set.of();
    private boolean registered;

    public MultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           Supplier<List<ProcessingRecipe.Kind>> kinds) {
        super(type, pos, state, kinds);
    }

    @Override
    public @Nullable MultiblockPattern pattern() {
        return MultiblockPattern.forMachine(BalanceManager.get(), machineId()).orElse(null);
    }

    @Override
    public boolean structureComplete() {
        return complete;
    }

    @Override
    public @Nullable BlockPos firstMissing() {
        return missing;
    }

    @Override
    public void markStructureDirty() {
        dirty = true;
    }

    @Override
    public boolean ownsPart(BlockPos pos) {
        return complete && parts.contains(pos);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(MachineBlock.FACING) ? s.getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /** Prüfabstand in Ticks aus misc.json ({@code multiblock_rules.structure_check_interval_ticks}). */
    public static long checkInterval() {
        JsonObject rules = BalanceManager.get().entry("misc", "multiblock_rules").orElse(new JsonObject());
        return rules.has("structure_check_interval_ticks") ? rules.get("structure_check_interval_ticks").getAsLong() : 1;
    }

    /** Prüft den Aufbau jetzt (auch für Tests und Werkzeuge). */
    public boolean checkStructure(ServerLevel level) {
        MultiblockPattern p = pattern();
        dirty = false;
        lastCheck = level.getGameTime();
        if (p == null) {
            complete = false;
            missing = null;
            return false;
        }
        List<MultiblockPattern.Missing> wrong = p.check(level, worldPosition, facing(), false);
        complete = wrong.isEmpty();
        missing = complete ? null : wrong.getFirst().pos();
        if (complete) {
            Set<BlockPos> set = new HashSet<>(p.partPositions(worldPosition, facing()));
            parts = set;
            for (BlockPos pos : set) {
                if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                    port.link(worldPosition);
                }
            }
        } else {
            parts = Set.of();
        }
        return complete;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        if (!registered) {
            MultiblockRegistry.register(level, worldPosition);
            registered = true;
        }
        if (dirty || level.getGameTime() - lastCheck >= checkInterval()) {
            checkStructure(level);
        }
        if (!complete) {
            setStatus(MachineStatus.ERROR);
            return;
        }
        super.serverTick(level);
    }

    @Override
    protected long syncValue(int index) {
        if (index == SYNC_MULTIBLOCK) {
            return complete ? 1 : 2;
        }
        if (index == SYNC_MISSING) {
            return missing == null ? 0 : missing.asLong();
        }
        return super.syncValue(index);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            MultiblockRegistry.unregister(level, worldPosition);
        }
    }
}
