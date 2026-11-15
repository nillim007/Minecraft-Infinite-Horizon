package de.infinitehorizon.creature;

import de.infinitehorizon.data.Planet;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * {@code mirror_beetle} (18): neutral. Schaut ein Spieler ihn bei Tageslicht aus weniger als {@code gaze_distance} Blöcken
 * an, bekommt er Blindheit für {@code blindness_ticks}; angegriffen greift er zurück.
 */
public class MirrorBeetle extends PlanetCreature {

    /** Blickrichtung zum Käfer: Kosinus ab dem ein Blick als „angeschaut“ gilt (wie beim Enderman). */
    private static final double GAZE_COS = 0.975;

    public MirrorBeetle(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        if (!params().has("retaliates") || params().get("retaliates").getAsBoolean()) {
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }
    }

    /** Sieht der Spieler den Käfer an? Reine Rechnung über Blickrichtung und Richtung zum Käfer. */
    public static boolean looksAt(Vec3 eye, Vec3 view, Vec3 target, double maxDistance) {
        return de.infinitehorizon.world.XBands.looksAt(new double[]{eye.x, eye.y, eye.z}, new double[]{view.x, view.y, view.z},
                new double[]{target.x, target.y, target.z}, maxDistance, GAZE_COS);
    }

    private boolean daylight(ServerLevel level) {
        if (!params().has("requires_daylight") || !params().get("requires_daylight").getAsBoolean()) {
            return true;
        }
        Planet planet = PlanetResolver.at(level, blockPosition()).planet();
        long clock = level.getOverworldClockTime();
        return HazardTracker.rules().daylight(planet == null ? clock : planet.dayTime(clock)) > 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 5 != 0 || !daylight(level)) {
            return;
        }
        int ticks = (int) param("blindness_ticks");
        for (Player p : level.players()) {
            if (!p.isSpectator() && !p.isCreative() && !p.hasEffect(MobEffects.BLINDNESS)
                    && looksAt(p.getEyePosition(), p.getViewVector(1f), getEyePosition(), param("gaze_distance"))
                    && p.hasLineOfSight(this)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks));
            }
        }
    }
}
