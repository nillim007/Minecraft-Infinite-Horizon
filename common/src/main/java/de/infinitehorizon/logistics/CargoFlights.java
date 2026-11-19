package de.infinitehorizon.logistics;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Frachtflüge (20.3): keine Entities, sondern Datensätze in der Overworld (Rakete, Heimat-Terminal und Hangar-Slot,
 * Ziel, Start- und Ankunftszeit, Fracht, Treibstoff). Die Rakete bleibt im Hangar-Slot und ist gesperrt. Am Ziel wird
 * entladen und für den Rückflug getankt; der Rückflug ist leer. Ohne Treibstoff wartet die Rakete und versucht es alle
 * {@code stranded_retry_ticks} erneut.
 */
public final class CargoFlights extends SavedData {

    /** Ergebnis eines Starts. */
    public enum Launch { OK, NO_ROCKET, BUSY, NO_TARGET, OUT_OF_FUEL }

    public static final String IN_FLIGHT = "in_flight";
    public static final String OUT_OF_FUEL = "out_of_fuel";

    /** Ein Flug; {@code fuel} = Treibstoff der Rakete in mB, {@code returning} = auf dem Rückflug. */
    public record Flight(UUID id, Identifier homeDimension, BlockPos home, int slot, Identifier targetDimension, BlockPos target,
                         ItemStack rocket, long start, long arrival, boolean returning, String status, long fuel,
                         List<ItemStack> items, List<IhFluidStack> fluids) {
        static final Codec<Flight> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Flight::id),
                Identifier.CODEC.fieldOf("home_dimension").forGetter(Flight::homeDimension),
                BlockPos.CODEC.fieldOf("home").forGetter(Flight::home),
                Codec.INT.fieldOf("slot").forGetter(Flight::slot),
                Identifier.CODEC.fieldOf("target_dimension").forGetter(Flight::targetDimension),
                BlockPos.CODEC.fieldOf("target").forGetter(Flight::target),
                ItemStack.CODEC.fieldOf("rocket").forGetter(Flight::rocket),
                Codec.LONG.fieldOf("start").forGetter(Flight::start),
                Codec.LONG.fieldOf("arrival").forGetter(Flight::arrival),
                Codec.BOOL.fieldOf("returning").forGetter(Flight::returning),
                Codec.STRING.fieldOf("status").forGetter(Flight::status),
                Codec.LONG.fieldOf("fuel").forGetter(Flight::fuel),
                ItemStack.CODEC.listOf().fieldOf("items").forGetter(Flight::items),
                IhFluidStack.CODEC.listOf().fieldOf("fluids").forGetter(Flight::fluids)
        ).apply(i, Flight::new));

        Flight with(long arrival, boolean returning, String status, long fuel, List<ItemStack> items, List<IhFluidStack> fluids) {
            return new Flight(id, homeDimension, home, slot, targetDimension, target, rocket, start, arrival, returning, status,
                    fuel, items, fluids);
        }
    }

    private record Stored(List<Flight> flights, List<Route> routes) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Flight.CODEC.listOf().fieldOf("flights").forGetter(Stored::flights),
                Route.CODEC.listOf().optionalFieldOf("routes", List.of()).forGetter(Stored::routes)
        ).apply(i, Stored::new));
    }

    public static final Codec<CargoFlights> CODEC = Stored.CODEC.xmap(s -> {
        CargoFlights f = new CargoFlights();
        s.flights().forEach(x -> f.flights.put(x.id(), x));
        s.routes().forEach(x -> f.routes.put(x.id(), x));
        return f;
    }, f -> new Stored(List.copyOf(f.flights.values()), List.copyOf(f.routes.values())));

    public static final SavedDataType<CargoFlights> TYPE = new SavedDataType<>(InfiniteHorizon.id("cargo_flights"),
            CargoFlights::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Flight> flights = new LinkedHashMap<>();
    /** Routen aller Agenturen (20.4, 3A.4 {@code CargoData}). */
    private final Map<UUID, Route> routes = new LinkedHashMap<>();
    /** Nächster Startversuch je Route und Hangar-Slot nach „ohne Treibstoff“ (nicht gespeichert). */
    private static final Map<String, Long> RETRY = new java.util.HashMap<>();

    public List<Route> routes() {
        return List.copyOf(routes.values());
    }

    public List<Route> routesOf(UUID agency) {
        return routes.values().stream().filter(r -> r.agency().equals(agency)).toList();
    }

    public Optional<Route> route(UUID id) {
        return Optional.ofNullable(routes.get(id));
    }

    public void putRoute(Route r) {
        routes.put(r.id(), r);
        setDirty();
    }

    public void removeRoute(UUID id) {
        if (routes.remove(id) != null) {
            setDirty();
        }
    }

    public CargoFlights() {
    }

    public static CargoFlights get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Flight> flights() {
        return List.copyOf(flights.values());
    }

    public Optional<Flight> flightOf(Identifier homeDimension, BlockPos home, int slot) {
        return flights.values().stream()
                .filter(f -> f.slot() == slot && f.home().equals(home) && f.homeDimension().equals(homeDimension)).findFirst();
    }

    /** Für Tests: Ankunft eines Flugs auf jetzt vorziehen. */
    public void arriveNow(UUID id, long now) {
        Flight f = flights.get(id);
        if (f != null) {
            flights.put(id, f.with(now, f.returning(), f.status(), f.fuel(), f.items(), f.fluids()));
            setDirty();
        }
    }

    // ------------------------------------------------------------------ Regeln

    private static JsonObject rules() {
        return BalanceManager.get().entry("logistics", "cargo_flight_rules").orElse(new JsonObject());
    }

    /** Entfernungspunkte der Dimension (12.2, planets.json). */
    public static double dp(Identifier dimension) {
        return PlanetRegistry.get().planet(dimension)
                .map(p -> p.raw().has("distance_points") ? p.raw().get("distance_points").getAsDouble() : 0.0).orElse(0.0);
    }

    public static boolean isFlying(ServerLevel level, BlockPos home, int slot) {
        return get(level.getServer()).flightOf(level.dimension().identifier(), home, slot).isPresent();
    }

    private static @Nullable ServerLevel level(MinecraftServer server, Identifier dimension) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    /** Ziel an {@code pos} (Chunk wird bei Bedarf geladen): Terminal, Werftdock oder Handelsposten. */
    private static @Nullable CargoSink sinkAt(@Nullable ServerLevel level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        level.getChunkAt(pos);
        return level.getBlockEntity(pos) instanceof CargoSink s ? s : null;
    }

    private static void animate(@Nullable ServerLevel level, BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    // ------------------------------------------------------------------ Start

    /**
     * Startet die Rakete aus Hangar-Slot {@code slot} des Terminals {@code home} zu {@code target} (20.3): tankt aus dem
     * Treibstofftank des Terminals (Frachter: Brennstab aus dem Lager, wenn {@code flights_left} = 0), lädt passende
     * Items und Fluide und legt den Flug an.
     */
    public static Launch launch(ServerLevel level, CargoTerminalBlockEntity home, int slot, Identifier targetDimension,
                                BlockPos target, Predicate<ItemStack> itemFilter, Predicate<Identifier> fluidFilter) {
        return launch(level, home, slot, targetDimension, target, itemFilter, fluidFilter, id -> Long.MAX_VALUE);
    }

    /** Wie oben, mit Höchstmenge je Item-ID (Bedarf der Werft, 20.4). */
    public static Launch launch(ServerLevel level, CargoTerminalBlockEntity home, int slot, Identifier targetDimension,
                                BlockPos target, Predicate<ItemStack> itemFilter, Predicate<Identifier> fluidFilter,
                                java.util.function.ToLongFunction<String> limit) {
        ItemStack rocket = home.hangar(slot);
        if (!CargoRockets.isRocket(rocket)) {
            return Launch.NO_ROCKET;
        }
        CargoFlights data = get(level.getServer());
        Identifier homeDim = level.dimension().identifier();
        if (data.flightOf(homeDim, home.getBlockPos(), slot).isPresent()) {
            return Launch.BUSY;
        }
        if (homeDim.equals(targetDimension) && home.getBlockPos().equals(target)
                || sinkAt(level(level.getServer(), targetDimension), target) == null) {
            return Launch.NO_TARGET;
        }
        double dpHome = dp(homeDim);
        double dpTarget = dp(targetDimension);
        long fuel = 0;
        JsonObject rod = CargoRockets.fuelItem(rocket);
        if (rod != null) {
            int left = CargoRockets.flightsLeft(rocket);
            if (left <= 0) {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getValue(Identifier.parse(rod.get("item").getAsString()));
                if (!home.takeItem(item, rod.get("count").getAsInt())) {
                    return Launch.OUT_OF_FUEL;
                }
                left = rod.get("flights").getAsInt();
            }
            CargoRockets.setFlightsLeft(rocket, left - 1);
        } else {
            long need = CargoRockets.fuelPerLeg(rocket, dpHome, dpTarget);
            JsonObject tank = CargoRockets.fuelTank(rocket);
            long capacity = tank == null ? 0 : tank.get("capacity").getAsLong();
            fuel = CargoRockets.fuel(rocket) + drawFuel(home.fuelTank(), capacity - CargoRockets.fuel(rocket));
            if (fuel < need) {
                CargoRockets.setFuel(rocket, fuel);
                home.setChanged();
                return Launch.OUT_OF_FUEL;
            }
            fuel -= need;
            CargoRockets.setFuel(rocket, fuel);
        }
        List<ItemStack> items = home.takeCargo(itemFilter, CargoRockets.itemSlots(rocket), limit);
        List<IhFluidStack> fluids = home.takeFluidCargo(fluidFilter, CargoRockets.fluidHolds(rocket),
                CargoRockets.fluidHoldCapacity(rocket));
        long now = OutpostManager.now(level);
        Flight f = new Flight(UUID.randomUUID(), homeDim, home.getBlockPos().immutable(), slot, targetDimension, target.immutable(),
                rocket.copy(), now, now + CargoRockets.flightTicks(rocket, dpHome, dpTarget), false, IN_FLIGHT, fuel, items, fluids);
        data.flights.put(f.id(), f);
        data.setDirty();
        home.setChanged();
        animate(level, home.getBlockPos());
        return Launch.OK;
    }

    /** Zieht bis zu {@code max} mB aus dem Treibstofftank. */
    private static long drawFuel(@Nullable IhFluidTank tank, long max) {
        if (tank == null || max <= 0 || tank.stack().isEmpty()) {
            return 0;
        }
        try (Transaction tx = Transaction.open()) {
            IhFluidStack got = tank.extract(tank.stack().fluid(), max, tx);
            tx.commit();
            return got.amount();
        }
    }

    // ------------------------------------------------------------------ Ankunft

    /** Jede Sekunde: fällige Flüge landen lassen (20.3). */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % OutpostManager.SNAPSHOT_INTERVAL != 0) {
            return;
        }
        process(server);
    }

    /** Bearbeitet alle fälligen Flüge (auch für Tests). */
    public static void process(MinecraftServer server) {
        CargoFlights data = get(server);
        if (data.flights.isEmpty() && data.routes.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        runRoutes(server, data, now);
        for (Flight f : List.copyOf(data.flights.values())) {
            if (f.arrival() <= now) {
                if (f.returning()) {
                    data.flights.remove(f.id());
                    returnHome(server, f);
                } else {
                    data.flights.put(f.id(), atTarget(server, f, now));
                }
                data.setDirty();
            }
        }
    }

    private static String key(Route r, int slot) {
        return r.id() + "#" + slot;
    }

    private static long retryTicks() {
        return rules().has("stranded_retry_ticks") ? rules().get("stranded_retry_ticks").getAsLong() : 0;
    }

    private static String itemId(ItemStack s) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    /** Offener Bedarf des Ziels für die Fracht der Route (leerer Filter: gesamter Bedarf). */
    private static long demand(CargoSink sink, Route r) {
        if (r.filter().isEmpty()) {
            return sink.totalDemand();
        }
        long d = 0;
        for (String id : r.filter()) {
            d += sink.demand(id);
        }
        return d;
    }

    /**
     * Routen (20.4): freie Raketen der zugewiesenen Hangar-Slots starten, sobald die Startbedingung gilt. Ein nicht
     * geladenes Heimatterminal wird nur alle {@code stranded_retry_ticks} geprüft (dabei wird der Außenposten
     * nachgerechnet, 20.1 „Logistik-Abfrage“).
     */
    static void runRoutes(MinecraftServer server, CargoFlights data, long now) {
        long retry = retryTicks();
        for (Route route : List.copyOf(data.routes.values())) {
            ServerLevel home = level(server, route.homeDimension());
            if (home == null || route.rockets().isEmpty()) {
                continue;
            }
            if (!home.isLoaded(route.home()) && retry > 0 && Math.floorMod(now, retry) >= OutpostManager.SNAPSHOT_INTERVAL) {
                continue;
            }
            home.getChunkAt(route.home());
            if (!(home.getBlockEntity(route.home()) instanceof CargoTerminalBlockEntity t)) {
                continue;
            }
            Route r = route;
            for (int slot : route.rockets()) {
                ItemStack rocket = t.hangar(slot);
                Long next = RETRY.get(key(r, slot));
                if (!CargoRockets.isRocket(rocket) || next != null && next > now
                        || data.flightOf(r.homeDimension(), r.home(), slot).isPresent()) {
                    continue;
                }
                ServerLevel targetLevel = level(server, r.targetDimension());
                CargoSink sink = sinkAt(targetLevel, r.target());
                // 23.3: Partner-Ziel nicht mehr freigegeben oder Partnerschaft beendet: nicht starten
                if (sink == null || !LogisticsService.mayDeliver(server, r.agency(), r.targetDimension(), r.target())) {
                    continue;
                }
                OutpostManager.catchUpOutpost(home, r.home());
                if (sink instanceof CargoTerminalBlockEntity && targetLevel != null) {
                    OutpostManager.catchUpOutpost(targetLevel, r.target());
                }
                boolean demandRoute = r.conditionType() == RouteRules.Condition.UNTIL_DEMAND_MET;
                Predicate<String> ok = id -> route.accepts(id) && (!demandRoute || sink.demand(id) > 0);
                RouteRules.Context ctx = new RouteRules.Context(now, r.lastStart(), r.amount(), t.matchingStacks(ok),
                        CargoRockets.itemSlots(rocket), t.matchingFluidHolds(ok), CargoRockets.fluidHolds(rocket),
                        sink.amountOf(r.item()), demandRoute ? demand(sink, r) : 0);
                if (!RouteRules.shouldStart(r.conditionType(), ctx)) {
                    continue;
                }
                Launch res = launch(home, t, slot, r.targetDimension(), r.target(), s -> ok.test(itemId(s)),
                        f -> ok.test(f.toString()), demandRoute ? sink::demand : id -> Long.MAX_VALUE);
                if (res == Launch.OK) {
                    RETRY.remove(key(r, slot));
                    r = r.withLastStart(now);
                    data.routes.put(r.id(), r);
                    data.setDirty();
                } else if (res == Launch.OUT_OF_FUEL) {
                    RETRY.put(key(r, slot), now + retry);
                }
            }
        }
    }

    /** Status einer Rakete der Route (20.4): unterwegs, geladen, wartend, ohne Treibstoff; dazu die Restzeit. */
    public record RocketStatus(UUID route, int slot, String status, long remaining) {
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, RocketStatus> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, RocketStatus::route,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT, RocketStatus::slot,
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RocketStatus::status,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, RocketStatus::remaining,
                        RocketStatus::new);
    }

    public static RocketStatus status(MinecraftServer server, Route r, int slot) {
        CargoFlights data = get(server);
        long now = server.overworld().getGameTime();
        Optional<Flight> f = data.flightOf(r.homeDimension(), r.home(), slot);
        if (f.isPresent()) {
            Flight x = f.get();
            String s = OUT_OF_FUEL.equals(x.status()) ? OUT_OF_FUEL
                    : !x.returning() && (!x.items().isEmpty() || !x.fluids().isEmpty()) ? "loaded" : IN_FLIGHT;
            return new RocketStatus(r.id(), slot, s, Math.max(0, x.arrival() - now));
        }
        Long next = RETRY.get(key(r, slot));
        return new RocketStatus(r.id(), slot, next != null && next > now ? OUT_OF_FUEL : "waiting", 0);
    }

    /** Am Ziel: Methan für den Rückflug behalten, entladen, nachtanken, zurückfliegen oder warten. */
    private static Flight atTarget(MinecraftServer server, Flight f, long now) {
        ServerLevel level = level(server, f.targetDimension());
        CargoSink sink = sinkAt(level, f.target());
        // 23.3: liefert nur, wenn das Ziel (noch) erlaubt ist; sonst bleibt die Fracht an Bord und fliegt zurück
        UUID agency = LogisticsService.ownerOf(server, f.homeDimension(), f.home());
        if (sink != null && !LogisticsService.mayDeliver(server, agency, f.targetDimension(), f.target())) {
            sink = null;
        }
        if (sink instanceof CargoTerminalBlockEntity && level != null) {
            OutpostManager.catchUpOutpost(level, f.target()); // 20.1: Frachtlandung rechnet den Außenposten nach
        }
        ItemStack rocket = f.rocket();
        double dpHome = dp(f.homeDimension());
        double dpTarget = dp(f.targetDimension());
        long fuel = f.fuel();
        List<ItemStack> items = new ArrayList<>(f.items());
        List<IhFluidStack> fluids = new ArrayList<>(f.fluids());
        JsonObject tank = CargoRockets.fuelTank(rocket);
        long need = tank == null ? 0 : CargoRockets.fuelPerLeg(rocket, dpTarget, dpHome);
        if (tank != null && fuel < need) {
            // Methan-Fracht: so viel behalten, wie der Rückflug braucht
            Identifier fuelId = Identifier.parse(tank.get("fluid").getAsString());
            for (int k = 0; k < fluids.size() && fuel < need; k++) {
                IhFluidStack c = fluids.get(k);
                if (c.is(fuelId)) {
                    long keep = Math.min(need - fuel, c.amount());
                    fuel += keep;
                    fluids.set(k, c.withAmount(c.amount() - keep));
                }
            }
        }
        if (sink != null) {
            List<ItemStack> restItems = new ArrayList<>();
            for (ItemStack s : items) {
                ItemStack rest = sink.insertItem(s, false);
                if (!rest.isEmpty()) {
                    restItems.add(rest);
                }
            }
            boolean delivered = restItems.size() < items.size()
                    || restItems.stream().mapToInt(ItemStack::getCount).sum() < items.stream().mapToInt(ItemStack::getCount).sum();
            items = restItems;
            List<IhFluidStack> restFluids = new ArrayList<>();
            for (IhFluidStack c : fluids) {
                if (c.isEmpty()) {
                    continue;
                }
                long moved = sink.acceptsFluidCargo() ? sink.insertFluid(c, false) : 0;
                if (moved < c.amount()) {
                    restFluids.add(c.withAmount(c.amount() - moved));
                }
            }
            delivered |= restFluids.stream().mapToLong(IhFluidStack::amount).sum()
                    < fluids.stream().filter(c -> !c.isEmpty()).mapToLong(IhFluidStack::amount).sum();
            fluids = restFluids;
            // 23.3 „Gemeinsame Werft“: liefernde Agentur merken
            if (delivered && agency != null && sink instanceof de.infinitehorizon.shipyard.ShipyardDockBlockEntity dock) {
                var yard = dock.yard();
                if (yard != null) {
                    de.infinitehorizon.shipyard.ShipyardState.get(server).addContributor(yard, agency);
                }
            }
            if (tank != null && fuel < need) {
                fuel += drawFuel(sink.fuelTank(), need - fuel);
            }
        } else {
            fluids.removeIf(IhFluidStack::isEmpty);
        }
        if (tank != null && fuel < need) {
            long retry = rules().has("stranded_retry_ticks") ? rules().get("stranded_retry_ticks").getAsLong() : 0;
            return f.with(now + retry, false, OUT_OF_FUEL, fuel, items, fluids);
        }
        animate(level, f.target());
        // 20.3: Rückflug leer; was am Ziel nicht passte, bleibt an Bord und kommt zurück (SPEC-GAP #113)
        return f.with(now + CargoRockets.flightTicks(rocket, dpTarget, dpHome), true, IN_FLIGHT, fuel - need, items, fluids);
    }

    /** Zurück im Hangar: Treibstoff ins Item, übrig gebliebene Fracht ins Lager (sonst daneben fallen lassen). */
    private static void returnHome(MinecraftServer server, Flight f) {
        ServerLevel level = level(server, f.homeDimension());
        if (level == null) {
            return;
        }
        level.getChunkAt(f.home());
        if (!(level.getBlockEntity(f.home()) instanceof CargoTerminalBlockEntity home)) {
            return;
        }
        ItemStack rocket = home.hangar(f.slot());
        if (CargoRockets.fuelTank(rocket) != null) {
            CargoRockets.setFuel(rocket, f.fuel());
        }
        for (ItemStack s : f.items()) {
            ItemStack rest = home.insertItem(s, false);
            if (!rest.isEmpty()) {
                Block.popResource(level, f.home().above(), rest);
            }
        }
        for (IhFluidStack c : f.fluids()) {
            home.insertFluid(c, false);
        }
        home.setChanged();
        animate(level, f.home());
    }

    /** Terminal abgebaut: seine Flüge enden (die Rakete fällt mit dem Inhalt des Terminals heraus). */
    public static void onTerminalRemoved(ServerLevel level, BlockPos pos) {
        CargoFlights data = get(level.getServer());
        Identifier dim = level.dimension().identifier();
        boolean changed = data.flights.values().removeIf(f -> f.home().equals(pos) && f.homeDimension().equals(dim));
        changed |= data.routes.values().removeIf(r -> r.home().equals(pos) && r.homeDimension().equals(dim)
                || r.target().equals(pos) && r.targetDimension().equals(dim));
        if (changed) {
            data.setDirty();
        }
    }
}
