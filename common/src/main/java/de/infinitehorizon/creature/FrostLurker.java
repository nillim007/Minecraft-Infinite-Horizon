package de.infinitehorizon.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * {@code frost_lurker} (18): feindlich; liegt unsichtbar und reglos im Eis ({@code hidden_in_ice}) und bricht hervor,
 * sobald ein Spieler näher als {@code emerge_distance} kommt oder er getroffen wird. Danach greift er im Nahkampf an.
 */
public class FrostLurker extends PlanetCreature {

    private boolean hidden;

    public FrostLurker(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        hidden = params().has("hidden_in_ice") && params().get("hidden_in_ice").getAsBoolean();
        setInvisible(hidden);
    }

    public boolean hidden() {
        return hidden;
    }

    /** Sofort hervorbrechen (Annäherung, Treffer oder Beschwörung durch den Wächter). */
    public void emerge() {
        if (!hidden) {
            return;
        }
        hidden = false;
        setInvisible(false);
        playSound(SoundEvents.GLASS_BREAK, 1f, 0.6f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !hidden && super.canUse();
            }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return !hidden && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!hidden) {
            return;
        }
        setDeltaMovement(Vec3.ZERO.add(0, getDeltaMovement().y, 0));
        Player p = level.getNearestPlayer(this, param("emerge_distance"));
        if (p != null && !p.isCreative() && !p.isSpectator()) {
            emerge();
            setTarget(p);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        emerge();
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("hidden", hidden);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        hidden = input.getBooleanOr("hidden", hidden);
        setInvisible(hidden);
    }
}
