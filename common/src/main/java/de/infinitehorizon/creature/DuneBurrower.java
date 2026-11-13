package de.infinitehorizon.creature;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.vehicle.RoverEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * {@code dune_burrower} (18): feindlich; gräbt unsichtbar im Sand. Laufende Maschinen aus {@code vibrating} und Rover
 * mit Bohrer im Radius {@code vibration_radius} ziehen ihn an; Spieler in der Nähe greift er an (dann sichtbar).
 */
public class DuneBurrower extends Monster {

    public static final TagKey<Block> VIBRATING = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("vibrating"));
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(DuneBurrower.class,
            EntityDataSerializers.BOOLEAN);

    public DuneBurrower(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, true);
    }

    public boolean burrowed() {
        return entityData.get(BURROWED);
    }

    private double param(String key) {
        return CreatureRules.num(CreatureRules.params(getType()), key);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || CreatureRules.canSpawn(getType(), level, blockPosition());
    }

    /** Nächste Vibrationsquelle: laufende Maschine aus {@code vibrating} oder Rover mit Bohrer. */
    public @Nullable BlockPos vibration(ServerLevel level) {
        int r = (int) param("vibration_radius");
        BlockPos center = blockPosition();
        BlockPos best = null;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r / 2, -r), center.offset(r, r / 2, r))) {
            if (level.getBlockState(p).is(VIBRATING) && level.getBlockEntity(p) instanceof MachineBlockEntity be
                    && be.status() == MachineStatus.RUNNING && (best == null || p.distSqr(center) < best.distSqr(center))) {
                best = p.immutable();
            }
        }
        for (RoverEntity rover : level.getEntitiesOfClass(RoverEntity.class, new AABB(center).inflate(r))) {
            if (rover.has(RoverEntity.DRILL) && (best == null || rover.blockPosition().distSqr(center) < best.distSqr(center))) {
                best = rover.blockPosition();
            }
        }
        return best;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        boolean hunting = getTarget() != null && getTarget().distanceToSqr(this) < 16;
        entityData.set(BURROWED, !hunting);
        setInvisible(burrowed() && CreatureRules.params(getType()).has("burrowed_invisible"));
        if (getTarget() == null && tickCount % 20 == 0) {
            BlockPos v = vibration(level);
            if (v != null) {
                getNavigation().moveTo(v.getX() + 0.5, v.getY(), v.getZ() + 0.5, 1.0);
            }
        }
    }
}
