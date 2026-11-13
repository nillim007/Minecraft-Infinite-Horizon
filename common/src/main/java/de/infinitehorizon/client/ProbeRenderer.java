package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.space.ProbeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Startende Sonde (12.3): zeichnet den Sondenblock und dreht ihn langsam um die Hochachse. */
public class ProbeRenderer extends EntityRenderer<ProbeEntity, ProbeRenderer.State> {

    /** Grad je Tick. */
    private static final float SPIN = 4f;

    public static class State extends EntityRenderState {
        public final BlockModelRenderState block = new BlockModelRenderState();
    }

    private final BlockModelResolver blocks;

    public ProbeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProbeEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blocks.update(state.block, ModBlocks.PROBE.get().defaultBlockState(), BlockDisplayContext.create());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, state.ageInTicks * SPIN);
        pose.translate(-0.5, 0, -0.5);
        state.block.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
