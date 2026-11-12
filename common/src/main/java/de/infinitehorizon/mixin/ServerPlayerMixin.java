package de.infinitehorizon.mixin;

import com.mojang.datafixers.util.Either;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.AtmosphereRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 14.4: In Planetendimensionen setzen Betten den Spawnpunkt nur in einer aktiven Kuppel; draußen kommt die Meldung
 * {@code message.infinite_horizon.bed.no_air}, und Schlafen geht nicht.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$noAir(AbstractBedBlock block, BlockState state, BedRule rule, BlockPos pos,
                                        CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (AtmosphereRules.airless(self.level(), pos)) {
            cir.setReturnValue(Either.left(new Player.BedSleepingProblem(
                    Component.translatable("message." + InfiniteHorizon.MOD_ID + ".bed.no_air"))));
        }
    }
}
