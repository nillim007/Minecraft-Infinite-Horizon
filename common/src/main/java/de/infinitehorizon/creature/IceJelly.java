package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.world.StructureStarts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code ice_jelly} (18): passiv, leuchtet, schwimmt langsam ({@code speed}) zur nächsten {@code seek_block} im Umkreis
 * {@code seek_radius}. Nahe Quellen findet sie direkt, weiter entfernte über die Quellenfelder ({@code hydrothermal_field}).
 */
public class IceJelly extends WaterAnimal {

    /** Neue Zielsuche alle 40 t; direkter Blick auf Quellen im Würfel ±8 (SPEC-GAP #105). */
    private static final int SEEK_INTERVAL = 40;
    private static final int LOCAL_SCAN = 8;
    /** Schwebehöhe über der Quelle und Abbremsen im Wasser. */
    private static final double HOVER = 1.5;
    private static final double DRAG = 0.9;
    private static final Identifier FIELD = InfiniteHorizon.id("hydrothermal_field");

    private @Nullable Vec3 target;
    private int seekTimer;

    public IceJelly(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
    }

    private JsonObject params() {
        return CreatureRules.params(getType());
    }

    public @Nullable Vec3 target() {
        return target;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || level.getFluidState(blockPosition()).is(FluidTags.WATER)
                && CreatureRules.inHomeDimension(getType(), level);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (--seekTimer <= 0) {
            seekTimer = SEEK_INTERVAL;
            target = findVent(level);
        }
        if (!isInWater()) {
            return;
        }
        double speed = CreatureRules.num(CreatureRules.entry(getType()), "speed");
        if (target != null && target.distanceToSqr(position()) > HOVER * HOVER) {
            setDeltaMovement(target.subtract(position()).normalize().scale(speed));
            setYRot((float) (Math.atan2(-getDeltaMovement().x, getDeltaMovement().z) * 180 / Math.PI));
        }
    }

    /** Nächste Quelle: im Würfel ±{@link #LOCAL_SCAN}, sonst die Mitte des nächsten Quellenfelds im Suchradius. */
    public @Nullable Vec3 findVent(ServerLevel level) {
        JsonObject p = params();
        if (!p.has("seek_block")) {
            return null;
        }
        Block vent = BuiltInRegistries.BLOCK.getValue(Identifier.parse(p.get("seek_block").getAsString()));
        BlockPos center = blockPosition();
        BlockPos best = null;
        for (BlockPos q : BlockPos.betweenClosed(center.offset(-LOCAL_SCAN, -LOCAL_SCAN, -LOCAL_SCAN),
                center.offset(LOCAL_SCAN, LOCAL_SCAN, LOCAL_SCAN))) {
            if (level.getBlockState(q).is(vent) && (best == null || q.distSqr(center) < best.distSqr(center))) {
                best = q.immutable();
            }
        }
        if (best != null) {
            return Vec3.atCenterOf(best).add(0, HOVER, 0);
        }
        Structure field = StructureStarts.structure(level, FIELD);
        BlockPos far = field == null ? null : StructureStarts.nearest(level, field, center, (int) CreatureRules.num(p, "seek_radius"));
        return far == null ? null : Vec3.atCenterOf(far);
    }

    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(DRAG));
        } else {
            super.travel(input);
        }
    }
}
