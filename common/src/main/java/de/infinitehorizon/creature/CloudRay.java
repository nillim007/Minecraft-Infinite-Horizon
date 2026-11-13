package de.infinitehorizon.creature;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * {@code cloud_ray} (18): passiv, fliegend; mit {@code tame_item} zähmbar ({@code tame_chance} je Versuch); gezähmt und
 * mit {@code mount_requires} (Sattel) ein fliegendes Reittier mit {@code mount_speed}. Gelenkt wird über die
 * Blickrichtung des Reiters (auch nach oben und unten).
 */
public class CloudRay extends PlanetCreature {

    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(CloudRay.class, EntityDataSerializers.BOOLEAN);
    /** Kreisflug-Wendigkeit wie Vanilla-Flieger (SPEC-GAP #92). */
    private static final int TURN = 20;

    private @Nullable UUID owner;

    public CloudRay(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, TURN, true);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !isVehicle() && super.canUse();
            }
        });
    }

    public boolean tamed() {
        return owner != null;
    }

    public boolean saddled() {
        return entityData.get(SADDLED);
    }

    private boolean is(ItemStack stack, String key) {
        return params().has(key) && stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(params().get(key).getAsString())));
    }

    /** Ein Zähmversuch (reine Regel mit Zufallswert): gelingt, wenn {@code roll < tame_chance}. */
    public boolean tryTame(Player player, double roll) {
        if (tamed()) {
            return true;
        }
        if (roll < param("tame_chance")) {
            owner = player.getUUID();
            return true;
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level() instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!tamed() && is(held, "tame_item")) {
            held.consume(1, player);
            boolean ok = tryTame(player, random.nextDouble());
            level().broadcastEntityEvent(this, ok ? (byte) 7 : (byte) 6);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (tamed() && !saddled() && is(held, "mount_requires")) {
            held.consume(1, player);
            entityData.set(SADDLED, true);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (tamed() && saddled() && !isVehicle()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return saddled() && getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setYRot(player.getYRot());
        setXRot(player.getXRot());
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza;
        double up = forward * -Math.sin(Math.toRadians(player.getXRot()));
        return new Vec3(player.xxa, up, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) param("mount_speed");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.storeNullable("owner", UUIDUtil.CODEC, owner);
        out.putBoolean("saddled", saddled());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        entityData.set(SADDLED, in.getBooleanOr("saddled", false));
    }
}
