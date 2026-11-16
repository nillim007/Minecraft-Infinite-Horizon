package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code geyser_moth} (18): harmlos, fliegt und kreist im Umkreis {@code circle_radius} um den nächsten
 * {@code circle_block} (Ammoniak-Geysir). Ohne Geysir in der Nähe treibt sie langsam.
 */
public class GeyserMoth extends PlanetCreature {

    /** Geysirsuche alle 100 t im Würfel ±16 waagerecht, ±8 senkrecht; Flughöhe über dem Geysir (SPEC-GAP #106). */
    private static final int SEEK_INTERVAL = 100;
    private static final int SEEK_H = 16;
    private static final int SEEK_V = 8;
    private static final double HOVER = 3;

    private @Nullable BlockPos geyser;
    private int seekTimer;
    private double angle;

    public GeyserMoth(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public @Nullable BlockPos geyser() {
        return geyser;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || CreatureRules.inHomeDimension(getType(), level);
    }

    /** Nächster Geysir im Suchwürfel oder null. */
    public @Nullable BlockPos findGeyser(ServerLevel level) {
        JsonObject p = params();
        if (!p.has("circle_block")) {
            return null;
        }
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(p.get("circle_block").getAsString()));
        BlockPos c = blockPosition();
        BlockPos best = null;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-SEEK_H, -SEEK_V, -SEEK_H), c.offset(SEEK_H, SEEK_V, SEEK_H))) {
            if (level.getBlockState(q).is(b) && (best == null || q.distSqr(c) < best.distSqr(c))) {
                best = q.immutable();
            }
        }
        return best;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (--seekTimer <= 0) {
            seekTimer = SEEK_INTERVAL;
            geyser = findGeyser(level);
        }
        double speed = CreatureRules.num(CreatureRules.entry(getType()), "speed");
        if (geyser == null) {
            setDeltaMovement(getDeltaMovement().scale(0.9));
            return;
        }
        double r = Math.max(1, param("circle_radius"));
        angle += speed / r;
        Vec3 goal = new Vec3(geyser.getX() + 0.5 + Math.cos(angle) * r, geyser.getY() + HOVER, geyser.getZ() + 0.5 + Math.sin(angle) * r);
        Vec3 step = goal.subtract(position());
        if (step.lengthSqr() > speed * speed) {
            step = step.normalize().scale(speed);
        }
        setDeltaMovement(step);
        setYRot((float) (Math.atan2(-step.x, step.z) * 180 / Math.PI));
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
