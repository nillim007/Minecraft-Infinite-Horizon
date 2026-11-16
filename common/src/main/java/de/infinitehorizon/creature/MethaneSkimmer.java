package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * {@code methane_skimmer} (18): passiv, läuft auf der Oberfläche von {@code walks_on_fluid} (Methan) wie der Schreiter
 * auf Lava und flieht vor Spielern näher als {@code flee_player_distance}. Sonst wechselt er ab und zu die Richtung.
 */
public class MethaneSkimmer extends PlanetCreature {

    /** Neue Laufrichtung alle 60 t (SPEC-GAP #106). */
    private static final int TURN_INTERVAL = 60;

    private int turnTimer;
    private Vec3 heading = Vec3.ZERO;

    public MethaneSkimmer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    private Fluid fluid() {
        JsonObject p = params();
        return p.has("walks_on_fluid") ? BuiltInRegistries.FLUID.getValue(Identifier.parse(p.get("walks_on_fluid").getAsString()))
                : net.minecraft.world.level.material.Fluids.EMPTY;
    }

    @Override
    public boolean canStandOnFluid(FluidState state) {
        return !state.isEmpty() && state.getType().isSame(fluid());
    }

    /** Standfläche auf dem Fluid: halbe Blockhöhe wie beim Schreiter auf Lava. */
    private static final net.minecraft.world.phys.shapes.VoxelShape STAND = net.minecraft.world.level.block.Block.column(16.0, 0.0, 8.0);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getLiquidCollisionShape() {
        return STAND;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || CreatureRules.inHomeDimension(getType(), level)
                && canStandOnFluid(level.getFluidState(blockPosition().below())) && level.getFluidState(blockPosition()).isEmpty();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        double speed = CreatureRules.num(CreatureRules.entry(getType()), "speed");
        Player near = level.getNearestPlayer(this, param("flee_player_distance"));
        if (near != null && !near.isSpectator() && !near.isCreative()) {
            Vec3 away = position().subtract(near.position()).multiply(1, 0, 1);
            heading = away.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : away.normalize();
            turnTimer = TURN_INTERVAL;
        } else if (--turnTimer <= 0) {
            turnTimer = TURN_INTERVAL;
            double a = getRandom().nextDouble() * Math.PI * 2;
            heading = getRandom().nextBoolean() ? new Vec3(Math.cos(a), 0, Math.sin(a)) : Vec3.ZERO;
        }
        getMoveControl().setWantedPosition(getX() + heading.x * 2, getY(), getZ() + heading.z * 2, 1.0);
        if (heading.lengthSqr() > 0) {
            setYRot((float) (Math.atan2(-heading.x, heading.z) * 180 / Math.PI));
        }
        setSpeed((float) speed);
    }
}
