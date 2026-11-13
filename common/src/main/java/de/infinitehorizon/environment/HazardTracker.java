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
import de.infinitehorizon.core.fluid.ComponentContainers;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.network.HazardStatePayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
        @Nullable HazardStatePayload lastSent;
    }

    /** Schlüssel der Server-Config für den O₂-Verbrauchsfaktor der Anzüge (24.4). */
    public static final String OXYGEN_CONSUMPTION_KEY = "oxygen_consumption";
    /** Der Verbrauchszähler rechnet in 1/1.000 mB (9.1). */
    private static final long MILLI = 1000;

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
            keepAirWithSuit(player);
            if (--t.untilCheck > 0) {
                continue;
            }
            t.untilCheck = r.intervalTicks();
            check(player, t, r);
            // Erkundungsziele (11.1) zusammen mit der Umweltprüfung
            de.infinitehorizon.research.ExplorationService.check(player);
            de.infinitehorizon.agency.AtlasService.recordBiome(player);
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

    /** Umweltwerte an der Position des Spielers mit seinem Schutz (Anzug 9.1, sonst Schutz ohne Anzug 8.3). */
    public static EnvironmentResult evaluate(ServerPlayer player, EnvironmentRules r) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        PlanetResolver.Resolved res = PlanetResolver.at(level, pos);
        int cover = coverBlocks(level, pos, r.radiation().coverBlocks());
        Suits.Tier suit = Suits.worn(player);
        Protection protection = suit == null ? r.noSuit() : Suits.protection(suit, Suits.oxygen(Suits.chestplate(player)));
        // Hitzezeit abgelaufen (9.1): ungeschützt gegen Hitze wie ohne Anzug
        VenusHeat heat = VenusHeat.rules();
        if (suit != null && suit.venusHeatTimer() && heat != null
                && heat.running(res.planet() == null ? null : res.planet().dimension(), pos.getY())
                && heat.expired(Services.ATTACHMENTS.hazardState(player).venusHeat())) {
            protection = new Protection(protection.tempMin(), r.noSuit().tempMax(), protection.pressures(),
                    protection.radiationMax(), protection.suppliesOxygen(), protection.deepPressure());
        }
        // Sonnensturm folgt mit Kapitel 19
        // Druckkabine des Rovers (16): Atemluft und Kabinentemperatur wie unter einer Kuppel T1 (SPEC-GAP #91)
        EnvironmentInput.DomeCover dome = player.getVehicle() instanceof de.infinitehorizon.vehicle.RoverEntity rover
                && rover.cabinActive() ? new EnvironmentInput.DomeCover(1, rover.cabinTemperature()) : domeCover(level, pos);
        EnvironmentInput in = new EnvironmentInput(res.planet(), res.zones(), res.biome(), level.getOverworldClockTime(),
                cover, de.infinitehorizon.event.EventService.active(level, de.infinitehorizon.event.EventService.SOLAR_STORM),
                dome, protection);
        return EnvironmentCalculator.compute(in, r);
    }

    /**
     * Aktive Kuppel an der Position (10.1): Atemluft, Druck {@code normal}, Innentemperatur aus domes.json. Eine Kuppel
     * läuft nur, wenn die Außentemperatur in ihrem Bereich liegt; sie regelt deshalb immer die Temperatur.
     */
    public static EnvironmentInput.@Nullable DomeCover domeCover(ServerLevel level, BlockPos pos) {
        return de.infinitehorizon.dome.DomeManager.domeAt(level, pos).map(d -> {
            com.google.gson.JsonObject rules = de.infinitehorizon.dome.DomeManager.rules();
            Double inside = rules.has("inside_temperature") ? rules.get("inside_temperature").getAsDouble() : null;
            return new EnvironmentInput.DomeCover(d.tier(), inside);
        }).orElse(null);
    }

    /**
     * Respawn (14.4): Ist der Zielpunkt ohne Anzug unsicher (z. B. Kuppel ausgefallen), erscheint der Spieler am
     * Weltspawn der Overworld (Klon-Kammer kommt später), mit Meldung.
     */
    public static void checkRespawn(ServerPlayer player) {
        if (BalanceManager.get().isEmpty()) {
            return;
        }
        EnvironmentRules r = rules();
        ServerLevel level = player.level();
        PlanetResolver.Resolved res = PlanetResolver.at(level, player.blockPosition());
        if (res.planet() == null || res.planet().dimension().equals(PlanetResolver.EARTH)) {
            return;
        }
        EnvironmentResult result = EnvironmentCalculator.compute(new EnvironmentInput(res.planet(), res.zones(), res.biome(),
                level.getOverworldClockTime(), 0, false, domeCover(level, player.blockPosition()), r.noSuit()), r);
        if (result.hazards().isEmpty()) {
            return;
        }
        ServerLevel overworld = level.getServer().overworld();
        BlockPos spawn = de.infinitehorizon.space.LandingService.find(overworld,
                level.getServer().getRespawnData().pos().getX(), level.getServer().getRespawnData().pos().getZ());
        player.teleport(new net.minecraft.world.level.portal.TeleportTransition(overworld,
                net.minecraft.world.phys.Vec3.atBottomCenterOf(spawn), net.minecraft.world.phys.Vec3.ZERO, player.getYRot(),
                player.getXRot(), net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message." + de.infinitehorizon.InfiniteHorizon.MOD_ID + ".respawn.unsafe"));
    }

    /** Vollständiger Anzug mit O₂: Der Vanilla-Luftvorrat bleibt unter Wasser voll (9.1). */
    private static void keepAirWithSuit(ServerPlayer player) {
        if (player.getAirSupply() >= player.getMaxAirSupply()) {
            return;
        }
        if (Suits.worn(player) != null && Suits.oxygen(Suits.chestplate(player)) > 0) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }

    /**
     * O₂ des Anzugs (9.1): Nachfüllen aus Flaschen im Inventar (0–35, dann Zweithand), dann Verbrauch mit vollständigem
     * Anzug außerhalb von {@code breathable}. Kreativ und Zuschauer verbrauchen nichts.
     */
    static void suitOxygen(ServerPlayer player, HazardState state, EnvironmentResult result, boolean exempt, int interval) {
        ItemStack chest = Suits.chestplate(player);
        if (Suits.tierOf(chest, 1) == null) {
            return;
        }
        int capacity = Suits.supply().capacity();
        int oxygen = Suits.oxygen(chest);
        oxygen = refillFromDome(player, oxygen, capacity, interval);
        if (oxygen < capacity) {
            Inventory inv = player.getInventory();
            for (int slot = 0; slot <= Inventory.INVENTORY_SIZE && oxygen < capacity; slot++) {
                ItemStack can = slot < Inventory.INVENTORY_SIZE ? inv.getItem(slot) : player.getOffhandItem();
                if (!ComponentContainers.hasContent(can)
                        || !Suits.supply().refillItems().contains(BuiltInRegistries.ITEM.getKey(can.getItem()).toString())) {
                    continue;
                }
                oxygen += (int) ComponentContainers.drain(can, capacity - oxygen).amount();
            }
            Suits.setOxygen(chest, oxygen);
        }
        Suits.Tier suit = Suits.worn(player);
        if (suit == null || result.breathable() || exempt || oxygen <= 0) {
            return;
        }
        double factor = ServerConfig.get().getDouble(OXYGEN_CONSUMPTION_KEY);
        long counter = state.suitOxygenCounter() + Suits.consumptionMilli(suit, interval, factor);
        int used = (int) Math.min(oxygen, counter / MILLI);
        state.setSuitOxygenCounter(used < oxygen ? counter - (long) used * MILLI : 0);
        Suits.setOxygen(chest, oxygen - used);
    }

    private static void check(ServerPlayer player, Timers t, EnvironmentRules r) {
        HazardState state = Services.ATTACHMENTS.hazardState(player);
        stepVenusHeat(player, state, r.intervalTicks());
        EnvironmentResult result = evaluate(player, r);
        t.last = result;
        boolean exempt = player.isCreative() || player.isSpectator();
        suitOxygen(player, state, result, exempt, r.intervalTicks());
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
        sendState(player, t, state, result, r);
    }

    /**
     * In aktiven Kuppeln füllt sich der Anzug mit {@code dome_refill_per_tick} mB/t aus dem O₂-Tank des Kuppelgenerators,
     * solange dort mindestens {@code dome_refill_min_tank} mB liegen (9.1).
     */
    private static int refillFromDome(ServerPlayer player, int oxygen, int capacity, int interval) {
        if (oxygen >= capacity) {
            return oxygen;
        }
        var dome = de.infinitehorizon.dome.DomeManager.domeAt(player.level(), player.blockPosition());
        if (dome.isEmpty() || !(player.level().getBlockEntity(dome.get().center())
                instanceof de.infinitehorizon.dome.DomeGeneratorBlockEntity gen)) {
            return oxygen;
        }
        com.google.gson.JsonObject supply = BalanceManager.get().entry("suits", "oxygen_supply").orElse(new com.google.gson.JsonObject());
        int perTick = supply.has("dome_refill_per_tick") ? supply.get("dome_refill_per_tick").getAsInt() : 0;
        int minTank = supply.has("dome_refill_min_tank") ? supply.get("dome_refill_min_tank").getAsInt() : 0;
        long inTank = gen.tank().stack().amount();
        if (inTank < minTank || perTick <= 0) {
            return oxygen;
        }
        long move = Math.min((long) perTick * interval, Math.min(capacity - oxygen, inTank - minTank + 1));
        if (move <= 0) {
            return oxygen;
        }
        try (de.infinitehorizon.core.transaction.Transaction tx = de.infinitehorizon.core.transaction.Transaction.open()) {
            gen.tank().extract(gen.tank().stack().fluid(), move, tx);
            tx.commit();
        }
        int result = oxygen + (int) move;
        Suits.setOxygen(Suits.chestplate(player), result);
        return result;
    }

    /** Hitzezeit (9.1) einen Prüftakt weiterzählen; ohne Anzug mit Timer füllt sie sich auf. */
    private static void stepVenusHeat(ServerPlayer player, HazardState state, int ticks) {
        VenusHeat heat = VenusHeat.rules();
        if (heat == null) {
            return;
        }
        Suits.Tier suit = Suits.worn(player);
        PlanetResolver.Resolved res = PlanetResolver.at(player.level(), player.blockPosition());
        boolean running = suit != null && suit.venusHeatTimer()
                && heat.running(res.planet() == null ? null : res.planet().dimension(), player.getBlockY())
                && !player.isCreative() && !player.isSpectator();
        state.setVenusHeat(heat.step(state.venusHeat(), running, ticks));
    }

    /** Restliche Hitzezeit für das HUD in Ticks; −1 = Leiste ausblenden. */
    private static int venusHeatLeft(ServerPlayer player, HazardState state) {
        VenusHeat heat = VenusHeat.rules();
        Suits.Tier suit = Suits.worn(player);
        if (heat == null || suit == null || !suit.venusHeatTimer()) {
            return -1;
        }
        PlanetResolver.Resolved res = PlanetResolver.at(player.level(), player.blockPosition());
        boolean running = heat.running(res.planet() == null ? null : res.planet().dimension(), player.getBlockY());
        return running || state.venusHeat() > 0 ? heat.duration() - state.venusHeat() : -1;
    }

    /** {@code HazardStatePayload} (3A.5) bei Änderung, höchstens einmal je Prüftakt. */
    private static void sendState(ServerPlayer player, Timers t, HazardState state, EnvironmentResult result,
                                  EnvironmentRules r) {
        byte[] percent = new byte[HazardStatePayload.BARS.length];
        for (int i = 0; i < percent.length; i++) {
            double f = 0;
            for (Hazard h : HazardStatePayload.BARS[i]) {
                f = Math.max(f, state.fraction(h, r.hazards().get(h).buildupTicks()));
            }
            percent[i] = (byte) Math.round(f * 100);
        }
        int suitOxygen = Suits.worn(player) == null ? -1 : Suits.oxygen(Suits.chestplate(player));
        HazardStatePayload payload = new HazardStatePayload(percent, (int) Math.round(result.temperature()),
                (byte) result.pressure().ordinal(), (byte) result.radiation(), result.breathable(), suitOxygen,
                venusHeatLeft(player, state));
        if (!payload.equals(t.lastSent)) {
            t.lastSent = payload;
            Services.NETWORK.sendToPlayer(player, payload);
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
