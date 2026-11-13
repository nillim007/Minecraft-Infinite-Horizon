package de.infinitehorizon.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.environment.PlanetResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

/**
 * Monster in Planetendimensionen (13.5): Die Biome tragen die Spawngewichte der Overworld; {@code rate} aus
 * {@code vanilla_mobs} ist die Wahrscheinlichkeit, dass ein natürlicher Spawn bestehen bleibt. Mit der Config
 * {@code planet_mobs = false} spawnen dort keine Monster. Die Overworld und Dimensionen ohne Eintrag bleiben unberührt.
 */
public final class PlanetMobRules {

    public static final String CONFIG_KEY = "planet_mobs";

    private PlanetMobRules() {
    }

    /** Bleibt dieser natürliche Spawn bestehen? */
    public static boolean keep(ServerLevel level, Mob mob) {
        if (level.dimension().identifier().equals(PlanetResolver.EARTH)) {
            return true;
        }
        Planet planet = PlanetRegistry.get().planet(level.dimension().identifier()).orElse(null);
        if (planet == null) {
            return true;
        }
        boolean monster = mob.getType().getCategory() == MobCategory.MONSTER;
        if (monster && !ServerConfig.get().getBoolean(CONFIG_KEY)) {
            return false;
        }
        double rate = rate(planet, BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())
                * de.infinitehorizon.event.EventService.monsterMultiplier(level);
        return rate >= 1.0 || level.getRandom().nextDouble() < rate;
    }

    /** {@code rate} der Art aus {@code vanilla_mobs}, sonst 1 (Spawn bleibt). */
    static double rate(Planet planet, String entity) {
        JsonElement list = planet.raw().get("vanilla_mobs");
        if (list == null || !list.isJsonArray()) {
            return 1.0;
        }
        for (JsonElement e : list.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if (entity.equals(o.get("entity").getAsString())) {
                return o.get("rate").getAsDouble();
            }
        }
        return 1.0;
    }
}
