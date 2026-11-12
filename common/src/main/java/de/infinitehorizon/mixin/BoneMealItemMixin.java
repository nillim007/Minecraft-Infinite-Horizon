package de.infinitehorizon.mixin;

import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 8.5: Knochenmehl wirkt ohne Atmosphäre nicht auf Pflanzen. */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void infinite_horizon$noAtmosphere(ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (AtmosphereRules.blocksBoneMeal(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
