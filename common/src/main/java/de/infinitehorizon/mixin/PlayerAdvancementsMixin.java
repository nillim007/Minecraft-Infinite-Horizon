package de.infinitehorizon.mixin;

import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 24.3: Jeder erreichte Erfolg der Mod kommt mit Zeitstempel in {@code logs/infinite_horizon-progress.log}. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void infinite_horizon$log(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
        PlayerAdvancements self = (PlayerAdvancements) (Object) this;
        if (cir.getReturnValueZ() && InfiniteHorizon.MOD_ID.equals(holder.id().getNamespace())
                && self.getOrStartProgress(holder).isDone()) {
            de.infinitehorizon.agency.ProgressLog.write(player, holder.id());
        }
    }
}
