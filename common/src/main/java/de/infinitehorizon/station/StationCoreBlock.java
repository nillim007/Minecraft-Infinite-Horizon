package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Stationskern (14.1): beim Setzen entsteht eine Plattform aus {@code station_hull} mit {@code generated=true}. */
public class StationCoreBlock extends MachineBlock {

    public StationCoreBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            buildPlatform(sl, pos);
            StationData.get(sl).put(pos, null);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        register(level, pos);
    }

    /** Kern mit Besitzer in {@link StationData} eintragen. */
    public static void register(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof IhBlockEntity be) {
            StationData.get(sl).put(pos, be.owner());
        }
    }

    /** 5×5-Plattform um den Kern, nur an Luftpositionen (14.1). */
    public static void buildPlatform(ServerLevel level, BlockPos core) {
        JsonObject p = StationCoreBlockEntity.config().has("platform") ? StationCoreBlockEntity.config().getAsJsonObject("platform")
                : new JsonObject();
        int r = (p.has("size") ? p.get("size").getAsInt() : 0) / 2;
        BlockState hull = ModBlocks.STATION_HULL.get().defaultBlockState().setValue(StationHullBlock.GENERATED, true);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos at = core.offset(dx, 0, dz);
                if ((dx != 0 || dz != 0) && level.getBlockState(at).isAir()) {
                    level.setBlockAndUpdate(at, hull);
                }
            }
        }
    }
}
