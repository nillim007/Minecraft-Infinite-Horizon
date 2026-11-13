package de.infinitehorizon.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Unsichtbarer Sitz in der Kapsel (12.5): Rechtsklick auf den Kapselblock setzt den Spieler darauf. Ohne Passagier oder
 * ohne Kapselblock verschwindet er. Er wird nicht gespeichert ({@link #shouldBeSaved()}); {@code noSave()} am Typ
 * würde das Reiten verhindern (api-entities-26.3.md, Abschnitt 3).
 */
public class SeatEntity extends Entity {

    private BlockPos anchor = BlockPos.ZERO;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    /** Kapselblock, zu dem der Sitz gehört. */
    public BlockPos anchor() {
        return anchor;
    }

    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor.immutable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && (getPassengers().isEmpty()
                || !(sl.getBlockState(anchor).getBlock() instanceof de.infinitehorizon.space.CapsuleBlock))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    /** Sitzt jemand darauf? */
    public boolean occupied() {
        return !getPassengers().isEmpty();
    }
}
