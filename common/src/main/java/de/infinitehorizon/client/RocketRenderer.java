package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.infinitehorizon.space.RocketEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Rakete als Entity (12.5): zeichnet die Teile als Blockmodelle übereinander und die Flossen neben dem Triebwerk (nur
 * Darstellung). Die Teileliste kommt aus den synchronisierten Entity-Daten.
 */
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderer.State> {

    /** Versatz der vier Flossen neben dem Triebwerk (N, O, S, W). */
    private static final int[][] FIN_OFFSETS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public static class State extends EntityRenderState {
        public final List<BlockModelRenderState> parts = new ArrayList<>();
        public final List<BlockModelRenderState> fins = new ArrayList<>();
    }

    private final BlockModelResolver blocks;

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static void ensure(List<BlockModelRenderState> list, int size) {
        while (list.size() < size) {
            list.add(new BlockModelRenderState());
        }
        while (list.size() > size) {
            list.removeLast();
        }
    }

    @Override
    public void extractRenderState(RocketEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        List<Identifier> parts = entity.clientParts();
        ensure(state.parts, parts.size());
        for (int i = 0; i < parts.size(); i++) {
            Block b = BuiltInRegistries.BLOCK.getValue(parts.get(i));
            blocks.update(state.parts.get(i), b.defaultBlockState(), BlockDisplayContext.create());
        }
        Identifier fin = entity.clientFin();
        int fins = fin == null ? 0 : Math.min(entity.clientFins(), FIN_OFFSETS.length);
        ensure(state.fins, fins);
        for (int i = 0; i < fins; i++) {
            blocks.update(state.fins.get(i), BuiltInRegistries.BLOCK.getValue(fin).defaultBlockState(), BlockDisplayContext.create());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        for (int i = 0; i < state.parts.size(); i++) {
            pose.pushPose();
            pose.translate(0, i, 0);
            state.parts.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        for (int i = 0; i < state.fins.size(); i++) {
            pose.pushPose();
            pose.translate(FIN_OFFSETS[i][0], 0, FIN_OFFSETS[i][1]);
            state.fins.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
