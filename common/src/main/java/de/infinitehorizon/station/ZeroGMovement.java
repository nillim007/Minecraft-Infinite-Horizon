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
        // Asteroidenanker (13.4): im Umkreis gravity_g, auf jedem Körper
        if (AsteroidAnchorBlock.covered(player.level(), player.position())) {
            JsonObject anchor = AsteroidAnchorBlock.config();
            return anchor.has("gravity_g") ? anchor.get("gravity_g").getAsDouble() : planetG;
        }
        // Titan (13.4): dichte Luft, beim Gleiten mit Elytren sinkt man langsamer
        if (planet != null && player.isFallFlying() && planet.raw().has("special") && planet.raw().get("special").isJsonObject()) {
            JsonObject special = planet.raw().getAsJsonObject("special");
            if (special.has("elytra_sink_rate_multiplier")) {
                return planetG * special.get("elytra_sink_rate_multiplier").getAsDouble();
            }
        }
        if (planet == null || !planet.isOrbit()) {
            return planetG;
        }
        JsonObject boots = BalanceManager.get().entry("suits", MAGNET_BOOTS.toString()).orElse(null);
        boolean wearing = boots != null && SuitModules.modules(player.getItemBySlot(EquipmentSlot.FEET)).contains(MAGNET_BOOTS);
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        // Schwerkraftplatte (12.9): im Wirkbereich gilt gravity_g
        if (GravityPlateBlockEntity.covered(player.level(), player.position())) {
            JsonObject plate = StationModules.config("gravity_plate");
            return plate.has("gravity_g") ? plate.get("gravity_g").getAsDouble() : planetG;
        }
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
        List<ServerPlayer> reset = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) {
                continue;
            }
            Planet planet = PlanetResolver.at(p.level(), p.blockPosition()).planet();
            JsonObject vr = voidReset(planet);
            if (vr != null && p.getY() < vr.get("below_y").getAsInt()) {
                reset.add(p);
            } else if (p.getY() < trigger && planet != null && planet.isOrbit()) {
                falling.add(p);
            }
        }
        falling.forEach(ZeroGMovement::fall);
        reset.forEach(ZeroGMovement::resetToSpawnAsteroid);
    }

    /** {@code special.void_reset} des Planeten (13.4 Asteroidengürtel), sonst null. */
    public static @Nullable JsonObject voidReset(@Nullable Planet planet) {
        if (planet == null || !planet.raw().has("special") || !planet.raw().get("special").isJsonObject()) {
            return null;
        }
        JsonObject s = planet.raw().getAsJsonObject("special");
        return s.has("void_reset") && s.get("void_reset").isJsonObject() ? s.getAsJsonObject("void_reset") : null;
    }

    /** Mitte des Spawn-Asteroiden (Struktur mit {@code position}) oder 0/80/0. */
    public static BlockPos spawnAsteroid(@Nullable Planet planet) {
        if (planet != null && planet.raw().has("structures") && planet.raw().get("structures").isJsonArray()) {
            for (var e : planet.raw().getAsJsonArray("structures")) {
                JsonObject st = e.getAsJsonObject();
                if (st.has("position") && st.get("position").isJsonObject()) {
                    JsonObject p = st.getAsJsonObject("position");
                    return new BlockPos(p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("z").getAsInt());
                }
            }
        }
        return new BlockPos(0, 80, 0);
    }

    /** Fall in die Leere (13.4): zurück auf den Spawn-Asteroiden mit Langsamem Fall. */
    public static void resetToSpawnAsteroid(ServerPlayer player) {
        ServerLevel level = player.level();
        Planet planet = PlanetResolver.at(level, player.blockPosition()).planet();
        JsonObject vr = voidReset(planet);
        BlockPos c = spawnAsteroid(planet);
        level.getChunk(c.getX() >> 4, c.getZ() >> 4);
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, c.getX(), c.getZ());
        BlockPos at = top > level.getMinY() + 1 ? new BlockPos(c.getX(), top, c.getZ()) : c.above(20);
        player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.resetFallDistance();
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (vr != null && vr.has("slow_falling_ticks")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, vr.get("slow_falling_ticks").getAsInt()));
        }
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

    /**
     * Fall unter y 0 im Orbit eines Gasriesen (14.3): auf den nächsten Stationskern der Dimension, sonst auf eine neue
     * Plattform ({@code fallback_platform}) an {@code fallback_position}; dazu {@code damage} Schaden.
     */
    static boolean fallToStation(ServerPlayer player, JsonObject giants) {
        ServerLevel level = player.level();
        StationData.Core core = StationData.get(level).nearest(player.blockPosition(), Integer.MAX_VALUE);
        BlockPos at;
        if (core != null) {
            at = core.pos().above();
        } else {
            var fb = giants.getAsJsonArray("fallback_position");
            at = new BlockPos(fb.get(0).getAsInt(), fb.get(1).getAsInt(), fb.get(2).getAsInt());
            JsonObject plat = giants.getAsJsonObject("fallback_platform");
            int half = plat.get("size").getAsInt() / 2;
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse(plat.get("block").getAsString()))
                    .defaultBlockState();
            if (block.hasProperty(StationHullBlock.GENERATED)) {
                block = block.setValue(StationHullBlock.GENERATED, true);
            }
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockPos p = at.offset(dx, -1, dz);
                    if (level.getBlockState(p).isAir()) {
                        level.setBlockAndUpdate(p, block);
                    }
                }
            }
        }
        player.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(at), Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        if (giants.has("damage")) {
            player.hurtServer(level, level.damageSources().fall(), giants.get("damage").getAsFloat());
        }
        return true;
    }

    /** Fall unter y 0 (14.3): Planeten mit Oberfläche → gleiche X/Z dort (Venus: Wolkenschicht) mit Langsamem Fall. */
    public static boolean fall(ServerPlayer player) {
        Identifier orbit = player.level().dimension().identifier();
        JsonObject surface = fallRules().has("surface_planet_orbits") ? fallRules().getAsJsonObject("surface_planet_orbits") : null;
        if (!listed(surface, orbit)) {
            JsonObject giants = fallRules().has("gas_giant_orbits") ? fallRules().getAsJsonObject("gas_giant_orbits") : null;
            return listed(giants, orbit) && fallToStation(player, giants);
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
