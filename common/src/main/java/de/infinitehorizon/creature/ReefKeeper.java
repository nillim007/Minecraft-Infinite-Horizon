package de.infinitehorizon.creature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code reef_keeper} (18): neutraler Wächter eines Quellenfelds. Wird feindlich gegen Spieler, die im Umkreis
 * {@code aggro_radius} Blöcke abbauen oder Proben nehmen ({@code aggro_triggers}), außer sie haben ihn mit
 * {@code pacify_count} {@code pacify_item} gefüttert: dann {@code pacify_ticks} friedlich zu diesem Spieler.
 */
public class ReefKeeper extends WaterAnimal {

    public static final String BLOCK_BREAK = "block_break";
    public static final String SAMPLE_TOOL = "sample_tool";
    /** Schwimm-KI wie der Delfin (Drehrate, Höchstwinkel). */
    private static final int MAX_TURN_X = 85;
    private static final int MAX_TURN_Y = 10;
    private static final Codec<Map<UUID, Long>> PACIFIED_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG);

    private final Map<UUID, Long> pacified = new HashMap<>();

    public ReefKeeper(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl(this, MAX_TURN_X, MAX_TURN_Y, 0.02f, 0.1f, true);
        lookControl = new SmoothSwimmingLookControl(this, MAX_TURN_Y);
    }

    private JsonObject params() {
        return CreatureRules.params(getType());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 40));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /** Ist der Wächter zu diesem Spieler gerade friedlich (gefüttert)? */
    public boolean pacifiedFor(Player player) {
        Long until = pacified.get(player.getUUID());
        return until != null && level().getGameTime() < until;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        JsonObject p = params();
        ItemStack stack = player.getItemInHand(hand);
        if (!p.has("pacify_item") || !stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(p.get("pacify_item").getAsString())))
                || stack.getCount() < CreatureRules.num(p, "pacify_count")) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            stack.consume((int) CreatureRules.num(p, "pacify_count"), player);
            pacified.put(player.getUUID(), level().getGameTime() + (long) CreatureRules.num(p, "pacify_ticks"));
            if (getTarget() == player) {
                setTarget(null);
            }
            player.sendOverlayMessage(Component.translatable("message.infinite_horizon.reef_keeper.calm"));
        }
        return InteractionResult.SUCCESS;
    }

    private boolean triggeredBy(String trigger) {
        JsonObject p = params();
        if (!p.has("aggro_triggers")) {
            return false;
        }
        for (JsonElement e : p.getAsJsonArray("aggro_triggers")) {
            if (e.getAsString().equals(trigger)) {
                return true;
            }
        }
        return false;
    }

    /** Ein Spieler baut bei {@code pos} ab oder nimmt eine Probe: Wächter im Umkreis greifen ihn an (18). */
    public static void provoke(ServerLevel level, Player player, BlockPos pos, String trigger) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        double r = CreatureRules.num(CreatureRules.params(de.infinitehorizon.registry.ModEntities.REEF_KEEPER.get()), "aggro_radius");
        for (ReefKeeper k : level.getEntitiesOfClass(ReefKeeper.class, new AABB(pos).inflate(r))) {
            if (k.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= r * r && k.triggeredBy(trigger)
                    && !k.pacifiedFor(player)) {
                k.setTarget(player);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (getTarget() instanceof Player p && (pacifiedFor(p) || p.isCreative() || p.isSpectator())) {
            setTarget(null);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player p && !p.isCreative()) {
            setTarget(p); // neutral: wehrt sich gegen Angreifer
        }
        return hurt;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("pacified", PACIFIED_CODEC, pacified);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        pacified.clear();
        input.read("pacified", PACIFIED_CODEC).ifPresent(pacified::putAll);
    }
}
