package de.infinitehorizon.space;

import de.infinitehorizon.block.IhBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Station des {@code cloud_elevator} (12.8): oben oder unten, mit der Position der verknüpften Station. */
public class CloudElevatorBlockEntity extends IhBlockEntity {

    private boolean lower;
    private @Nullable BlockPos link;

    public CloudElevatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean lower() {
        return lower;
    }

    public @Nullable BlockPos link() {
        return link;
    }

    public void setLink(@Nullable BlockPos link, boolean lower) {
        this.link = link == null ? null : link.immutable();
        this.lower = lower;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("lower", lower);
        if (link != null) {
            output.putLong("link", link.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lower = input.getBooleanOr("lower", false);
        link = input.getLong("link").map(BlockPos::of).orElse(null);
    }
}
