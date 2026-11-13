package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Venus-Wolkenschicht (12.8): Landeplattform aus {@code aerostat_platform} für Raketen und die Fahrten des
 * {@code cloud_elevator}. Werte in spaceflight.json ({@code venus_aerostat}, {@code cloud_elevator}); die Schicht ist
 * die Zone {@code cloud_layer} des Planeten.
 */
public final class CloudLayer {

    /** Zone der Wolkenschicht in planets.json. */
    public static final String ZONE = "cloud_layer";

    private record Ride(ResourceKey<Level> dimension, BlockPos station, long due) {
    }

    private static final Map<UUID, Ride> RIDES = new LinkedHashMap<>();

    private CloudLayer() {
    }

    private static JsonObject aerostat() {
        return BalanceManager.get().entry("spaceflight", "venus_aerostat").orElseGet(JsonObject::new);
    }

    private static JsonObject elevator() {
        return BalanceManager.get().entry("spaceflight", "cloud_elevator").orElseGet(JsonObject::new);
    }

    private static int num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsInt() : 0;
    }

    /** Hat der Planet dieser Dimension eine Wolkenschicht? */
    public static boolean hasLayer(ServerLevel level) {
        return PlanetRegistry.get().planet(level.dimension().identifier())
                .map(p -> p.zones().stream().anyMatch(z -> ZONE.equals(z.id()))).orElse(false);
    }

    /**
     * Landepunkt in der Wolkenschicht (12.8): oberster fester Block zwischen {@code check_y_min} und {@code check_y_max}
     * an X/Z; gibt es keinen, entsteht einmalig eine Plattform ({@code platform_size}) auf {@code platform_y}.
     */
    public static BlockPos landingPoint(ServerLevel level, int x, int z) {
        JsonObject a = aerostat();
        for (int y = num(a, "check_y_max"); y >= num(a, "check_y_min"); y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).isSolid()) {
                return p.above();
            }
        }
        int y = num(a, "platform_y");
        int half = num(a, "platform_size") / 2;
        BlockState block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(a.get("block").getAsString())).defaultBlockState();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                level.setBlock(new BlockPos(x + dx, y, z + dz), block, Block.UPDATE_ALL);
            }
        }
        return new BlockPos(x, y + 1, z);
    }

    /**
     * Ankunft unten (12.8): erster fester Block unter {@code surface_below_y} an X/Z ohne Fluid mit
     * {@code air_blocks_above} Luft darüber. Trifft die Suche zuerst auf Lava, entsteht dort eine Plattform
     * ({@code lava_platform_size}, {@code lava_platform_block}) auf der Lavaoberfläche. Rückgabe: Block der Station.
     */
    public static @Nullable BlockPos surfaceStation(ServerLevel level, int x, int z) {
        JsonObject e = elevator();
        int air = num(e, "air_blocks_above");
        for (int y = num(e, "surface_below_y") - 1; y > level.getMinY(); y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(p);
            if (!level.getFluidState(p).isEmpty()) {
                int half = num(e, "lava_platform_size") / 2;
                BlockState rock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(e.get("lava_platform_block").getAsString()))
                        .defaultBlockState();
                for (int dx = -half; dx <= half; dx++) {
                    for (int dz = -half; dz <= half; dz++) {
                        level.setBlock(p.offset(dx, 0, dz), rock, Block.UPDATE_ALL);
                    }
                }
                return p.above();
            }
            if (s.isSolid() && clear(level, p.above(), air)) {
                return p.above();
            }
        }
        return null;
    }

    private static boolean clear(ServerLevel level, BlockPos from, int count) {
        for (int i = 0; i < count; i++) {
            if (!level.getBlockState(from.above(i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Rechtsklick auf eine Station: Fahrt nach {@code delay_ticks}. */
    public static void requestRide(ServerLevel level, BlockPos station, ServerPlayer player) {
        RIDES.put(player.getUUID(), new Ride(level.dimension(), station.immutable(),
                level.getServer().getTickCount() + num(elevator(), "delay_ticks")));
        player.sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".cloud_elevator.start"));
    }

    /** Jeder Server-Tick: fällige Fahrten ausführen. */
    public static void tick(MinecraftServer server) {
        if (RIDES.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Ride>> it = RIDES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Ride> entry = it.next();
            if (entry.getValue().due() > server.getTickCount()) {
                continue;
            }
            it.remove();
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            if (p != null && level != null && p.level() == level) {
                ride(level, entry.getValue().station(), p);
            }
        }
    }

    /** Fahrt von {@code station} zur verknüpften Station; oben ohne Verknüpfung entsteht die untere Station. */
    public static boolean ride(ServerLevel level, BlockPos station, ServerPlayer player) {
        if (!(level.getBlockEntity(station) instanceof CloudElevatorBlockEntity from)) {
            return false;
        }
        BlockPos target = from.link();
        if (target == null || !(level.getBlockEntity(target) instanceof CloudElevatorBlockEntity)) {
            if (from.lower()) {
                return false;
            }
            target = surfaceStation(level, station.getX(), station.getZ());
            if (target == null) {
                return false;
            }
            level.setBlock(target, ModBlocks.CLOUD_ELEVATOR.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(target) instanceof CloudElevatorBlockEntity lowerBe) {
                lowerBe.setLink(station, true);
                lowerBe.setOwner(from.storedOwner());
            }
            from.setLink(target, false);
        }
        // Fahrzeuge im Umkreis vehicle_radius fahren mit (16), mit ihren Insassen
        double r = num(elevator(), "vehicle_radius");
        Vec3 arrive = Vec3.atBottomCenterOf(target.above());
        for (de.infinitehorizon.vehicle.RoverEntity rover : level.getEntitiesOfClass(de.infinitehorizon.vehicle.RoverEntity.class,
                new net.minecraft.world.phys.AABB(station).inflate(r))) {
            rover.teleportTo(arrive.x, arrive.y, arrive.z);
        }
        if (!player.isPassenger()) {
            player.teleport(new TeleportTransition(level, arrive, Vec3.ZERO, player.getYRot(), player.getXRot(),
                    TeleportTransition.DO_NOTHING));
        }
        return true;
    }
}
