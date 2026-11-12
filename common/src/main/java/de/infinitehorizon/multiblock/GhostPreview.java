package de.infinitehorizon.multiblock;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Geisterprojektion der fehlenden Multiblock-Blöcke (4.1): Sneak-Rechtsklick mit leerer Hand auf den Controller zeigt
 * {@code ghost_projection_ticks} lang Partikel an jedem fehlenden Platz. Rein clientseitig: das Muster kommt aus den
 * Multiblock-Daten ({@link BalanceManager#getOrBundled()}) und wird mit der Client-Welt verglichen; kein Payload.
 */
public final class GhostPreview {

    // Platzhalterfarbe der Partikel (rot wie die Statuslampe „error“) und Abstand der Partikelwellen in Ticks
    private static final int COLOR = 0xE04030;
    private static final float SCALE = 1.5f;
    private static final int WAVE_TICKS = 4;

    private static final Map<BlockPos, Long> ACTIVE = new HashMap<>();

    private GhostPreview() {
    }

    /** Startet die Projektion am Controller {@code pos} (nur Client). */
    // SPEC-GAP: Partikel statt Geisterblöcke, Muster aus den Jar-Daten (spec-gaps #28)
    public static void show(Level level, BlockPos pos) {
        JsonObject rules = BalanceManager.getOrBundled().entry("misc", "multiblock_rules").orElse(new JsonObject());
        long ticks = rules.has("ghost_projection_ticks") ? rules.get("ghost_projection_ticks").getAsLong() : 0;
        ACTIVE.put(pos.immutable(), level.getGameTime() + ticks);
    }

    /** Läuft gerade eine Projektion an diesem Controller? */
    public static boolean active(Level level, BlockPos pos) {
        Long until = ACTIVE.get(pos);
        return until != null && level.getGameTime() <= until;
    }

    /** Fehlende Plätze laut Client-Welt (für Projektion und Tests). */
    public static List<MultiblockPattern.Missing> missing(Level level, BlockPos pos, BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Direction facing = state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
        return MultiblockPattern.forMachine(BalanceManager.getOrBundled(), id)
                .map(p -> p.check(level, pos, facing, true)).orElse(List.of());
    }

    /** Client-Tick des Controllers: alle {@value #WAVE_TICKS} Ticks Partikel an den fehlenden Plätzen. */
    public static void clientTick(Level level, BlockPos pos, BlockState state) {
        if (ACTIVE.isEmpty() || !ACTIVE.containsKey(pos)) {
            return;
        }
        if (!active(level, pos)) {
            ACTIVE.remove(pos);
            return;
        }
        if (level.getGameTime() % WAVE_TICKS != 0) {
            return;
        }
        DustParticleOptions dust = new DustParticleOptions(COLOR, SCALE);
        for (MultiblockPattern.Missing m : missing(level, pos, state)) {
            BlockPos p = m.pos();
            level.addParticle(dust, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
        }
    }
}
