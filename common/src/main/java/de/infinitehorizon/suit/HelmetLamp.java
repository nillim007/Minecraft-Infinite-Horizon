package de.infinitehorizon.suit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Modul {@code helmet_lamp} (9.2): alle {@code update_interval_ticks} ein Vanilla-{@code light}-Block an der
 * Kopfposition, nur in den Blöcken aus {@code places_in} (Wasser als {@code waterlogged}); der alte verschwindet beim
 * Weitergehen, Absetzen, Tod, Abmelden und Dimensionswechsel. Werte aus {@code balance/items.json}.
 */
public final class HelmetLamp {

    public static final Identifier MODULE = InfiniteHorizon.id("helmet_lamp");

    private record Placed(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final Map<UUID, Placed> PLACED = new HashMap<>();

    private HelmetLamp() {
    }

    private static JsonObject rules() {
        return BalanceManager.get().entry("items", MODULE.toString()).orElseGet(JsonObject::new);
    }

    private static int interval(JsonObject r) {
        return r.has("update_interval_ticks") ? r.get("update_interval_ticks").getAsInt() : 0;
    }

    /** Lampe aktiv: lebender Spieler mit dem Modul im getragenen Helm. */
    public static boolean active(Player p) {
        return p.isAlive() && !p.isSpectator() && SuitModules.modules(p.getItemBySlot(EquipmentSlot.HEAD)).contains(MODULE);
    }

    /** Jeder Server-Tick; gearbeitet wird im Takt {@code update_interval_ticks}. */
    public static void tick(MinecraftServer server) {
        JsonObject r = rules();
        int interval = interval(r);
        if (interval <= 0 || server.getTickCount() % interval != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            update(p, r);
        }
        PLACED.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void update(ServerPlayer p, JsonObject r) {
        MinecraftServer server = p.level().getServer();
        Placed prev = PLACED.get(p.getUUID());
        if (!active(p)) {
            if (prev != null) {
                clear(server, PLACED.remove(p.getUUID()));
            }
            return;
        }
        ServerLevel level = p.level();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        if (prev != null && prev.dimension() == level.dimension() && prev.pos().equals(head)
                && level.getBlockState(head).is(Blocks.LIGHT)) {
            return;
        }
        if (prev != null) {
            clear(server, PLACED.remove(p.getUUID()));
        }
        BlockState light = lightFor(level, head, r);
        if (light != null) {
            level.setBlock(head, light, Block.UPDATE_ALL);
            PLACED.put(p.getUUID(), new Placed(level.dimension(), head.immutable()));
        }
    }

    /** Lichtblock für die Position oder null, wenn dort nichts entstehen darf ({@code places_in}). */
    private static BlockState lightFor(ServerLevel level, BlockPos pos, JsonObject r) {
        Set<Identifier> allowed = new HashSet<>();
        if (r.has("places_in")) {
            for (JsonElement e : r.getAsJsonArray("places_in")) {
                allowed.add(Identifier.parse(e.getAsString()));
            }
        }
        BlockState at = level.getBlockState(pos);
        if (!allowed.contains(BuiltInRegistries.BLOCK.getKey(at.getBlock()))) {
            return null;
        }
        boolean water;
        if (at.is(Blocks.WATER)) {
            // nur Quellen, sonst entstünde neues Wasser
            if (!level.getFluidState(pos).isSource()) {
                return null;
            }
            water = true;
        } else {
            water = at.is(Blocks.LIGHT) && at.getValue(BlockStateProperties.WATERLOGGED);
        }
        int lightLevel = r.has("light_level") ? r.get("light_level").getAsInt() : 0;
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel)
                .setValue(BlockStateProperties.WATERLOGGED, water);
    }

    private static void clear(MinecraftServer server, Placed placed) {
        ServerLevel level = server.getLevel(placed.dimension());
        if (level == null || !level.isLoaded(placed.pos())) {
            return;
        }
        BlockState s = level.getBlockState(placed.pos());
        if (s.is(Blocks.LIGHT)) {
            level.setBlock(placed.pos(), s.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Abmelden, Tod, Dimensionswechsel: alten Lichtblock sofort entfernen. */
    public static void remove(ServerPlayer player) {
        Placed prev = PLACED.remove(player.getUUID());
        if (prev != null) {
            clear(player.level().getServer(), prev);
        }
    }
}
