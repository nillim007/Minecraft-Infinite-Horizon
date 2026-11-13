package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * {@code sulfur_snail} (18): passiv, feuerfest; legt alle {@code trail_interval_ticks} eine {@code sulfur_trail}-Schicht,
 * alle {@code lay_interval_ticks} {@code lay_count} {@code lay_item} (wie das Huhn sein Ei) und vermehrt sich mit
 * {@code breed_item}.
 */
public class SulfurSnail extends Animal {

    private int trailTimer;
    private int layTimer;

    public SulfurSnail(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    private JsonObject params() {
        return CreatureRules.params(getType());
    }

    private Item item(String key) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(params().get(key).getAsString()));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PanicGoal(this, 1.2));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return params().has("breed_item") && stack.is(item("breed_item"));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return (AgeableMob) getType().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return reason != EntitySpawnReason.NATURAL || CreatureRules.canSpawn(getType(), level, blockPosition());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (++trailTimer >= CreatureRules.num(params(), "trail_interval_ticks")) {
            trailTimer = 0;
            placeTrail(level);
        }
        if (!isBaby() && ++layTimer >= CreatureRules.num(params(), "lay_interval_ticks")) {
            layTimer = 0;
            spawnAtLocation(level, new ItemStack(item("lay_item"), (int) CreatureRules.num(params(), "lay_count")));
        }
    }

    /** Schwefelspur an der eigenen Position, wenn dort Luft ist und darunter fester Boden. */
    public boolean placeTrail(ServerLevel level) {
        BlockPos p = blockPosition();
        Block trail = BuiltInRegistries.BLOCK.getValue(Identifier.parse(params().get("trail_block").getAsString()));
        BlockState state = trail.defaultBlockState();
        if (level.getBlockState(p).isAir() && state.canSurvive(level, p)) {
            level.setBlockAndUpdate(p, state);
            return true;
        }
        return false;
    }
}
