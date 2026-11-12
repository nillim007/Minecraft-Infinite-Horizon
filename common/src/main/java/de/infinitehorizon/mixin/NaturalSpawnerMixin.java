package de.infinitehorizon.mixin;

import de.infinitehorizon.world.PlanetMobRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spawnrate der Monster in Planetendimensionen (13.5): bricht natürliche Spawns mit Wahrscheinlichkeit 1 − rate ab. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "isValidPositionForMob", at = @At("RETURN"), cancellable = true)
    private static void infinite_horizon$planetRate(ServerLevel level, Mob mob, double distance,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !PlanetMobRules.keep(level, mob)) {
            cir.setReturnValue(false);
        }
    }
}
