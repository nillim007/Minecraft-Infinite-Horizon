package de.infinitehorizon.station;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** {@code clone_chamber} (14.4): Rechtsklick bindet den Spieler an die Kammer und öffnet das GUI (Kulturen-Slot). */
public class CloneChamberBlock extends MachineBlock {

    public CloneChamberBlock(Properties properties, Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties, type);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            GlobalPos here = GlobalPos.of(sl.dimension(), pos);
            CloneChambers data = CloneChambers.get(sl.getServer());
            if (!here.equals(data.binding(player.getUUID()))) {
                data.bind(player.getUUID(), here);
                player.sendSystemMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".clone.bound"));
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
