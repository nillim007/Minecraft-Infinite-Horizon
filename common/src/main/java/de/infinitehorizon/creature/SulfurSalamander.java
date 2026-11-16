package de.infinitehorizon.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * {@code sulfur_salamander} (18): feindlich, feuerfest; greift aus der Ferne mit Schwefelklumpen an (wie die Lohe mit
 * kleinen Feuerbällen, aber ohne Feuer). Lebt in den Lavaseen und an den Vulkanschloten von Io.
 */
public class SulfurSalamander extends PlanetCreature implements RangedAttackMob {

    /** Angriff alle 60 t im Umkreis 16 wie die Lohe (SPEC-GAP #104). */
    private static final int ATTACK_INTERVAL = 60;
    private static final float ATTACK_RADIUS = 16f;

    public SulfurSalamander(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, ATTACK_INTERVAL, ATTACK_RADIUS));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, ATTACK_RADIUS));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        SulfurGlob glob = shoot(level, target);
        level.addFreshEntity(glob);
    }

    /** Schwefelklumpen in Richtung des Ziels, mit kleiner Streuung wie bei der Lohe. */
    public SulfurGlob shoot(ServerLevel level, LivingEntity target) {
        double spread = Math.sqrt(Math.sqrt(distanceToSqr(target))) * 0.5;
        Vec3 dir = new Vec3(target.getX() - getX(), target.getY(0.5) - getY(0.5), target.getZ() - getZ());
        dir = new Vec3(getRandom().triangle(dir.x, 2.297 * spread), dir.y, getRandom().triangle(dir.z, 2.297 * spread));
        SulfurGlob glob = new SulfurGlob(level, this, dir.normalize());
        glob.setPos(glob.getX(), getY(0.5) + 0.5, glob.getZ());
        glob.setItem(new ItemStack(de.infinitehorizon.registry.ModItems.simple("sulfur").get()));
        return glob;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
