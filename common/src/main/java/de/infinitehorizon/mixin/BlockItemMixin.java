package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 8.5: Blöcke aus {@code needs_atmosphere} lassen sich ohne Atmosphäre nicht setzen. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$noAtmosphere(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (AtmosphereRules.denyPlacement(context.getLevel(), context.getClickedPos(), state, context.getPlayer())) {
            cir.setReturnValue(false);
        }
    }
}
