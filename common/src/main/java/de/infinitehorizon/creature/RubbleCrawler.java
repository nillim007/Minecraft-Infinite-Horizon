package de.infinitehorizon.creature;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.dome.DomeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * {@code rubble_crawler} (18): neutral; als Stein getarnt, solange kein Spieler näher als {@code reveal_distance} ist.
 * Sucht Blöcke aus {@code crawler_food} außerhalb von Kuppeln im Radius {@code food_search_radius} und baut alle
 * {@code eat_interval_ticks} einen ab. SPEC-GAP #92: auch getarnt kriecht er zu seiner Nahrung, wandert aber nicht.
 */
public class RubbleCrawler extends PlanetCreature {

    public static final TagKey<Block> FOOD = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("crawler_food"));
    private static final EntityDataAccessor<Boolean> DISGUISED = SynchedEntityData.defineId(RubbleCrawler.class,
            EntityDataSerializers.BOOLEAN);

    private int eatCooldown;

    public RubbleCrawler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISED, true);
    }

    public boolean disguised() {
        return entityData.get(DISGUISED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !disguised() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        boolean near = level.getNearestPlayer(this, param("reveal_distance")) != null;
        entityData.set(DISGUISED, !near && getTarget() == null);
        if (--eatCooldown > 0) {
            return;
        }
        eatCooldown = (int) param("eat_interval_ticks");
        BlockPos food = nearest((int) param("food_search_radius"), (p, s) -> s.is(FOOD)
                && (!params().has("food_outside_domes_only") || DomeManager.domeAt(level, p).isEmpty()));
        if (food == null) {
            return;
        }
        if (food.closerToCenterThan(position(), 2.5)) {
            eat(food);
        } else {
            getNavigation().moveTo(food.getX() + 0.5, food.getY(), food.getZ() + 0.5, 1.0);
        }
    }
}
