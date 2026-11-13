package de.infinitehorizon.creature;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code shade_strider} (18): scheu; flieht vor Licht über {@code flee_light_above} und vor Spielern näher als
 * {@code flee_player_distance}. Bleibt ihm ein Spieler {@code follow_trigger_ticks} lang auf den Fersen (doppelte
 * Fluchtdistanz), läuft er zum nächsten {@code moon_ice} oder Erz im Radius {@code guide_radius}.
 */
public class ShadeStrider extends PlanetCreature {

    private int followTicks;
    private @Nullable BlockPos guide;

    public ShadeStrider(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public boolean guiding() {
        return guide != null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, (float) param("flee_player_distance"), 1.0, 1.3) {
            @Override
            public boolean canUse() {
                return !guiding() && super.canUse();
            }
        });
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8));
    }

    private boolean isGuideTarget(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
        if (params().has("guide_targets")) {
            for (JsonElement e : params().getAsJsonArray("guide_targets")) {
                String t = e.getAsString();
                if (t.equals(id) || ("ores".equals(t) && id.endsWith("_ore"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (guide != null) {
            if (guide.closerToCenterThan(position(), 3) || getNavigation().isDone() && tickCount % 20 == 0) {
                getNavigation().moveTo(guide.getX() + 0.5, guide.getY() + 1, guide.getZ() + 0.5, 1.0);
            }
            if (guide.closerToCenterThan(position(), 3)) {
                guide = null;
                followTicks = 0;
            }
            return;
        }
        Player follower = level.getNearestPlayer(this, 2 * param("flee_player_distance"));
        followTicks = follower == null ? 0 : followTicks + 1;
        if (followTicks >= param("follow_trigger_ticks")) {
            guide = nearest((int) param("guide_radius"), (p, s) -> isGuideTarget(s));
            followTicks = 0;
            return;
        }
        if (level.getMaxLocalRawBrightness(blockPosition()) > param("flee_light_above") && getNavigation().isDone()) {
            Vec3 away = DefaultRandomPos.getPos(this, 8, 4);
            if (away != null && level.getMaxLocalRawBrightness(BlockPos.containing(away)) <= param("flee_light_above")) {
                getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            }
        }
    }

    /** Für Tests: Ziel jetzt suchen, als wäre ihm lange genug ein Spieler gefolgt. */
    public @Nullable BlockPos startGuiding() {
        guide = nearest((int) param("guide_radius"), (p, s) -> isGuideTarget(s));
        return guide;
    }
}
