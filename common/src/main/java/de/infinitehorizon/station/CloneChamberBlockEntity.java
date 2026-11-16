package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * {@code clone_chamber} (6.1, 14.4): braucht dauernd Strom ({@code energy_per_tick}) und hält {@code microbe_culture} im
 * Eingabeslot. Meldet Strom und Kulturen an {@link CloneChambers}; dort beim Respawn verbrauchte Kulturen zieht sie hier ab.
 */
public class CloneChamberBlockEntity extends ProcessingMachineBlockEntity {

    public CloneChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    /** Verbrauchsgut je Respawn ({@code consumes_per_respawn.item}, stations.json). */
    public static Item culture() {
        JsonObject c = BalanceManager.getOrBundled().entry("stations", InfiniteHorizon.id("clone_chamber").toString())
                .orElse(new JsonObject());
        JsonObject per = c.has("consumes_per_respawn") ? c.getAsJsonObject("consumes_per_respawn") : new JsonObject();
        return per.has("item") ? BuiltInRegistries.ITEM.getValue(Identifier.parse(per.get("item").getAsString()))
                : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return stack.is(culture());
    }

    public GlobalPos globalPos() {
        return GlobalPos.of(level.dimension(), getBlockPos());
    }

    @Override
    protected void process(ServerLevel level) {
        boolean powered = energy.useInternal(stats().energyPerTick());
        ItemStack slot = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        int cultures = slot.is(culture()) ? slot.getCount() : 0;
        int take = CloneChambers.get(level.getServer()).report(globalPos(), powered, cultures);
        if (take > 0 && !slot.isEmpty()) {
            slot.shrink(take);
            setChanged();
        }
        setStatus(powered ? MachineStatus.RUNNING : MachineStatus.IDLE);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel sl) {
            CloneChambers.get(sl.getServer()).remove(GlobalPos.of(sl.dimension(), pos));
        }
        super.preRemoveSideEffects(pos, state);
    }
}
