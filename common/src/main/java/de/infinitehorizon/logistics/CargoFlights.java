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

    public static final Codec<CargoFlights> CODEC = Flight.CODEC.listOf().xmap(l -> {
        CargoFlights f = new CargoFlights();
        l.forEach(x -> f.flights.put(x.id(), x));
        return f;
    }, f -> List.copyOf(f.flights.values()));

    public static final SavedDataType<CargoFlights> TYPE = new SavedDataType<>(InfiniteHorizon.id("cargo_flights"),
            CargoFlights::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Flight> flights = new LinkedHashMap<>();

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
        List<ItemStack> items = home.takeCargo(itemFilter, CargoRockets.itemSlots(rocket));
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
        if (data.flights.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
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

    /** Am Ziel: Methan für den Rückflug behalten, entladen, nachtanken, zurückfliegen oder warten. */
    private static Flight atTarget(MinecraftServer server, Flight f, long now) {
        ServerLevel level = level(server, f.targetDimension());
        CargoSink sink = sinkAt(level, f.target());
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
            fluids = restFluids;
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
        if (data.flights.values().removeIf(f -> f.home().equals(pos) && f.homeDimension().equals(dim))) {
            data.setDirty();
        }
    }
}
