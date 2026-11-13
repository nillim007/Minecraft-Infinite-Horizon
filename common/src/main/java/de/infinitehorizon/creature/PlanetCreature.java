package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Basis der Planetenkreaturen (18): Werte aus creatures.json, eigene Spawnregel, Projektil-Immunität auf Wunsch. */
public abstract class PlanetCreature extends PathfinderMob {

    protected PlanetCreature(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    protected JsonObject params() {
        return CreatureRules.params(getType());
    }

    protected double param(String key) {
        return CreatureRules.num(params(), key);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || CreatureRules.canSpawn(getType(), level, blockPosition());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (params().has("projectile_immune") && params().get("projectile_immune").getAsBoolean()
                && source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /** Nächster Block im Würfel {@code radius}, der {@code test} erfüllt (Abstand nach Quadrat). */
    protected @Nullable BlockPos nearest(int radius, java.util.function.BiPredicate<BlockPos, BlockState> test) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos center = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState s = level().getBlockState(p);
            if (!s.isAir() && test.test(p, s)) {
                double d = p.distSqr(center);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** Für Unterklassen: Block ohne Drop entfernen. */
    protected void eat(BlockPos pos) {
        level().destroyBlock(pos, false, this);
        level().gameEvent(this, net.minecraft.world.level.gameevent.GameEvent.EAT, pos);
    }

    protected static boolean is(BlockState s, Block b) {
        return s.is(b);
    }
}
