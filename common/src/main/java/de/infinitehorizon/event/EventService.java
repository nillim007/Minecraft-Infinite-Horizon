package de.infinitehorizon.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.dome.DomeManager;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.network.EventStatePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Ereignisse und Wetter je Dimension (19, events.json): alle {@code check_interval_ticks} Würfeln (× {@code event_frequency}),
 * Planung {@code schedule_ahead_ticks} voraus, Vorwarnung {@code warning_ticks_without_station} vorher (die
 * Wetterstation kommt mit ihrem Forschungsknoten, SPEC-GAP #93), eigenes Wetter für Mars (Staubsturm) und Venus
 * (Säureregen). Wirkungen: Strahlung und Maschinenpausen im Sonnensturm, Solarfaktor, Meteoriteneinschläge,
 * Säureschaden, Staubgeister.
 */
public final class EventService {

    public static final Identifier SOLAR_STORM = InfiniteHorizon.id("solar_storm");
    public static final Identifier METEOR_SHOWER = InfiniteHorizon.id("meteor_shower");
    public static final Identifier DUST_STORM = InfiniteHorizon.id("dust_storm");
    public static final Identifier ACID_RAIN = InfiniteHorizon.id("acid_rain");

    private EventService() {
    }

    static JsonObject entry(String key) {
        return BalanceManager.get().entry("events", key).orElseGet(JsonObject::new);
    }

    static JsonObject event(Identifier id) {
        return entry(id.toString());
    }

    static double num(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsDouble() : 0;
    }

    private static JsonObject effects(Identifier id) {
        JsonObject e = event(id);
        return e.has("effects") ? e.getAsJsonObject("effects") : new JsonObject();
    }

    /** Wetterereignis dieser Dimension oder null (immer klar). */
    static @Nullable Identifier weatherEvent(Identifier dimension) {
        JsonObject w = entry("weather");
        JsonObject map = w.has("weather_events") ? w.getAsJsonObject("weather_events") : new JsonObject();
        return map.has(dimension.toString()) ? Identifier.parse(map.get(dimension.toString()).getAsString()) : null;
    }

    /** Faktor aus der Einstellung {@code event_frequency}. */
    static double frequency() {
        JsonObject f = entry("event_frequency");
        String key = ServerConfig.get().getString(f.has("config_key") ? f.get("config_key").getAsString() : "event_frequency");
        JsonObject m = f.has("multipliers") ? f.getAsJsonObject("multipliers") : new JsonObject();
        return m.has(key) ? m.get(key).getAsDouble() : 1.0;
    }

    /** Läuft das Ereignis {@code id} gerade in dieser Dimension (geplant oder als Wetter)? */
    public static boolean active(Level level, Identifier id) {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        PlanetEvents d = PlanetEvents.get(sl);
        PlanetEvents.Planned p = d.planned();
        long now = sl.getGameTime();
        if (p != null && p.event().equals(id) && now >= p.start() && now < p.end()) {
            return true;
        }
        return d.weather() && id.equals(weatherEvent(sl.dimension().identifier()));
    }

    /** Für Tests und Befehle: Ereignis sofort für {@code ticks} starten. */
    public static void force(ServerLevel level, Identifier id, int ticks) {
        long now = level.getGameTime();
        if (id.equals(weatherEvent(level.dimension().identifier())) || "weather".equals(str(event(id), "trigger"))) {
            PlanetEvents.get(level).setWeather(true, now + ticks);
            FORCED_WEATHER.put(level.dimension().identifier(), id);
        } else {
            PlanetEvents.get(level).setPlanned(new PlanetEvents.Planned(id, now, now + ticks, true));
        }
        sendState(level);
    }

    /** Für Tests: Wetterereignis an einer Dimension ohne eigenes Wetter (z. B. Overworld). */
    private static final java.util.Map<Identifier, Identifier> FORCED_WEATHER = new java.util.HashMap<>();

    public static void clear(ServerLevel level) {
        PlanetEvents.get(level).setPlanned(null);
        PlanetEvents.get(level).setWeather(false, 0);
        FORCED_WEATHER.remove(level.dimension().identifier());
        sendState(level);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    /** Ereignis am Ort (Wetter auch erzwungen). */
    private static boolean activeHere(ServerLevel level, Identifier id) {
        return active(level, id) || PlanetEvents.get(level).weather() && id.equals(FORCED_WEATHER.get(level.dimension().identifier()));
    }

    // ---------------------------------------------------------------- Wirkungen

    /** Solarfaktor durch Ereignisse: Sonnensturm × reward.solar_multiplier, Staubsturm × effects.solar_multiplier. */
    public static double solarMultiplier(Level level) {
        return solarMultiplier(level, null);
    }

    /** Solarfaktor an {@code pos}: Unter einer Kuppel, die vor dem Staubsturm schützt (T2, 10.1), wirkt er dort nicht. */
    public static double solarMultiplier(Level level, @org.jspecify.annotations.Nullable BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) {
            return 1.0;
        }
        double f = 1.0;
        if (activeHere(sl, SOLAR_STORM)) {
            JsonObject reward = event(SOLAR_STORM).getAsJsonObject("reward");
            f *= reward == null ? 1.0 : num(reward, "solar_multiplier");
        }
        if (activeHere(sl, DUST_STORM) && (pos == null || !domeProtects(sl, pos, DUST_STORM))) {
            f *= num(effects(DUST_STORM), "solar_multiplier");
        }
        return f;
    }

    /**
     * Schützt eine aktive Kuppel an {@code pos} vor dem Ereignis? {@code protection} in events.json: {@code dome} mit
     * Mindeststufe (10.1: T2 gegen Staubsturm und Säureregen) oder {@code dome_module} (Schildmodul gegen Meteoriten).
     */
    public static boolean domeProtects(ServerLevel level, BlockPos pos, Identifier event) {
        JsonObject e = event(event);
        var dome = DomeManager.domeAt(level, pos);
        if (e == null || !e.has("protection") || dome.isEmpty()) {
            return false;
        }
        for (JsonElement pe : e.getAsJsonArray("protection")) {
            JsonObject prot = pe.getAsJsonObject();
            String type = str(prot, "type");
            if ("dome".equals(type) && dome.get().tier() >= num(prot, "min_tier")) {
                return true;
            }
            if ("dome_module".equals(type)) {
                Identifier module = Identifier.parse(str(prot, "id"));
                for (var d : DomeManager.domesAt(level, pos)) {
                    if (level.getBlockEntity(d.center()) instanceof de.infinitehorizon.dome.DomeGeneratorBlockEntity g
                            && g.modules().contains(module)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Monster-Spawnfaktor (Staubsturm {@code monster_spawn_multiplier}). */
    public static double monsterMultiplier(ServerLevel level) {
        return activeHere(level, DUST_STORM) ? num(effects(DUST_STORM), "monster_spawn_multiplier") : 1.0;
    }

    /** Sonnensturm: Maschinen ohne Deckung und ohne Kuppel ab Stufe {@code exempt_dome_min_tier} pausieren. */
    public static boolean machinePaused(ServerLevel level, BlockPos pos) {
        if (!activeHere(level, SOLAR_STORM)) {
            return false;
        }
        JsonObject pause = effects(SOLAR_STORM).getAsJsonObject("machine_pause");
        if (pause == null || cover(level, pos, (int) num(pause, "exempt_cover_blocks")) >= num(pause, "exempt_cover_blocks")) {
            return false;
        }
        var dome = DomeManager.domeAt(level, pos);
        if (dome.isPresent() && dome.get().tier() >= num(pause, "exempt_dome_min_tier")) {
            return false;
        }
        PlanetEvents.Planned p = PlanetEvents.get(level).planned();
        long since = level.getGameTime() - (p == null ? 0 : p.start());
        return since % (long) num(pause, "interval_ticks") < num(pause, "pause_ticks");
    }

    /** Feste Blöcke über {@code pos} (bis {@code max}). */
    static int cover(ServerLevel level, BlockPos pos, int max) {
        int n = 0;
        for (int y = pos.getY() + 1; y < level.getMaxY() && n < max; y++) {
            if (level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).isSolid()) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- Takt

    public static void tick(MinecraftServer server) {
        if (BalanceManager.get().isEmpty()) {
            return;
        }
        JsonObject plan = entry("event_planning");
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty() && plan.has("requires_player_in_dimension")) {
                continue;
            }
            tickLevel(level, plan);
        }
    }

    private static void tickLevel(ServerLevel level, JsonObject plan) {
        PlanetEvents data = PlanetEvents.get(level);
        long now = level.getGameTime();
        Identifier dim = level.dimension().identifier();
        RandomSource random = level.getRandom();
        // Wetter (19): klar ↔ Wetterereignis
        Identifier weather = weatherEvent(dim);
        if (weather != null && now >= data.weatherUntil) {
            JsonObject w = entry("weather");
            boolean next = !data.weather();
            int min = (int) num(w, next ? "weather_min" : "clear_min");
            int max = (int) num(w, next ? "weather_max" : "clear_max");
            data.setWeather(next, now + min + random.nextInt(Math.max(1, max - min + 1)));
            sendState(level);
        }
        PlanetEvents.Planned p = data.planned();
        if (p != null && now >= p.end()) {
            data.setPlanned(null);
            sendState(level);
            p = null;
        }
        // Würfeln
        long interval = (long) num(plan, "check_interval_ticks");
        if (p == null && interval > 0 && now % interval == 0) {
            roll(level, data, plan, random);
        }
        p = data.planned();
        // Frühwarnung (19): Agenturen mit laufender space_weather_station in dieser Dimension
        if (p != null && !p.warned() && now >= p.start() - (long) num(plan, "warning_ticks_with_station")) {
            earlyWarn(level, p);
        }
        if (p != null && !p.warned() && now >= p.start() - (long) num(plan, "warning_ticks_without_station")) {
            data.setPlanned(new PlanetEvents.Planned(p.event(), p.start(), p.end(), true));
            warn(level, p.event(), p.start());
        }
        if (p != null && now == p.start()) {
            sendState(level);
        }
        effects(level, now);
    }

    private static void roll(ServerLevel level, PlanetEvents data, JsonObject plan, RandomSource random) {
        Identifier dim = level.dimension().identifier();
        double freq = frequency();
        for (String key : BalanceManager.get().entries("events").map(JsonObject::keySet).orElse(java.util.Set.of())) {
            JsonObject e = entry(key);
            if (!"roll".equals(str(e, "trigger")) || !contains(e, "dimensions", dim.toString())) {
                continue;
            }
            double chance = num(e, "chance_per_1200") * freq;
            JsonObject mult = e.has("chance_multipliers") ? e.getAsJsonObject("chance_multipliers") : new JsonObject();
            if (mult.has(dim.toString())) {
                chance *= mult.get(dim.toString()).getAsDouble();
            }
            if (e.has("chance_per") || chance <= 0 || random.nextDouble() >= chance) {
                continue;
            }
            long start = level.getGameTime() + (long) num(plan, "schedule_ahead_ticks");
            int min = (int) num(e, "duration_min");
            int max = (int) num(e, "duration_max");
            data.setPlanned(new PlanetEvents.Planned(Identifier.parse(key), start, start + min + random.nextInt(Math.max(1, max - min + 1)), false));
            return;
        }
    }

    private static boolean contains(JsonObject o, String key, String value) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return false;
        }
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (e.getAsString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** Frühe Vorwarnung an die Mitglieder der Agenturen mit laufender Station (je Agentur einmal). */
    public static void earlyWarn(ServerLevel level, PlanetEvents.Planned p) {
        java.util.Set<java.util.UUID> stations = WeatherStations.agencies(level);
        if (stations.isEmpty()) {
            return;
        }
        java.util.Set<java.util.UUID> fresh = new java.util.HashSet<>();
        for (java.util.UUID a : stations) {
            if (WeatherStations.markWarned(level, p.start(), a)) {
                fresh.add(a);
            }
        }
        for (ServerPlayer player : level.players()) {
            java.util.UUID a = WeatherStations.agencyOf(player);
            if (a != null && fresh.contains(a)) {
                tell(level, player, p.event());
                sendState(player);
            }
        }
    }

    private static void tell(ServerLevel level, ServerPlayer player, Identifier event) {
        player.sendSystemMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".event.warning",
                Component.translatable("event." + event.getNamespace() + "." + event.getPath())));
        var sound = BuiltInRegistries.SOUND_EVENT.getValue(InfiniteHorizon.id("warning.radiation"));
        if (sound != null) {
            level.playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, 1f, 1f);
        }
    }

    /** Vorwarnung (19): Chat, Ton, HUD-Symbol; wer schon früh gewarnt wurde, bekommt sie nicht noch einmal. */
    private static void warn(ServerLevel level, Identifier event, long start) {
        for (ServerPlayer p : level.players()) {
            if (!WeatherStations.warned(p, start)) {
                tell(level, p, event);
            }
        }
        sendState(level);
    }

    private static void effects(ServerLevel level, long now) {
        if (activeHere(level, METEOR_SHOWER)) {
            JsonObject fx = effects(METEOR_SHOWER);
            if (now % Math.max(1, (long) num(fx, "impact_interval_ticks")) == 0) {
                for (ServerPlayer p : level.players()) {
                    impact(level, p.blockPosition(), fx);
                }
            }
        }
        if (activeHere(level, ACID_RAIN)) {
            JsonObject fx = effects(ACID_RAIN);
            if (now % Math.max(1, (long) num(fx, "interval_ticks")) == 0) {
                for (ServerPlayer p : level.players()) {
                    acid(level, p, (int) num(fx, "durability_loss"));
                }
            }
        }
        if (activeHere(level, DUST_STORM) && now % Math.max(1, (long) num(entry("event_planning"), "check_interval_ticks")) == 0) {
            // SPEC-GAP #93: Staubgeister erscheinen je Prüftakt bei jedem Spieler im Sturm
            for (ServerPlayer p : level.players()) {
                if (domeProtects(level, p.blockPosition(), DUST_STORM)) {
                    continue;
                }
                var wraith = ModEntities.DUST_WRAITH.get().create(level, EntitySpawnReason.EVENT);
                if (wraith != null) {
                    BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                            p.blockPosition().offset(level.getRandom().nextInt(17) - 8, 0, level.getRandom().nextInt(17) - 8));
                    wraith.setPos(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
                    level.addFreshEntity(wraith);
                }
            }
        }
    }

    /** Meteoriteneinschlag im Umkreis {@code impact_radius}: Explosion ohne Feuer, danach {@code leaves_block}. */
    public static BlockPos impact(ServerLevel level, BlockPos around, JsonObject fx) {
        int r = (int) num(fx, "impact_radius");
        RandomSource random = level.getRandom();
        BlockPos column = around.offset(random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (domeProtects(level, top, METEOR_SHOWER)) {
            return top; // vom Schildmodul abgefangen (10.1)
        }
        boolean damage = ServerConfig.get().getBoolean(str(fx, "block_damage_config"));
        level.explode(null, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, (float) num(fx, "explosion_power"),
                fx.has("fire") && fx.get("fire").getAsBoolean(), damage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        BlockPos land = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        String leaves = str(fx, "leaves_block");
        if (!leaves.isEmpty() && level.getBlockState(land).canBeReplaced()) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(leaves));
            level.setBlockAndUpdate(land, b.defaultBlockState());
        }
        return land;
    }

    /** Säureregen: ungeschützt unter freiem Himmel verlieren Rüstung und gehaltene Items Haltbarkeit. */
    public static boolean acid(ServerLevel level, ServerPlayer p, int loss) {
        if (p.isCreative() || p.isSpectator() || !level.canSeeSky(BlockPos.containing(p.getEyePosition()))) {
            return false;
        }
        var dome = DomeManager.domeAt(level, p.blockPosition());
        for (JsonElement pe : event(ACID_RAIN).getAsJsonArray("protection")) {
            JsonObject prot = pe.getAsJsonObject();
            if ("dome".equals(str(prot, "type")) && dome.isPresent() && dome.get().tier() >= num(prot, "min_tier")) {
                return false;
            }
            Suits.Tier suit = Suits.worn(p);
            if ("suit".equals(str(prot, "type")) && suit != null && suit.tier() >= num(prot, "min_tier")) {
                return false;
            }
        }
        boolean hit = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack s = p.getItemBySlot(slot);
            if (!s.isEmpty() && s.isDamageableItem()) {
                s.hurtAndBreak(loss, p, slot);
                hit = true;
            }
        }
        return hit;
    }

    // ---------------------------------------------------------------- Client

    /** Stand an alle Spieler der Dimension: Ereignis, Phase (1 Warnung, 2 aktiv) und Rest-Ticks. */
    public static void sendState(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            sendState(p);
        }
    }

    public static void sendState(ServerPlayer player) {
        ServerLevel level = player.level();
        PlanetEvents d = PlanetEvents.get(level);
        long now = level.getGameTime();
        PlanetEvents.Planned p = d.planned();
        String id = "";
        byte phase = 0;
        int ticks = 0;
        if (p != null && now < p.end()) {
            id = p.event().toString();
            phase = (byte) (now >= p.start() ? 2 : p.warned() || WeatherStations.warned(player, p.start()) ? 1 : 0);
            ticks = (int) (now >= p.start() ? p.end() - now : p.start() - now);
        } else {
            Identifier w = d.weather() ? weatherEvent(level.dimension().identifier()) : null;
            if (w == null) {
                w = d.weather() ? FORCED_WEATHER.get(level.dimension().identifier()) : null;
            }
            if (w != null) {
                id = w.toString();
                phase = 2;
                ticks = (int) Math.max(0, d.weatherUntil - now);
            }
        }
        Services.NETWORK.sendToPlayer(player, new EventStatePayload(id, phase, ticks));
    }
}
