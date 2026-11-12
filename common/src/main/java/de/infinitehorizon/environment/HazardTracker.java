package de.infinitehorizon.environment;

import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Belastung je Spieler (8.2, 3A.3): läuft im Server-Tick über die Spielerliste (auch GameTest-Spieler). Prüftakt
 * {@code interval_ticks} je Spieler ab Ankunft in der Dimension; ruft {@link EnvironmentCalculator} und schreibt die
 * Belastung ins Attachment {@code hazard_state}. Kreativ und Zuschauer bekommen keine Belastung und keinen Schaden.
 */
public final class HazardTracker {

    /** Laufzeitdaten je Spieler (nicht gespeichert): Takt, Dimension, Schadenszähler, letztes Ergebnis. */
    private static final class Timers {
        int untilCheck;
        @Nullable ResourceKey<Level> dimension;
        final Map<Hazard, Integer> damage = new EnumMap<>(Hazard.class);
        @Nullable EnvironmentResult last;
    }

    private static final Map<UUID, Timers> TIMERS = new HashMap<>();
    private static @Nullable BalanceManager rulesSource;
    private static @Nullable EnvironmentRules rules;

    private HazardTracker() {
    }

    /** Regeln aus der aktuellen Balance (nach {@code /reload} neu). */
    public static EnvironmentRules rules() {
        BalanceManager balance = BalanceManager.get();
        if (rules == null || rulesSource != balance) {
            rules = EnvironmentRules.from(balance);
            rulesSource = balance;
        }
        return rules;
    }

    public static void tick(MinecraftServer server) {
        if (BalanceManager.get().isEmpty()) {
            return;
        }
        EnvironmentRules r = rules();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Timers t = TIMERS.computeIfAbsent(player.getUUID(), id -> new Timers());
            ResourceKey<Level> dim = player.level().dimension();
            if (!dim.equals(t.dimension)) {
                t.dimension = dim;
                t.untilCheck = r.intervalTicks();
            }
            if (--t.untilCheck > 0) {
                continue;
            }
            t.untilCheck = r.intervalTicks();
            check(player, t, r);
        }
        if (server.getTickCount() % r.intervalTicks() == 0) {
            TIMERS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }

    /** Ergebnis der letzten Prüfung (null = noch keine); für HUD und Tests. */
    public static @Nullable EnvironmentResult lastResult(ServerPlayer player) {
        Timers t = TIMERS.get(player.getUUID());
        return t == null ? null : t.last;
    }

    /** Umweltwerte an der Position des Spielers mit seinem Schutz (in M3b: ohne Anzug). */
    public static EnvironmentResult evaluate(ServerPlayer player, EnvironmentRules r) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        PlanetResolver.Resolved res = PlanetResolver.at(level, pos);
        int cover = coverBlocks(level, pos, r.radiation().coverBlocks());
        // TODO(M3d): Schutz aus dem Anzug; M3e: Kuppel; Kapitel 19: Sonnensturm
        EnvironmentInput in = new EnvironmentInput(res.planet(), res.zones(), res.biome(), level.getOverworldClockTime(),
                cover, false, null, r.noSuit());
        return EnvironmentCalculator.compute(in, r);
    }

    private static void check(ServerPlayer player, Timers t, EnvironmentRules r) {
        EnvironmentResult result = evaluate(player, r);
        t.last = result;
        HazardState state = Services.ATTACHMENTS.hazardState(player);
        boolean exempt = player.isCreative() || player.isSpectator();
        for (Hazard h : Hazard.values()) {
            EnvironmentRules.HazardRule rule = r.hazards().get(h);
            boolean active = !exempt && result.has(h);
            int value = HazardState.step(state.buildup(h), active, r.intervalTicks(), rule.buildupTicks(), rule.decayTicks());
            state.setBuildup(h, value);
            if (!active || value < rule.buildupTicks()) {
                t.damage.remove(h);
                continue;
            }
            EnvironmentRules.HazardRule.DamageOverride d = rule.damageFor(result.temperature(), result.radiation());
            int timer = t.damage.getOrDefault(h, 0) + r.intervalTicks();
            while (timer >= d.damageInterval()) {
                hurt(player, r.damageTypes().get(h), d.damage());
                timer -= d.damageInterval();
            }
            t.damage.put(h, timer);
        }
    }

    private static void hurt(ServerPlayer player, @Nullable Identifier type, float amount) {
        if (type == null) {
            return;
        }
        ServerLevel level = player.level();
        level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(ResourceKey.create(Registries.DAMAGE_TYPE, type))
                .ifPresent(holder -> player.hurtServer(level, new DamageSource(holder), amount));
    }

    /** Feste Blöcke ({@code isSolidRender}) senkrecht über dem Kopf, gezählt bis {@code enough} (8.1). */
    static int coverBlocks(ServerLevel level, BlockPos feet, int enough) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
        int count = 0;
        BlockPos.MutableBlockPos p = feet.mutable().move(0, 2, 0);
        while (p.getY() < top && count < enough) {
            if (level.getBlockState(p).isSolidRender()) {
                count++;
            }
            p.move(0, 1, 0);
        }
        return count;
    }
}
