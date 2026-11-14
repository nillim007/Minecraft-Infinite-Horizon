package de.infinitehorizon.station;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.AtmosphereRules;
import de.infinitehorizon.machine.MachineLocation;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;

/** Stationsmodule (14.2): Ortsbindung, Forschungslabor, Wohnmodul als Bett. */
public final class StationModules {

    private StationModules() {
    }

    /** Eintrag aus stations.json ({@code station_core}, {@code research_lab} …) oder leer. */
    public static JsonObject config(String path) {
        return BalanceManager.get().entry("stations", InfiniteHorizon.id(path).toString()).orElse(new JsonObject());
    }

    /** Arbeitet das Modul hier (Ortsbindung aus stations.json, meist nur im Orbit)? */
    public static boolean works(ServerLevel level, BlockPos pos, String path) {
        return MachineLocation.allowed(config(path), level, pos);
    }

    /** FP-Faktor eines Analysators (14.2, research.json {@code research_lab}): angrenzendes Labor im Orbit. */
    public static double labMultiplier(ServerLevel level, BlockPos analyzer) {
        JsonObject r = BalanceManager.get().entry("research", "research_lab").orElse(null);
        if (r == null || !r.has("analyzer_fp_multiplier")) {
            return 1.0;
        }
        for (Direction d : Direction.values()) {
            BlockPos at = analyzer.relative(d);
            if (level.getBlockState(at).is(ModBlocks.RESEARCH_LAB.get()) && works(level, at, "research_lab")) {
                return r.get("analyzer_fp_multiplier").getAsDouble();
            }
        }
        return 1.0;
    }

    /**
     * Wohnmodul (14.2, 14.4): Bett-Funktion. Setzt den Respawnpunkt über dem Modul, nur im Orbit und nur in einer aktiven
     * Kuppel; sonst Meldung. Gibt zurück, ob der Punkt gesetzt wurde.
     */
    public static boolean setSpawn(ServerPlayer player, BlockPos module) {
        ServerLevel level = player.level();
        if (!works(level, module, "habitation_module")) {
            player.sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".station_module.orbit_only"));
            return false;
        }
        if (AtmosphereRules.airless(level, module.above())) {
            player.sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".bed.no_air"));
            return false;
        }
        // erzwungener Punkt (wie /spawnpoint): Vanilla prüft beim Respawn nur, ob man dort stehen kann
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(level.dimension(), module.above(), player.getYRot(), 0), true), false);
        player.sendOverlayMessage(Component.translatable("block.minecraft.set_spawn"));
        return true;
    }
}
