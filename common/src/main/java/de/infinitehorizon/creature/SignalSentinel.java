package de.infinitehorizon.creature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.item.StarChartItem;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * {@code signal_sentinel} (18): Boss mit Bossleiste und Phasen aus creatures.json ({@code phases}). Phase 1: Nahkampf und
 * Schockwelle (Rückstoß im Umkreis {@code knockback_radius} alle {@code interval_ticks}); Phase 2: beim Eintritt
 * {@code summon}, dazu ein Strahl wie beim Wächter ({@code damage} nach {@code duration_ticks} Sichtkontakt). Gehört zur
 * Agentur des Beschwörers; besiegt droppt er die Sternenkarte, wenn die Agentur keine gültige hat.
 */
public class SignalSentinel extends PlanetCreature {

    /** Stärke der Schockwelle und Reichweite des Strahls (SPEC-GAP #108). */
    private static final double SHOCKWAVE_PUSH = 1.5;
    private static final double BEAM_RANGE = 16;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable(
            "entity." + InfiniteHorizon.MOD_ID + ".signal_sentinel"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private @Nullable UUID summoner;
    private int phase = 1;
    private int shockwaveTimer;
    private int beamTicks;

    public SignalSentinel(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setSummoner(UUID agency) {
        this.summoner = agency;
    }

    public @Nullable UUID summoner() {
        return summoner;
    }

    public int phase() {
        return phase;
    }

    public int beamTicks() {
        return beamTicks;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, (float) BEAM_RANGE));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    private JsonObject phaseConfig(int n) {
        JsonObject e = CreatureRules.entry(getType());
        if (e.has("phases")) {
            for (JsonElement p : e.getAsJsonArray("phases")) {
                if (p.getAsJsonObject().get("phase").getAsInt() == n) {
                    return p.getAsJsonObject();
                }
            }
        }
        return new JsonObject();
    }

    /** Phase zum Anteil der Lebenspunkte ({@code hp_from} ≥ Anteil > {@code hp_to}). */
    int phaseFor(float ratio) {
        JsonObject e = CreatureRules.entry(getType());
        if (e.has("phases")) {
            for (JsonElement p : e.getAsJsonArray("phases")) {
                JsonObject o = p.getAsJsonObject();
                if (ratio <= o.get("hp_from").getAsDouble() && ratio > o.get("hp_to").getAsDouble()) {
                    return o.get("phase").getAsInt();
                }
            }
        }
        return phase;
    }

    private @Nullable JsonObject attack(JsonObject phaseCfg, String type) {
        if (!phaseCfg.has("attacks")) {
            return null;
        }
        for (JsonElement a : phaseCfg.getAsJsonArray("attacks")) {
            if (type.equals(a.getAsJsonObject().get("type").getAsString())) {
                return a.getAsJsonObject();
            }
        }
        return null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossBar.setProgress(getHealth() / getMaxHealth());
        int now = phaseFor(getHealth() / getMaxHealth());
        if (now != phase) {
            phase = now;
            enterPhase(level, phaseConfig(phase));
        }
        JsonObject cfg = phaseConfig(phase);
        JsonObject shock = attack(cfg, "shockwave");
        if (shock != null && ++shockwaveTimer >= shock.get("interval_ticks").getAsInt()) {
            shockwaveTimer = 0;
            shockwave(level, shock.get("knockback_radius").getAsDouble());
        }
        JsonObject beam = attack(cfg, "beam");
        LivingEntity target = getTarget();
        if (beam != null && target != null && target.isAlive() && distanceTo(target) <= BEAM_RANGE && hasLineOfSight(target)) {
            beamTicks++;
            if (beamTicks % 4 == 0) {
                Vec3 from = getEyePosition();
                Vec3 step = target.getEyePosition().subtract(from).scale(0.1);
                for (int k = 1; k <= 10; k++) {
                    Vec3 at = from.add(step.scale(k));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                }
            }
            if (beamTicks >= beam.get("duration_ticks").getAsInt()) {
                beamTicks = 0;
                target.hurtServer(level, damageSources().indirectMagic(this, this), beam.get("damage").getAsFloat());
            }
        } else {
            beamTicks = 0;
        }
    }

    /** Phasenwechsel: Beschwörung aus {@code summon} (z. B. 3 Frostkriecher, sofort sichtbar). */
    private void enterPhase(ServerLevel level, JsonObject cfg) {
        if (!cfg.has("summon")) {
            return;
        }
        JsonObject s = cfg.getAsJsonObject("summon");
        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(
                net.minecraft.resources.Identifier.parse(s.get("entity").getAsString()));
        for (int k = 0; k < s.get("count").getAsInt(); k++) {
            var e = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (e == null) {
                continue;
            }
            double a = Math.PI * 2 * k / Math.max(1, s.get("count").getAsInt());
            e.snapTo(getX() + Math.cos(a) * 2, getY(), getZ() + Math.sin(a) * 2, 0, 0);
            if (e instanceof FrostLurker l) {
                l.emerge();
            }
            level.addFreshEntity(e);
        }
    }

    /** Stößt alle Spieler im Umkreis weg. */
    public void shockwave(ServerLevel level, double radius) {
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(radius),
                p -> p.distanceTo(this) <= radius && !p.isCreative() && !p.isSpectator())) {
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            Vec3 push = (away.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : away.normalize()).scale(SHOCKWAVE_PUSH);
            p.setDeltaMovement(p.getDeltaMovement().add(push.x, SHOCKWAVE_PUSH / 3, push.z));
            p.needsSync = true;
            p.syncVelocity = true; // 26.3: nur so erfährt auch der Spieler selbst die neue Geschwindigkeit
        }
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 6, radius / 3, 0.2, radius / 3, 0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (summoner == null) {
            return;
        }
        SignalData data = SignalData.get(level.getServer());
        data.defeated(summoner, level.getServer().overworld().getGameTime());
        if (data.chart(summoner) == null) {
            UUID chart = UUID.randomUUID();
            ItemStack stack = new ItemStack(ModItems.STAR_CHART.get());
            stack.set(ModDataComponents.CHART_OWNER.get(), new StarChartItem.ChartOwner(summoner, chart));
            data.setChart(summoner, chart);
            spawnAtLocation(level, stack);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && summoner != null && reason.shouldDestroy() && !isDeadOrDying()
                && level() instanceof ServerLevel sl) {
            SignalData.get(sl.getServer()).vanished(summoner);
        }
        super.remove(reason);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (params().has("boss_bar") && params().get("boss_bar").getAsBoolean()) {
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("summoner", UUIDUtil.CODEC, summoner);
        output.putInt("phase", phase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        summoner = input.read("summoner", UUIDUtil.CODEC).orElse(null);
        phase = input.getIntOr("phase", 1);
    }
}
