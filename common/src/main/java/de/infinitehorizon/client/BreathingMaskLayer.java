package de.infinitehorizon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.GeneratedData;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Atemmaske der Monster auf fremden Welten (13.5): zeichnet das Modell der Kreatur noch einmal mit einer Textur, die
 * nur am Kopf deckt (wie das Halsband des Wolfs). Eine Textur je Art, passend zur Texturgröße ihres Modells
 * ({@code textures/entity/breathing_mask/<art>.png}, Platzhalter aus tools/gen_placeholders.py). Sichtbar in jeder
 * Dimension mit Planeten-Eintrag außer der Overworld.
 */
public class BreathingMaskLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {

    private final Identifier texture;

    public BreathingMaskLayer(RenderLayerParent<S, M> parent, EntityType<?> type) {
        super(parent);
        this.texture = InfiniteHorizon.id("textures/entity/breathing_mask/"
                + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + ".png");
    }

    /** Arten mit Atemmaske: alle {@code vanilla_mobs} der Planeten (erzeugt in {@code infinite_horizon/planet_mobs.json}). */
    public static List<EntityType<?>> types() {
        List<EntityType<?>> out = new ArrayList<>();
        GeneratedData.array("planet_mobs", "entities").forEach(e ->
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(e.getAsString())).ifPresent(out::add));
        return out;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (state.isInvisible || !PlanetVisuals.masksActive()) {
            return;
        }
        renderColoredCutoutModel(getParentModel(), texture, poseStack, collector, light, state, -1, 1);
    }
}
