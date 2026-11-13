package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * Platzhalter-Darstellung der Kreaturen (18, 24.5): ein Blockmodell in Größe der Hitbox, gedreht nach dem Körper.
 * Unsichtbare Kreaturen (z. B. eingegrabener {@code dune_burrower}) werden nicht gezeichnet. SPEC-GAP #92: eigene
 * Modelle folgen mit dem grafischen Feinschliff.
 */
public class CreatureRenderer<T extends LivingEntity> extends EntityRenderer<T, CreatureRenderer.State> {

    public static class State extends EntityRenderState {
        public final BlockModelRenderState block = new BlockModelRenderState();
        public float yRot;
        public float width;
        public float height;
        public boolean hidden;
        public boolean hurt;
    }

    private final BlockModelResolver blocks;
    private final Function<T, BlockState> look;

    public CreatureRenderer(EntityRendererProvider.Context context, Function<T, BlockState> look) {
        super(context);
        this.blocks = context.getBlockModelResolver();
        this.look = look;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        blocks.update(state.block, look.apply(entity), BlockDisplayContext.create());
        state.yRot = entity.yBodyRot;
        state.width = entity.getBbWidth();
        state.height = entity.getBbHeight();
        state.hidden = entity.isInvisible();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hidden) {
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, -state.yRot);
            pose.translate(-state.width / 2, 0, -state.width / 2);
            pose.scale(state.width, state.height, state.width);
            state.block.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
