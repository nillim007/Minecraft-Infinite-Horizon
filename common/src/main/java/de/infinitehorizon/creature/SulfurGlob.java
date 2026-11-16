package de.infinitehorizon.creature;

import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code sulfur_glob} (18): Projektil des Schwefelsalamanders, fliegt wie der kleine Feuerball der Lohe, setzt aber
 * nichts in Brand ({@code projectile_sets_fire} false). Schaden = Angriffsschaden des Schützen.
 */
public class SulfurGlob extends Fireball {

    public SulfurGlob(EntityType<? extends SulfurGlob> type, Level level) {
        super(type, level);
    }

    public SulfurGlob(Level level, LivingEntity owner, Vec3 direction) {
        super(ModEntities.SULFUR_GLOB.get(), owner, direction, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity owner) {
            float damage = owner.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                    ? (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0f;
            Entity target = hit.getEntity();
            target.hurtServer(level, damageSources().mobProjectile(this, owner), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }
}
