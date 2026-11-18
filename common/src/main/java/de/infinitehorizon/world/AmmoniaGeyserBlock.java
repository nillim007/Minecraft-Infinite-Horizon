package de.infinitehorizon.world;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@code ammonia_geyser} (13.4, Enceladus): unzerstörbar; alle {@code eruption_interval_ticks} eine Fontäne
 * {@code fountain_height} Blöcke hoch. Entities darin steigen mit {@code entity_velocity_up} Blöcken je Tick, solange die
 * Fontäne läuft ({@code fountain_height} / {@code entity_velocity_up} Ticks). Werte aus items.json.
 */
public class AmmoniaGeyserBlock extends BaseEntityBlock {

    private final Supplier<? extends BlockEntityType<?>> type;

    public AmmoniaGeyserBlock(Properties properties, Supplier<? extends BlockEntityType<?>> type) {
        super(properties);
        this.type = type;
    }

    public static JsonObject config() {
        return BalanceManager.getOrBundled().entry("items", InfiniteHorizon.id("ammonia_geyser").toString()).orElse(new JsonObject());
    }

    /** Dauer einer Fontäne in Ticks: Höhe ÷ Steiggeschwindigkeit. */
    public static int fountainTicks() {
        JsonObject c = config();
        double v = c.has("entity_velocity_up") ? c.get("entity_velocity_up").getAsDouble() : 0;
        return v <= 0 ? 0 : (int) Math.ceil(c.get("fountain_height").getAsDouble() / v);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide()) {
            return null;
        }
        return (l, pos, st, be) -> {
            long interval = Math.max(1, config().get("eruption_interval_ticks").getAsLong());
            // jeder Geysir mit eigenem Versatz, damit nicht alle gleichzeitig ausbrechen
            long phase = Math.floorMod(l.getGameTime() + pos.asLong(), interval);
            if (phase < fountainTicks()) {
                erupt((ServerLevel) l, pos, phase == 0);
            }
        };
    }

    /** Ein Tick der Fontäne: Entities in der Säule steigen; Partikel beim Start und laufend. */
    public static List<Entity> erupt(ServerLevel level, BlockPos pos, boolean start) {
        JsonObject c = config();
        double height = c.get("fountain_height").getAsDouble();
        double v = c.get("entity_velocity_up").getAsDouble();
        AABB column = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + height, pos.getZ() + 1)
                .inflate(0.3, 0, 0.3);
        List<Entity> hit = level.getEntities((Entity) null, column, e -> !e.isSpectator());
        for (Entity e : hit) {
            Vec3 d = e.getDeltaMovement();
            if (d.y < v) {
                e.setDeltaMovement(d.x, v, d.z);
                e.needsSync = true;
                e.syncVelocity = true; // 26.3: nur so erfährt auch der Spieler selbst die neue Geschwindigkeit
                e.resetFallDistance();
            }
        }
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, start ? 20 : 4,
                0.2, 0.5, 0.2, 0.3);
        return hit;
    }

    /** Leere Block-Entity für den Takt. */
    public static class TickerEntity extends BlockEntity {
        public TickerEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
