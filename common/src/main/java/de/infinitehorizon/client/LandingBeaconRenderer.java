package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.infinitehorizon.space.LandingBeaconBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roter Strahl des Landefeuers (12.6) wie beim Vanilla-Leuchtfeuer, bis zur absoluten Höhe {@code beam_top_y}, nur
 * unter freiem Himmel.
 */
public class LandingBeaconRenderer implements BlockEntityRenderer<LandingBeaconBlockEntity, LandingBeaconRenderer.State> {

    /** Rot (ARGB); Farbe laut spaceflight.json {@code beam_color} = red. */
    private static final int RED = 0xFFE02020;
    /** Takt der Vanilla-Strahlanimation. */
    private static final int ANIMATION_PERIOD = 40;
    private static final float SOLID = 0.2f;
    private static final float GLOW = 0.25f;

    public static class State extends BlockEntityRenderState {
        public float animationTime;
        public int height;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LandingBeaconBlockEntity be, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, state, crumbling);
        var level = be.getLevel();
        boolean open = level != null && level.canSeeSky(be.getBlockPos().above());
        state.height = open ? Math.max(0, LandingBeaconBlockEntity.beamTop() - be.getBlockPos().getY() - 1) : 0;
        state.animationTime = level == null ? 0 : Math.floorMod(level.getGameTime(), ANIMATION_PERIOD) + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.height > 0) {
            BeaconRenderer.submitBeaconBeam(pose, collector, BeaconRenderer.BEAM_LOCATION, 1.0f, state.animationTime, 1,
                    state.height, RED, SOLID, GLOW);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return LandingBeaconBlockEntity.beamTop();
    }
}
