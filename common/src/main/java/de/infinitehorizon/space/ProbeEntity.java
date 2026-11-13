package de.infinitehorizon.space;

import de.infinitehorizon.data.BalanceManager;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Startende Sonde (12.3): steigt {@code ascent_ticks} lang linear um {@code ascent_height} ({@code rocket_flight} in
 * spaceflight.json) und verschwindet. Reine Darstellung, der Flug selbst steht in {@code probe_flights} (23.1).
 */
public class ProbeEntity extends Entity {

    private double startY = Double.NaN;
    private int age;

    public ProbeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    private static int intValue(String key) {
        return BalanceManager.get().entry("spaceflight", "rocket_flight")
                .filter(o -> o.has(key)).map(o -> o.get(key).getAsInt()).orElse(0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        if (Double.isNaN(startY)) {
            startY = getY();
        }
        age++;
        int ticks = intValue("ascent_ticks");
        if (ticks <= 0 || age >= ticks) {
            discard();
            return;
        }
        setPos(getX(), startY + (double) intValue("ascent_height") * age / ticks, getZ());
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
}
