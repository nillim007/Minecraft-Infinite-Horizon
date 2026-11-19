package de.infinitehorizon.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dämpfung im Vakuum ohne Kuppel (24.6, 3.7: {@code SoundEngine}-Lautstärke), siehe {@code SoundDamping}. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void infiniteHorizon$dampInVacuum(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float f = de.infinitehorizon.client.SoundDamping.factor(sound);
        if (f != 1f) {
            cir.setReturnValue(cir.getReturnValueF() * f);
        }
    }
}
