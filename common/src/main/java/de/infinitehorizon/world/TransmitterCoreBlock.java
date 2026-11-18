package de.infinitehorizon.world;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.creature.CreatureRules;
import de.infinitehorizon.creature.SignalData;
import de.infinitehorizon.creature.SignalSentinel;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * {@code transmitter_core} (13.4): unzerstörbar; Rechtsklick beschwört den {@code signal_sentinel}, wenn für die Agentur
 * keiner lebt und keiner in den letzten {@code resummon_cooldown_ticks} besiegt wurde.
 */
public class TransmitterCoreBlock extends Block {

    /** Der Wächter erscheint so viele Blöcke nördlich des Kerns in der Halle (SPEC-GAP #108). */
    private static final int SPAWN_OFFSET = 4;

    public TransmitterCoreBlock(Properties properties) {
        super(properties);
    }

    private static long cooldown() {
        JsonObject s = CreatureRules.entry(InfiniteHorizon.id("signal_sentinel"));
        JsonObject spawn = s.has("spawn") ? s.getAsJsonObject("spawn") : new JsonObject();
        return spawn.has("resummon_cooldown_ticks") ? spawn.get("resummon_cooldown_ticks").getAsLong() : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            summon(sl, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** Beschwört den Wächter für die Agentur des Spielers oder meldet, warum nicht. */
    public static @Nullable SignalSentinel summon(ServerLevel level, BlockPos core, ServerPlayer player) {
        Agency agency = AgencyManager.ensureFor(player);
        SignalData data = SignalData.get(level.getServer());
        long now = level.getServer().overworld().getGameTime();
        if (!data.canSummon(agency.id(), now, cooldown())) {
            player.sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".sentinel.not_ready"));
            return null;
        }
        SignalSentinel s = ModEntities.SIGNAL_SENTINEL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (s == null) {
            return null;
        }
        BlockPos at = core.north(SPAWN_OFFSET);
        s.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.setSummoner(agency.id());
        level.addFreshEntity(s);
        data.summoned(agency.id(), s.getUUID());
        player.sendSystemMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".sentinel.summoned"));
        return s;
    }
}
