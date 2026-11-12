package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 8.5: Feuer (Feuerzeug, Feuerkugel) lässt sich ohne Atmosphäre nicht entzünden. */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {

    @Inject(method = "canBePlacedAt", at = @At("HEAD"), cancellable = true)
    private static void infinite_horizon$noAtmosphere(Level level, BlockPos pos, Direction direction,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel sl && AtmosphereRules.airless(sl, pos)) {
            cir.setReturnValue(false);
        }
    }
}
