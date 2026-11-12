package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 8.5: Gesetztes Wasser gefriert ({@code freeze}) oder verdampft ({@code evaporate}) ohne Atmosphäre. */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void infinite_horizon$noAtmosphere(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                               boolean movedByPiston, CallbackInfo ci) {
        if (!level.isClientSide() && state.getFluidState().is(FluidTags.WATER)) {
            AtmosphereRules.waterPlaced(level, pos);
        }
    }
}
