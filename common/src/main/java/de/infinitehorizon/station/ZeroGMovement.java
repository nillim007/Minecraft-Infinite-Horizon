package de.infinitehorizon.station;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.space.CloudLayer;
import de.infinitehorizon.space.LandingService;
import de.infinitehorizon.suit.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Schwerelosigkeit (14.3): Magnetstiefel und Fall unter y 0 in Orbit-Dimensionen. */
public final class ZeroGMovement {

    public static final Identifier MAGNET_BOOTS = InfiniteHorizon.id("magnet_boots");

    private ZeroGMovement() {
    }

    /** Wirksames g für den Spieler: Magnetstiefel auf festem Grund im Orbit (12.9). */
    public static double gravity(ServerPlayer player, @Nullable Planet planet, double planetG) {
        if (planet == null || !planet.isOrbit()) {
            return planetG;
        }
        JsonObject boots = BalanceManager.get().entry("suits", MAGNET_BOOTS.toString()).orElse(null);
        boolean wearing = boots != null && SuitModules.modules(player.getItemBySlot(EquipmentSlot.FEET)).contains(MAGNET_BOOTS);
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        return StationRules.gravity(planetG, true, wearing, !player.level().getBlockState(below).isAir(),
                wearing ? boots.get("gravity_g").getAsDouble() : planetG);
    }

    private static JsonObject fallRules() {
        return BalanceManager.get().entry("stations", "orbit_fall_rules").orElse(new JsonObject());
    }

    /** Jeder Server-Tick: Spieler unter {@code trigger_below_y} in einem Orbit fallen auf den Planeten darunter. */
    public static void tick(MinecraftServer server) {
        JsonObject rules = fallRules();
        if (!rules.has("trigger_below_y")) {
            return;
        }
        int trigger = rules.get("trigger_below_y").getAsInt();
        List<ServerPlayer> falling = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getY() < trigger && !p.isSpectator()) {
                Planet planet = PlanetResolver.at(p.level(), p.blockPosition()).planet();
                if (planet != null && planet.isOrbit()) {
                    falling.add(p);
                }
            }
        }
        falling.forEach(ZeroGMovement::fall);
    }

    private static boolean listed(JsonObject group, Identifier orbit) {
        if (group == null || !group.has("orbits")) {
            return false;
        }
        for (JsonElement e : group.getAsJsonArray("orbits")) {
            if (e.getAsString().equals(orbit.toString())) {
                return true;
            }
        }
        return false;
    }

    /** Fall unter y 0 (14.3): Planeten mit Oberfläche → gleiche X/Z dort (Venus: Wolkenschicht) mit Langsamem Fall. */
    public static boolean fall(ServerPlayer player) {
        Identifier orbit = player.level().dimension().identifier();
        JsonObject surface = fallRules().has("surface_planet_orbits") ? fallRules().getAsJsonObject("surface_planet_orbits") : null;
        if (!listed(surface, orbit)) {
            return false; // SPEC-GAP #96: Gasriesen-Orbits kommen mit M7
        }
        String below = StationRules.surfaceBelow(orbit.getPath());
        Identifier target = below.contains(":") ? Identifier.parse(below) : Identifier.fromNamespaceAndPath(orbit.getNamespace(), below);
        ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, target));
        if (level == null) {
            return false;
        }
        int x = player.getBlockX();
        int z = player.getBlockZ();
        BlockPos point = CloudLayer.hasLayer(level) ? CloudLayer.landingPoint(level, x, z) : LandingService.find(level, x, z);
        player.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(point), Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        JsonObject slow = BalanceManager.get().entry("misc", "slow_fall").orElse(null);
        if (slow != null && surface.has("slow_falling") && surface.get("slow_falling").getAsBoolean()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slow.get("ticks").getAsInt()));
        }
        return true;
    }
}
