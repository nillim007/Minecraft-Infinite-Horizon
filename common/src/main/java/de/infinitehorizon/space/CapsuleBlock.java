package de.infinitehorizon.space;

import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.vehicle.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Kapsel (12.4, 12.5): Rechtsklick setzt den Spieler auf einen unsichtbaren Sitz, solange ein Sitz frei ist
 * ({@code seats} aus rocket_parts.json). Einsteigen darf jeder (23.2). Absteigen mit Schleichen. SPEC-GAP #73: Sitz und
 * Kapsel-Sitzen kommen laut 27.7 mit M4e, die Checkliste in M4d braucht sie schon.
 */
public class CapsuleBlock extends Block {

    public CapsuleBlock(Properties properties) {
        super(properties);
    }

    /** Sitze der Kapsel an {@code pos}. */
    public static List<SeatEntity> seats(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(SeatEntity.class, new AABB(pos), s -> s.anchor().equals(pos));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }
        int capacity = RocketPartRegistry.get().part(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                .map(RocketPartRegistry.Part::seats).orElse(0);
        long taken = seats(level, pos).stream().filter(SeatEntity::occupied).count();
        if (taken >= capacity) {
            return InteractionResult.FAIL;
        }
        SeatEntity seat = ModEntities.SEAT.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (seat == null) {
            return InteractionResult.FAIL;
        }
        seat.setAnchor(pos);
        seat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sl.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.SUCCESS_SERVER;
    }
}
