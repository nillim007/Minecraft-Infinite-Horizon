package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyAdvancements;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.research.FpLedger;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Sondenflüge (12.3): Start von der Rampe (Block wird zum {@code probe_entity}), Flug in {@code probe_flights} mit
 * Ankunft = Overworld-Spielzeit beim Start + {@code base_ticks} + {@code per_dp_ticks} × DP, Prüfung jede Sekunde
 * (übersteht Neustart und Abmelden), Ankunft mit {@code probe_reached}, FP beim ersten Mal, Nachricht und Erfolg
 * {@code first_signal}.
 */
public final class ProbeService {

    public static final Identifier FIRST_SIGNAL = InfiniteHorizon.id("first_signal");
    /** Prüftakt: jede Sekunde (12.3). */
    private static final int CHECK_TICKS = 20;

    private ProbeService() {
    }

    private static JsonObject rules() {
        return BalanceManager.get().entry("spaceflight", "probe_flight").orElse(new JsonObject());
    }

    /** Countdown vor dem Start (Ticks). */
    public static int countdownTicks() {
        JsonObject r = rules();
        return r.has("countdown_ticks") ? r.get("countdown_ticks").getAsInt() : 0;
    }

    /** Flugzeit zum Ziel (Ticks): {@code base_ticks + per_dp_ticks × DP}. */
    public static long flightTicks(Planet target) {
        JsonObject r = rules();
        double dp = target.raw().has("distance_points") ? target.raw().get("distance_points").getAsDouble() : 0;
        return Math.round((r.has("base_ticks") ? r.get("base_ticks").getAsDouble() : 0)
                + (r.has("per_dp_ticks") ? r.get("per_dp_ticks").getAsDouble() : 0) * dp);
    }

    /** Darf die Agentur eine Sonde zu {@code target} schicken? Ziel in {@code probe_targets} mit Planeten-Eintrag. */
    public static boolean canTarget(Agency agency, Identifier target) {
        return agency.progress().probeTargets().contains(target) && PlanetRegistry.get().planet(target).isPresent();
    }

    /** Startet einen Flug (ohne Countdown); Ankunft in Overworld-Spielzeit. */
    public static void startFlight(MinecraftServer server, Agency agency, Identifier target) {
        Planet planet = PlanetRegistry.get().planet(target).orElseThrow();
        long now = server.overworld().getGameTime();
        agency.progress().addProbeFlight(new AgencyProgress.ProbeFlight(target, now + flightTicks(planet)));
        AgencyData.get(server).setDirty();
    }

    /** Nach dem Countdown: Sondenblock entfernen, {@code probe_entity} aufsteigen lassen, Flug eintragen. */
    public static void launch(ServerLevel level, BlockPos probeBlock, Agency agency, Identifier target) {
        level.setBlockAndUpdate(probeBlock, Blocks.AIR.defaultBlockState());
        ProbeEntity entity = ModEntities.PROBE_ENTITY.get().create(level, EntitySpawnReason.TRIGGERED);
        if (entity != null) {
            entity.setPos(probeBlock.getX() + 0.5, probeBlock.getY(), probeBlock.getZ() + 0.5);
            level.addFreshEntity(entity);
        }
        startFlight(level.getServer(), agency, target);
    }

    /** Jeder Server-Tick: jede Sekunde angekommene Flüge abschließen. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        AgencyData data = AgencyData.get(server);
        long now = server.overworld().getGameTime();
        for (Agency agency : data.all()) {
            if (agency.progress().probeFlights().isEmpty()) {
                continue;
            }
            List<AgencyProgress.ProbeFlight> arrived = agency.progress().takeArrived(now);
            for (AgencyProgress.ProbeFlight f : arrived) {
                arrive(server, agency, f.target());
            }
            if (!arrived.isEmpty()) {
                data.setDirty();
            }
        }
    }

    /** Ankunft (12.3): Ziel erreicht, beim ersten Mal FP, Nachricht, Erfolg. */
    public static void arrive(MinecraftServer server, Agency agency, Identifier target) {
        boolean first = agency.progress().addProbeReached(target);
        if (first) {
            FpLedger.Rules rules = FpLedger.rules();
            int fp = BalanceManager.get().entry("research", "probe_new_target")
                    .filter(o -> o.has("fp")).map(o -> o.get("fp").getAsInt()).orElse(0);
            agency.progress().addFp(FpLedger.value(fp, 0, rules));
        }
        AgencyData.get(server).setDirty();
        Component msg = Component.translatable("message." + InfiniteHorizon.MOD_ID + ".probe.arrived",
                Component.translatable("planet." + target.getNamespace() + "." + target.getPath())).withStyle(ChatFormatting.AQUA);
        for (Agency.Member m : agency.members()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(m.player());
            if (sp != null) {
                sp.sendSystemMessage(msg);
            }
        }
        // SPEC-GAP #74: first_signal bei jeder Sondenankunft (in M4 gibt es nur den Mond als Ziel)
        AgencyAdvancements.awardToAgency(server, agency.id(), FIRST_SIGNAL);
        ResearchService.syncAgency(server, agency);
    }

    /** Agentur eines Spielers (für den Start). */
    public static Agency agencyOf(ServerPlayer player) {
        return de.infinitehorizon.agency.AgencyManager.ensureFor(player);
    }
}
