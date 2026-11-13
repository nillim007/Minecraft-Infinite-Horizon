package de.infinitehorizon.vehicle;

import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** {@code rover_kit} (16): Rechtsklick auf den Boden stellt den Rover mit den Daten des Bausatzes auf. */
public class RoverKitItem extends Item {

    public RoverKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        RoverEntity rover = ModEntities.ROVER.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (rover == null) {
            return InteractionResult.FAIL;
        }
        RoverData data = context.getItemInHand().getOrDefault(ModDataComponents.ROVER_DATA.get(), RoverData.EMPTY);
        rover.load(data, null);
        if (context.getPlayer() instanceof ServerPlayer sp) {
            rover.claim(sp);
            rover.setYRot(sp.getYRot());
        }
        rover.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        if (!level.noCollision(rover, rover.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(rover);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }
}
