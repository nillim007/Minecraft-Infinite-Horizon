package de.infinitehorizon.mixin;

import de.infinitehorizon.space.RocketEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 12.5: Absteigen mit Schleichen ist vom Countdown bis zur Landung gesperrt. 26.3 hat keinen Fahrzeug-Hook dafür
 * ({@code Player#rideTick} ruft direkt {@code stopRiding}); tote oder entfernte Spieler steigen immer aus.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    /** 18: Wird eine Sternenkarte als Item-Entity zerstört (nicht aufgehoben), verliert die Agentur ihre Karte. */
    @Inject(method = "remove", at = @At("HEAD"))
    private void infinite_horizon$starChartLost(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (reason.shouldDestroy() && self instanceof net.minecraft.world.entity.item.ItemEntity item
                && self.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var owner = item.getItem().get(de.infinitehorizon.registry.ModDataComponents.CHART_OWNER.get());
            if (owner != null && !item.getItem().isEmpty()) {
                de.infinitehorizon.creature.SignalData.get(level.getServer()).chartLost(owner.agency(), owner.uuid());
            }
        }
    }

    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$rocketLock(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player p && p.isAlive() && !p.isRemoved() && p.getVehicle() instanceof RocketEntity r
                && r.dismountLocked()) {
            ci.cancel();
        }
    }
}
