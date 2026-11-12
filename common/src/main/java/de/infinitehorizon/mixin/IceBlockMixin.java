package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 8.5: In {@code freeze}-Gebieten ohne Atmosphäre schmilzt Eis nicht. */
@Mixin(IceBlock.class)
public abstract class IceBlockMixin {

    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$keepIce(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (AtmosphereRules.keepIce(level, pos)) {
            ci.cancel();
        }
    }
}
