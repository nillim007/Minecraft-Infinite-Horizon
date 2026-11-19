package de.infinitehorizon.vehicle;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * {@code starship} (21.3, 23.4): Sternenschiff nach dem Finale. 8 Sitze, die ersten {@code crew_seats} sind die
 * Besatzung; nur sie öffnet mit der Inventartaste die Sprungziele. Springt nur zwischen Orbit-Dimensionen, schwebt,
 * nimmt keinen Schaden.
 */
public class StarshipEntity extends Entity implements HasCustomInventoryScreen {

    private @Nullable UUID owner;

    public StarshipEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("vehicles", "infinite_horizon:starship").orElse(new JsonObject());
    }

    private static int num(String key, int fallback) {
        JsonObject r = rules();
        return r.has(key) ? r.get(key).getAsInt() : fallback;
    }

    public int seats() {
        return num("seats", 1);
    }

    public int crewSeats() {
        return num("crew_seats", 1);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    /** Sitzt der Spieler auf einem Besatzungsplatz? */
    public boolean isCrew(Entity e) {
        int i = getPassengers().indexOf(e);
        return i >= 0 && i < crewSeats();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return getType().getDimensions();
    }

    /** Besatzung vorn, Passagiere hinten, je Reihe nebeneinander. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        int row = i < crewSeats() ? 0 : 1;
        int perRow = Math.max(1, Math.max(crewSeats(), seats() - crewSeats()));
        int col = row == 0 ? i : i - crewSeats();
        double x = (col - (perRow - 1) / 2.0) * 0.8;
        return new Vec3(x, 1.0, row == 0 ? 1.0 : -0.5);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isPassenger() && canAddPassenger(player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        openCustomInventoryScreen(sp);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Inventartaste im Sitz: Sprungziele (nur Besatzung). */
    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!isCrew(sp)) {
            sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.starship.crew_only"));
            return;
        }
        StarshipService.openTargets(sp, this);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.storeNullable("owner_agency", UUIDUtil.CODEC, owner);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        owner = in.read("owner_agency", UUIDUtil.CODEC).orElse(null);
    }
}
