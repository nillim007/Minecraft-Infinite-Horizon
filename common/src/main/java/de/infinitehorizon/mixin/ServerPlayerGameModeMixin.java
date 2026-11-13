package de.infinitehorizon.mixin;

import de.infinitehorizon.agency.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 23.1, 23.2: Nicht-Mitglieder bauen fremde Block-Entities und in aktiven fremden Kuppeln keinen Block ab. Liefert
 * {@code destroyBlock} false, schickt Vanilla dem Client den Block zurück ({@code destroyAndAck}).
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$protect(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Permissions.mayBreak(player, level, pos)) {
            player.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
            cir.setReturnValue(false);
        }
    }
}
