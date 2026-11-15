package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Asteroidenanker (13.4): steht auf einem Asteroiden und setzt im Umkreis {@code radius} (items.json) für Spieler die
 * Schwerkraft {@code gravity_g}. Ohne Strom; geladene Anker melden sich jeden Tick (Laufzeitdaten, nicht gespeichert).
 */
public class AsteroidAnchorBlock extends BaseEntityBlock {

    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> ANCHORS = new ConcurrentHashMap<>();

    private final Supplier<? extends BlockEntityType<?>> type;

    public AsteroidAnchorBlock(Properties properties, Supplier<? extends BlockEntityType<?>> type) {
        super(properties);
        this.type = type;
    }

    public static JsonObject config() {
        return BalanceManager.get().entry("items", InfiniteHorizon.id("asteroid_anchor").toString()).orElse(new JsonObject());
    }

    /** Nur auf Asteroidengestein oder -erz (13.4: „Block auf einem Asteroiden“). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.below()).getBlock()).getPath().startsWith("asteroid_");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return level.isClientSide() ? null : (l, pos, st, be) -> ANCHORS.computeIfAbsent(l.dimension(),
                k -> new ConcurrentHashMap<>()).put(pos.immutable(), l.getGameTime());
    }

    /** Liegt {@code pos} im Umkreis eines geladenen Ankers dieser Dimension? */
    public static boolean covered(ServerLevel level, Vec3 pos) {
        Map<BlockPos, Long> m = ANCHORS.get(level.dimension());
        if (m == null || m.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        m.values().removeIf(t -> t < now - 1);
        double r = config().has("radius") ? config().get("radius").getAsDouble() : 0;
        for (BlockPos p : m.keySet()) {
            if (Vec3.atCenterOf(p).distanceToSqr(pos) <= r * r) {
                return true;
            }
        }
        return false;
    }

    /** Leere Block-Entity nur für den Ticker. */
    public static class Entity extends BlockEntity {
        public Entity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
