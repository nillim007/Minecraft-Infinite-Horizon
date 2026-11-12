package de.infinitehorizon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import de.infinitehorizon.client.PlanetVisuals;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.MoonPhase;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Feste Himmelskörper der Planeten (25.1 {@code sky.celestial_bodies}, z. B. die Erde am Mondhimmel): gezeichnet wie
 * die Vanilla-Sonne (Quad aus dem Atlas {@code celestials}), aber an fester Stelle, die sich nicht mit der Tageszeit
 * dreht. Läuft auf beiden Loadern (Fabric hat keinen Himmel-Hook).
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Shadow
    @Final
    private TextureAtlas celestialsAtlas;

    @Shadow
    @Final
    private RenderSystem.AutoStorageIndexBuffer quadIndices;

    @Shadow
    @Final
    private static float SUN_HEIGHT;

    @Unique
    private final Map<Identifier, GpuBuffer> infinite_horizon$quads = new HashMap<>();

    @Shadow
    protected abstract Matrix4f applyCelestialBodyTransform(PoseStack poseStack, float height, float size);

    @Shadow
    protected abstract void drawCelestialBody(Supplier<String> label, RenderPass pass, GpuBufferSlice transform,
                                              GpuBuffer indices, GpuBuffer vertices, int baseVertex);

    @Shadow
    private static GpuBuffer buildCelestialQuad(String label, TextureAtlasSprite sprite) {
        throw new AssertionError();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void infinite_horizon$celestialBodies(RenderPass pass, PoseStack pose, float sunAngle, float moonAngle,
                                                  float starAngle, MoonPhase phase, float sunAlpha, float starBrightness,
                                                  CallbackInfo ci) {
        for (PlanetVisuals.Body body : PlanetVisuals.bodies()) {
            GpuBuffer quad = infinite_horizon$quads.computeIfAbsent(body.sprite(),
                    id -> buildCelestialQuad("Infinite Horizon " + id, celestialsAtlas.getSprite(id)));
            pose.pushPose();
            // Azimut ab Norden im Uhrzeigersinn, Höhe über dem Horizont; das Quad liegt ohne Drehung im Zenit
            pose.rotateDegrees(Axis.YP, -body.azimuth());
            pose.rotateDegrees(Axis.XP, -(90.0F - body.elevation()));
            Matrix4f m = applyCelestialBodyTransform(pose, SUN_HEIGHT, body.size());
            GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(m, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
            drawCelestialBody(() -> "Infinite Horizon celestial body", pass, transform, quadIndices.getBuffer(6), quad, 0);
            pose.popPose();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void infinite_horizon$close(CallbackInfo ci) {
        infinite_horizon$quads.values().forEach(GpuBuffer::close);
        infinite_horizon$quads.clear();
    }
}
