package de.infinitehorizon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Der Vanilla-GameTest-Server baut seine Welt nur aus dem Flachwelt-Preset und übergibt eine leere
 * Dimensions-Registry; Dimensionen aus Datenpaketen (Mond, 13.1) fehlen dort. Tests in Mod-Dimensionen (27) brauchen
 * sie: Dieser Mixin reicht die Datenpaket-Dimensionen durch. SPEC-GAP #47. Echte Welten laden sie ohnehin.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {

    @WrapOperation(method = "lambda$create$1", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete infinite_horizon$withDatapackDimensions(WorldDimensions dimensions,
            Registry<LevelStem> empty, Operation<WorldDimensions.Complete> original,
            @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return original.call(dimensions, context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
    }
}
