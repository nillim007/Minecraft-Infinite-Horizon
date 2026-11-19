package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.vehicle.StarshipEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Sternenschiff (21.3): Platzhalter aus Blockmodellen (Rumpf, Fenster vorn, zwei Triebwerke hinten). Das echte Modell
 * folgt mit dem grafischen Feinschliff (M9).
 */
public class StarshipRenderer extends EntityRenderer<StarshipEntity, StarshipRenderer.State> {

    /** Bauplan: x, y, z (Blöcke relativ zur Mitte) und Block-ID. */
    private static final Object[][] SHAPE = buildShape();

    public static class State extends EntityRenderState {
        public final List<BlockModelRenderState> blocks = new ArrayList<>();
        public float yaw;
    }

    private final BlockModelResolver resolver;

    public StarshipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.resolver = context.getBlockModelResolver();
    }

    private static Object[][] buildShape() {
        List<Object[]> s = new ArrayList<>();
        for (int z = -3; z <= 3; z++) {
            for (int x = -1; x <= 1; x++) {
                s.add(new Object[]{x, 0, z, "infinite_horizon:station_hull"});
                if (Math.abs(x) == 1 || z == -3) {
                    s.add(new Object[]{x, 1, z, z >= 2 ? "infinite_horizon:station_window" : "infinite_horizon:station_hull"});
                }
            }
        }
        s.add(new Object[]{-1, 0, -4, "infinite_horizon:rocket_engine_t2"});
        s.add(new Object[]{1, 0, -4, "infinite_horizon:rocket_engine_t2"});
        return s.toArray(new Object[0][]);
    }

    private static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(id));
        return b == Blocks.AIR ? Blocks.IRON_BLOCK : b;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StarshipEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        while (state.blocks.size() < SHAPE.length) {
            state.blocks.add(new BlockModelRenderState());
        }
        for (int i = 0; i < SHAPE.length; i++) {
            resolver.update(state.blocks.get(i), block((String) SHAPE[i][3]).defaultBlockState(), BlockDisplayContext.create());
        }
        state.yaw = entity.getYRot(partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.rotateDegrees(com.mojang.math.Axis.YP, -state.yaw);
        for (int i = 0; i < SHAPE.length; i++) {
            pose.pushPose();
            pose.translate((int) SHAPE[i][0] - 0.5, (int) SHAPE[i][1], (int) SHAPE[i][2] - 0.5);
            state.blocks.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    /** Für Logs: verwendete Blöcke. */
    static String describe() {
        return InfiniteHorizon.MOD_ID + ":starship " + SHAPE.length + " Blöcke";
    }
}
