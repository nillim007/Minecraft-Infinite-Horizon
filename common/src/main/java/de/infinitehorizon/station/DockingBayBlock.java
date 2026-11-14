package de.infinitehorizon.station;

import de.infinitehorizon.space.LandingBeaconBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Andockbucht (14.2): wie ein Landefeuer benennbar und Landeziel; tankt gelandete Raketen (Ticker auf dem Server). */
public class DockingBayBlock extends LandingBeaconBlock {

    public DockingBayBlock(Properties properties, Supplier<? extends BlockEntityType<? extends DockingBayBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l, pos, st, be) -> {
            if (be instanceof DockingBayBlockEntity bay && l instanceof ServerLevel sl) {
                bay.serverTick(sl);
            }
        };
    }
}
