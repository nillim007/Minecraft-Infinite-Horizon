package de.infinitehorizon.mixin;

import de.infinitehorizon.space.FlightService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 12.5: Vor dem Abmelden aus der Rakete bzw. dem Sitz steigen. Sonst speichert Vanilla ein Fahrzeug mit genau einem
 * Spieler zusammen mit dem Spieler und entfernt es aus der Welt; im Flug merkt sich der Spieler {@code pending_flight}.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void infinite_horizon$leaveRocket(ServerPlayer player, CallbackInfo ci) {
        FlightService.onLogout(player);
    }
}
