package de.infinitehorizon.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 18: Eine Sternenkarte als Item-Entity verschwindet nicht von selbst ({@code despawns} false in items.json). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void infinite_horizon$keepStarChart(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && self.getItem().has(de.infinitehorizon.registry.ModDataComponents.CHART_OWNER.get())) {
            self.setUnlimitedLifetime();
        }
    }
}
