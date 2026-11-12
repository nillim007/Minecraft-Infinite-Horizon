package de.infinitehorizon.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Zugriff auf {@code LivingEntityRenderer#addLayer} (geschützt), für die Atemmaske auf NeoForge. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    @Invoker("addLayer")
    @SuppressWarnings("rawtypes")
    boolean infinite_horizon$addLayer(RenderLayer layer);
}
