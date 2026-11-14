package de.infinitehorizon.environment;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Schwerkraft (12.9) als Attribut-Modifikatoren mit fester ID {@code infinite_horizon:planet_gravity} (nie den
 * Basiswert ändern): {@code gravity} und {@code fall_damage_multiplier} mit g − 1 ({@code add_multiplied_total}),
 * {@code safe_fall_distance} mit 3 / max(g, 0,05) − 3 ({@code add_value}). Angewendet beim Dimensionswechsel,
 * Respawn, Login und alle {@code update_interval_ticks}. Werte aus misc.json ({@code gravity}).
 */
public final class GravityService {

    /** Regeln aus misc.json. */
    public record Rules(Identifier modifierId, int intervalTicks, double safeFallNumerator, double safeFallMinG) {

        static Rules from(BalanceManager balance) {
            JsonObject g = balance.entry("misc", "gravity").orElseThrow(() -> new IllegalStateException("misc.json: gravity fehlt"));
            return new Rules(Identifier.parse(g.get("modifier_id").getAsString()), g.get("update_interval_ticks").getAsInt(),
                    g.get("safe_fall_distance_numerator").getAsDouble(), g.get("safe_fall_distance_min_g").getAsDouble());
        }
    }

    private static BalanceManager rulesSource;
    private static Rules rules;

    private GravityService() {
    }

    public static Rules rules() {
        BalanceManager balance = BalanceManager.get();
        if (rules == null || rulesSource != balance) {
            rules = Rules.from(balance);
            rulesSource = balance;
        }
        return rules;
    }

    /** Werte der drei Modifikatoren für g (reine Funktion): {gravity, fall_damage_multiplier, safe_fall_distance}. */
    public static double[] modifierAmounts(double g, Rules r) {
        double safeFall = r.safeFallNumerator() / Math.max(g, r.safeFallMinG()) - r.safeFallNumerator();
        return new double[]{g - 1, g - 1, safeFall};
    }

    /** Alle {@code update_interval_ticks}: Schwerkraft aller Spieler. */
    public static void tick(MinecraftServer server) {
        if (BalanceManager.get().isEmpty()) {
            return;
        }
        Rules r = rules();
        if (server.getTickCount() % r.intervalTicks() != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(GravityService::apply);
    }

    /** Schwerkraft des Planeten an der Position des Spielers setzen (Login, Respawn, Dimensionswechsel, Takt). */
    public static void apply(ServerPlayer player) {
        if (BalanceManager.get().isEmpty()) {
            return;
        }
        Planet planet = PlanetResolver.at(player.level(), player.blockPosition()).planet();
        double g = planet == null ? 1.0 : planet.gravity();
        apply(player, de.infinitehorizon.station.ZeroGMovement.gravity(player, planet, g), rules());
    }

    public static void apply(LivingEntity entity, double g, Rules r) {
        double[] amounts = modifierAmounts(g, r);
        set(entity, Attributes.GRAVITY, r.modifierId(), amounts[0], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, g);
        set(entity, Attributes.FALL_DAMAGE_MULTIPLIER, r.modifierId(), amounts[1], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, g);
        set(entity, Attributes.SAFE_FALL_DISTANCE, r.modifierId(), amounts[2], AttributeModifier.Operation.ADD_VALUE, g);
    }

    private static void set(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount,
                            AttributeModifier.Operation op, double g) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        if (g == 1.0) {
            inst.removeModifier(id);
            return;
        }
        AttributeModifier existing = inst.getModifier(id);
        if (existing == null || existing.amount() != amount || existing.operation() != op) {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }
}
