package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.infinitehorizon.vehicle.RoverEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;

/**
 * Rover (16, Platzhalter-Grafik 24.5): Wanne aus Eisenblock in der Größe des Entity, vier Räder aus Kohleblock und mit
 * Druckkabine ein Glasaufbau.
 */
public class RoverRenderer extends EntityRenderer<RoverEntity, RoverRenderer.State> {

    public static class State extends EntityRenderState {
        public final BlockModelRenderState body = new BlockModelRenderState();
        public final BlockModelRenderState wheel = new BlockModelRenderState();
        public final BlockModelRenderState cabin = new BlockModelRenderState();
        public float yRot;
        public float width;
        public float height;
        public boolean hasCabin;
    }

    private final BlockModelResolver blocks;

    public RoverRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RoverEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blocks.update(state.body, Blocks.IRON_BLOCK.defaultBlockState(), BlockDisplayContext.create());
        blocks.update(state.wheel, Blocks.COAL_BLOCK.defaultBlockState(), BlockDisplayContext.create());
        blocks.update(state.cabin, Blocks.GLASS.defaultBlockState(), BlockDisplayContext.create());
        state.yRot = entity.getYRot(partialTick);
        state.width = entity.getBbWidth();
        state.height = entity.getBbHeight();
        state.hasCabin = entity.has(RoverEntity.CABIN);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float w = state.width;
        float wheel = w / 4;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, -state.yRot);
        // Räder an den Ecken
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                pose.pushPose();
                pose.translate(sx * (w / 2 - wheel / 2) - wheel / 2, 0, sz * (w / 2 - wheel / 2) - wheel / 2);
                pose.scale(wheel, wheel, wheel);
                state.wheel.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
        // Wanne
        pose.pushPose();
        pose.translate(-w / 2, wheel / 2, -w / 2);
        pose.scale(w, state.height / 4, w);
        state.body.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        if (state.hasCabin) {
            pose.pushPose();
            pose.translate(-w / 3, wheel / 2 + state.height / 4, -w / 3);
            pose.scale(w * 2 / 3, state.height / 2, w * 2 / 3);
            state.cabin.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
