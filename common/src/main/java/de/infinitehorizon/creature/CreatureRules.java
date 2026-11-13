package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;

/**
 * Werte der Kreaturen (18, creatures.json): Attribute, Verhaltensparameter und eigene Spawnregeln statt der
 * Vanilla-Bodenregeln (feindlich: Licht ≤ {@code monster_light_max}; sonst jeder feste Planetenblock).
 */
public final class CreatureRules {

    private CreatureRules() {
    }

    public static JsonObject entry(Identifier id) {
        return BalanceManager.getOrBundled().entry("creatures", id.toString()).orElseGet(JsonObject::new);
    }

    public static JsonObject entry(EntityType<?> type) {
        return entry(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static JsonObject params(EntityType<?> type) {
        JsonObject e = entry(type);
        return e.has("behavior_params") ? e.getAsJsonObject("behavior_params") : new JsonObject();
    }

    public static double num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsDouble() : 0;
    }

    /** Attribute aus {@code hp}, {@code armor}, {@code damage}, {@code speed}. */
    public static AttributeSupplier.Builder attributes(Identifier id) {
        JsonObject e = entry(id);
        AttributeSupplier.Builder b = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Math.max(1, num(e, "hp")))
                .add(Attributes.ARMOR, num(e, "armor"))
                .add(Attributes.MOVEMENT_SPEED, num(e, "speed"))
                .add(Attributes.TEMPT_RANGE);
        if (num(e, "damage") > 0) {
            b.add(Attributes.ATTACK_DAMAGE, num(e, "damage"));
        }
        if (CreatureRules.params(id).has("flying")) {
            b.add(Attributes.FLYING_SPEED, num(e, "speed"));
        }
        return b;
    }

    private static JsonObject params(Identifier id) {
        JsonObject e = entry(id);
        return e.has("behavior_params") ? e.getAsJsonObject("behavior_params") : new JsonObject();
    }

    /** Spawnregel (18): fester Block darunter in einer Planetendimension; Feinde nur bei Licht ≤ Grenze. */
    public static boolean canSpawn(EntityType<?> type, LevelAccessor level, BlockPos pos) {
        JsonObject e = entry(type);
        JsonObject spawn = e.has("spawn") ? e.getAsJsonObject("spawn") : new JsonObject();
        if (spawn.has("weight") && num(spawn, "weight") <= 0) {
            return false;
        }
        boolean flying = params(type).has("flying") || params(type).has("floating");
        if (!flying && !level.getBlockState(pos.below()).isSolid()) {
            return false;
        }
        if ("monster".equals(e.has("category") ? e.get("category").getAsString() : "")) {
            JsonObject rules = BalanceManager.getOrBundled().entry("creatures", "spawn_rules").orElseGet(JsonObject::new);
            return level.getMaxLocalRawBrightness(pos) <= num(rules, "monster_light_max");
        }
        return true;
    }

    /** Planet der Dimension (für Spawns außerhalb der Planeten: keine Kreaturen). */
    static boolean onPlanet(net.minecraft.world.level.Level level) {
        return PlanetResolver.planetFor(level.dimension().identifier(), level.isClientSide() ? PlanetRegistry.client()
                : PlanetRegistry.get()) != null;
    }
}
