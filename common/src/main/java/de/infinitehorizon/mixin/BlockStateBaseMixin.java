package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 8.5: Fackeln und andere Blöcke aus {@code needs_atmosphere} bekommen Random Ticks und erlöschen ohne Atmosphäre;
 * Pflanzen wachsen dort nicht (Random Tick abgebrochen).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "isRandomlyTicking", at = @At("RETURN"), cancellable = true)
    private void infinite_horizon$tickWithoutAir(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && asState().is(AtmosphereRules.NEEDS_ATMOSPHERE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$noAtmosphere(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (AtmosphereRules.randomTick(asState(), level, pos)) {
            ci.cancel();
        }
    }
}
