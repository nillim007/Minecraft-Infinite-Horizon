package de.infinitehorizon.station;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.OwnedBlock;
import de.infinitehorizon.machine.MachineLocation;
import de.infinitehorizon.space.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Stationskern als Item (14.1): nur in Orbit-Dimensionen. Rechtsklick ohne Zielblock setzt den Kern
 * {@code air_placement_distance} Blöcke vor den Spieler; aus der Kapsel einer Rakete direkt unter die Rakete (12.5, 7.).
 */
public class StationCoreItem extends BlockItem {

    public StationCoreItem(Block block, Properties properties) {
        super(block, properties);
    }

    private static boolean orbit(Level level, BlockPos pos) {
        return level instanceof ServerLevel sl && MachineLocation.allowed(StationCoreBlockEntity.config(), sl, pos);
    }

    private static void deny(@Nullable Player player) {
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".station_core.orbit_only"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide() && !orbit(ctx.getLevel(), ctx.getClickedPos())) {
            deny(ctx.getPlayer());
            return InteractionResult.FAIL;
        }
        return super.useOn(ctx);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockPos pos = airTarget(player);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!orbit(level, pos)) {
            deny(player);
            return InteractionResult.FAIL;
        }
        return placeInAir(level, pos, player, hand) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    /** Zielposition ohne Zielblock: unter der Rakete, in der der Spieler sitzt, sonst vor dem Spieler. */
    public static BlockPos airTarget(Player player) {
        if (player.getVehicle() instanceof RocketEntity rocket) {
            return rocket.blockPosition().below();
        }
        int d = StationCoreBlockEntity.config().has("air_placement_distance")
                ? StationCoreBlockEntity.config().get("air_placement_distance").getAsInt() : 0;
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(d));
        return BlockPos.containing(at);
    }

    /** Setzt Kern und Plattform an einer Luftposition; Besitz wie beim normalen Setzen. */
    public boolean placeInAir(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockState state = getBlock().defaultBlockState();
        if (state.hasProperty(de.infinitehorizon.machine.MachineBlock.FACING)) {
            state = state.setValue(de.infinitehorizon.machine.MachineBlock.FACING, player.getDirection().getOpposite());
        }
        level.setBlockAndUpdate(pos, state);
        OwnedBlock.claim(level, pos, player);
        StationCoreBlock.register(level, pos);
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        return true;
    }
}
