package de.infinitehorizon.mixin;

import com.mojang.serialization.Lifecycle;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla stuft jede Dimension außerhalb von {@code minecraft} als experimentell ein; die Welt zeigt dann beim
 * Erstellen und Laden den Hinweis „Experimentelle Einstellungen“. Fabric API hebt das für Mods auf, NeoForge nicht.
 * Unsere Planetendimensionen (13.1) gelten als stabil. SPEC-GAP #51.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {

    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void infinite_horizon$ownDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem stem,
                                                                CallbackInfoReturnable<Lifecycle> cir) {
        if (InfiniteHorizon.MOD_ID.equals(key.identifier().getNamespace())) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }
}
