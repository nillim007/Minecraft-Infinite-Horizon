package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 8.5: Blöcke aus {@code needs_atmosphere} lassen sich ohne Atmosphäre nicht setzen. 11.2: Von Spielern gesetzte Blöcke
 * aus den {@code sampleable}-Tags werden als nicht beprobbar markiert.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$noAtmosphere(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (AtmosphereRules.denyPlacement(context.getLevel(), context.getClickedPos(), state, context.getPlayer())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void infinite_horizon$markPlaced(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && context.getPlayer() != null) {
            de.infinitehorizon.research.SampleTool.onPlayerPlaced(context.getLevel(), context.getClickedPos(), state);
        }
    }
}
