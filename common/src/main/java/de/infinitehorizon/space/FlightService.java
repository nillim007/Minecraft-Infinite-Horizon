package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.network.FlightSequencePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.vehicle.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Raketenflug auf dem Server (12.5, 3A.2): Start prüfen, Raketenblöcke in ein {@link RocketEntity} umwandeln, Treibstoff
 * abziehen, Countdown, Aufstieg um {@code ascent_height}, Teleport in die Zieldimension auf
 * min(Landepunkt + {@code arrival_height}, Bauhöhe − Raketenhöhe) und lineare Landung in {@code landing_ticks}. Die
 * Client-Sequenz ist nur Darstellung; {@code skip}, Tod und Abmelden schließen den Flug sofort ab.
 */
public final class FlightService {

    /**
     * Landeort: Landefeuer ({@code beacon}) oder X/Z; beides leer = Spawn der Dimension (Overworld: Weltspawn, sonst
     * 0/0).
     */
    public record Landing(Optional<BlockPos> beacon, Optional<Integer> x, Optional<Integer> z) {
        public static final Landing DEFAULT = new Landing(Optional.empty(), Optional.empty(), Optional.empty());
        public static final Codec<Landing> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.optionalFieldOf("beacon").forGetter(Landing::beacon),
                Codec.INT.optionalFieldOf("x").forGetter(Landing::x),
                Codec.INT.optionalFieldOf("z").forGetter(Landing::z)
        ).apply(i, Landing::new));

        public static Landing at(int x, int z) {
            return new Landing(Optional.empty(), Optional.of(x), Optional.of(z));
        }

        public static Landing beacon(BlockPos pos) {
            return new Landing(Optional.of(pos), Optional.empty(), Optional.empty());
        }
    }

    /** Flugzustand einer Rakete (in ihren Speicherdaten, übersteht den Dimensionswechsel). */
    public record Flight(Identifier target, RocketEntity.Phase phase, int ticks, double startY, Landing landing,
                         Optional<BlockPos> point, double arrivalY) {
        public static final Codec<Flight> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("target").forGetter(Flight::target),
                Codec.INT.xmap(n -> RocketEntity.Phase.values()[Math.max(0, Math.min(n, RocketEntity.Phase.values().length - 1))],
                        RocketEntity.Phase::ordinal).fieldOf("phase").forGetter(Flight::phase),
                Codec.INT.fieldOf("ticks").forGetter(Flight::ticks),
                Codec.DOUBLE.fieldOf("start_y").forGetter(Flight::startY),
                Landing.CODEC.fieldOf("landing").forGetter(Flight::landing),
                BlockPos.CODEC.optionalFieldOf("point").forGetter(Flight::point),
                Codec.DOUBLE.optionalFieldOf("arrival_y", 0.0).forGetter(Flight::arrivalY)
        ).apply(i, Flight::new));

        Flight with(RocketEntity.Phase phase, int ticks) {
            return new Flight(target, phase, ticks, startY, landing, point, arrivalY);
        }
    }

    private FlightService() {
    }

    // ---------------------------------------------------------------- Werte

    private static JsonObject flightRules() {
        return BalanceManager.get().entry("spaceflight", "rocket_flight").orElse(new JsonObject());
    }

    private static int rule(String key) {
        JsonObject r = flightRules();
        return r.has(key) && r.get(key).isJsonPrimitive() ? r.get(key).getAsInt() : 0;
    }

    private static SoundEvent sound(String name) {
        return BuiltInRegistries.SOUND_EVENT.getValue(InfiniteHorizon.id(name));
    }

    private static @Nullable ServerLevel level(MinecraftServer server, Identifier dimension) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    // ---------------------------------------------------------------- Ziele

    /**
     * Ziele für eine Rakete (12.1): Planeten-Einträge mit vorhandener Dimension; freigeschaltet sind der eigene Körper,
     * die Erde, {@code earth_orbit} und Ziele in {@code probe_reached}, wenn die Stufe reicht.
     */
    public static List<LaunchChecklist.Destination> destinations(ServerLevel level, long fuel, int tier, @Nullable Agency agency) {
        Planet here = PlanetResolver.planetFor(level.dimension().identifier(), PlanetRegistry.get());
        return LaunchChecklist.destinations(level, fuel, tier, here, agency);
    }

    // ---------------------------------------------------------------- Start

    /** Start von der Rampe (Navigationskarte → {@code LaunchRequestPayload}); gibt einen Fehlerschlüssel oder null zurück. */
    public static @Nullable String launchFromPad(ServerLevel level, LaunchControlBlockEntity control, ServerPlayer requester,
                                                 Identifier target, Landing landing) {
        if (!Permissions.can(requester, control.storedOwner(), Permissions.Action.LAUNCH)) {
            return "permission";
        }
        LaunchChecklist.State s = control.evaluate(level);
        if (s.load() != LaunchPad.Load.ROCKET || !s.ready() || s.rocket() == null || s.pad() == null) {
            return "checklist";
        }
        Agency agency = AgencyManager.ensureFor(requester);
        LaunchChecklist.Destination dest = destinations(level, s.rocket().usableFuel(), s.rocket().tier(), agency).stream()
                .filter(d -> d.dimension().equals(target)).findFirst().orElse(null);
        if (dest == null || !dest.unlocked() || dest.reach() == FuelCalculator.Reach.NONE) {
            return "target";
        }
        LaunchPad.Check pad = s.pad();
        // Teile, Flossen und Fracht übernehmen, Blöcke entfernen
        BlockPos engine = pad.stack().getFirst();
        Identifier fin = null;
        List<BlockPos> remove = new ArrayList<>(pad.stack());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = engine.relative(d);
            Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock());
            if (de.infinitehorizon.data.RocketPartRegistry.get().part(id)
                    .filter(part -> part.role() == de.infinitehorizon.data.RocketPartRegistry.Role.FIN).isPresent()) {
                fin = id;
                remove.add(p);
            }
        }
        List<ItemStack> cargo = new ArrayList<>();
        for (BlockPos p : pad.stack()) {
            if (level.getBlockEntity(p) instanceof CargoBayBlockEntity bay) {
                for (int i = 0; i < bay.getContainerSize(); i++) {
                    cargo.add(bay.getItem(i).copy());
                }
                bay.clearContent();
            }
        }
        List<Player> crew = new ArrayList<>();
        for (SeatEntity seat : CapsuleBlock.seats(level, pad.stack().getLast())) {
            for (Entity e : seat.getPassengers()) {
                if (e instanceof Player p) {
                    crew.add(p);
                }
            }
        }
        RocketEntity rocket = ModEntities.ROCKET.get().create(level, EntitySpawnReason.TRIGGERED);
        if (rocket == null) {
            return "spawn";
        }
        rocket.configure(s.rocket(), fin, cargo, agency.id());
        for (BlockPos p : remove) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        rocket.setPos(engine.getX() + 0.5, engine.getY(), engine.getZ() + 0.5);
        level.addFreshEntity(rocket);
        for (Player p : crew) {
            p.stopRiding();
            p.startRiding(rocket);
        }
        start(level, rocket, target, landing, dest.outbound());
        return null;
    }

    /** Neustart einer gelandeten Rakete (12.5: überall ohne Rampe). */
    public static @Nullable String launchFromRocket(ServerLevel level, RocketEntity rocket, ServerPlayer requester,
                                                    Identifier target, Landing landing) {
        if (rocket.inFlight()) {
            return "busy";
        }
        if (!Permissions.can(requester, rocket.owner(), Permissions.Action.LAUNCH)) {
            return "permission";
        }
        if (rocket.getPassengers().stream().noneMatch(e -> e instanceof Player p && Suits.worn(p) != null)) {
            return "crew";
        }
        Planet here = PlanetResolver.planetFor(level.dimension().identifier(), PlanetRegistry.get());
        double g = here == null ? 1.0 : here.gravity();
        double min = BalanceManager.get().entry("spaceflight", "thrust_to_weight")
                .filter(o -> o.has("min_ratio")).map(o -> o.get("min_ratio").getAsDouble()).orElse(1.0);
        boolean orbit = here != null && here.isOrbit();
        if (!orbit && g > 0 && rocket.thrust() / (rocket.mass() * g) < min) {
            return "thrust";
        }
        Agency agency = AgencyManager.ensureFor(requester);
        LaunchChecklist.Destination dest = destinations(level, rocket.usableFuel(), rocket.tier(), agency).stream()
                .filter(d -> d.dimension().equals(target)).findFirst().orElse(null);
        if (dest == null || !dest.unlocked() || dest.reach() == FuelCalculator.Reach.NONE) {
            return "target";
        }
        start(level, rocket, target, landing, dest.outbound());
        return null;
    }

    private static void start(ServerLevel level, RocketEntity rocket, Identifier target, Landing landing, long cost) {
        rocket.burn(cost);
        rocket.setFlight(new Flight(target, RocketEntity.Phase.COUNTDOWN, 0, rocket.getY(), landing, Optional.empty(), 0));
        level.playSound(null, rocket.blockPosition(), sound("rocket.countdown"), SoundSource.NEUTRAL, 1f, 1f);
        // Offene GUIs der Insassen (Navigationskarte, Raketen-GUI) vorher schließen: Das Schließen-Paket beendet auf dem
        // Client jeden Bildschirm, käme es danach, wäre auch die Flugsequenz weg
        for (Entity e : rocket.getPassengers()) {
            if (e instanceof ServerPlayer sp && sp.containerMenu != sp.inventoryMenu) {
                sp.closeContainer();
            }
        }
        sequence(rocket, FlightSequencePayload.START, rule("countdown_ticks") + rule("ascent_ticks"));
    }

    private static void sequence(RocketEntity rocket, byte type, int ticks) {
        for (Entity e : rocket.getPassengers()) {
            if (e instanceof ServerPlayer sp) {
                Identifier target = rocket.flight() == null ? rocket.level().dimension().identifier() : rocket.flight().target();
                Services.NETWORK.sendToPlayer(sp, new FlightSequencePayload(target, type, ticks));
            }
        }
    }

    // ---------------------------------------------------------------- Ablauf

    /** Jeder Tick einer Rakete im Flug. */
    public static void tick(ServerLevel level, RocketEntity r) {
        Flight f = r.flight();
        if (f == null) {
            return;
        }
        int t = f.ticks() + 1;
        switch (f.phase()) {
            case COUNTDOWN -> {
                if (t >= rule("countdown_ticks")) {
                    r.setFlight(f.with(RocketEntity.Phase.ASCENT, 0));
                    level.playSound(null, r.blockPosition(), sound("rocket.launch"), SoundSource.NEUTRAL, 1f, 1f);
                } else {
                    r.setFlight(f.with(f.phase(), t));
                }
            }
            case ASCENT -> {
                int ticks = Math.max(1, rule("ascent_ticks"));
                if (t >= ticks) {
                    arrive(level, r);
                } else {
                    r.setPos(r.getX(), f.startY() + (double) rule("ascent_height") * t / ticks, r.getZ());
                    r.setFlight(f.with(f.phase(), t));
                }
            }
            case LANDING -> {
                int ticks = Math.max(1, rule("landing_ticks"));
                BlockPos point = f.point().orElse(r.blockPosition());
                if (t >= ticks) {
                    land(level, r);
                } else {
                    r.setPos(point.getX() + 0.5, f.arrivalY() + (point.getY() - f.arrivalY()) * t / ticks, point.getZ() + 0.5);
                    r.setFlight(f.with(f.phase(), t));
                }
            }
            default -> {
            }
        }
    }

    /** Ende des Aufstiegs: Landepunkt suchen, Chunk laden, in die Zieldimension teleportieren. */
    static @Nullable RocketEntity arrive(ServerLevel from, RocketEntity r) {
        Flight f = r.flight();
        ServerLevel target = f == null ? null : level(from.getServer(), f.target());
        if (f == null || target == null) {
            return r;
        }
        BlockPos point = f.point().orElseGet(() -> landingPoint(target, f.landing(), r.owner()));
        target.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(point), 2);
        double arrivalY = Math.min(point.getY() + rule("arrival_height"), target.getMaxY() - r.height());
        Flight landing = new Flight(f.target(), RocketEntity.Phase.LANDING, 0, f.startY(), f.landing(), Optional.of(point), arrivalY);
        r.setFlight(landing);
        // Der Dimensionswechsel wirft die Insassen kurz ab und setzt sie in die neue Instanz (api-entities-26.3.md, 4.)
        r.allowDismount = true;
        Entity moved = r.teleport(new TeleportTransition(target, new Vec3(point.getX() + 0.5, arrivalY, point.getZ() + 0.5),
                Vec3.ZERO, r.getYRot(), r.getXRot(), TeleportTransition.DO_NOTHING));
        r.allowDismount = false;
        RocketEntity next = moved instanceof RocketEntity re ? re : r;
        next.setFlight(landing);
        target.playSound(null, point, sound("rocket.landing"), SoundSource.NEUTRAL, 1f, 1f);
        sequence(next, FlightSequencePayload.LANDING, rule("landing_ticks"));
        return next;
    }

    /** Aufsetzen: Rakete steht auf dem Landepunkt, Schwerkraft des Planeten für die Insassen (12.9). */
    static void land(ServerLevel level, RocketEntity r) {
        Flight f = r.flight();
        BlockPos point = f == null ? r.blockPosition() : f.point().orElse(r.blockPosition());
        r.setPos(point.getX() + 0.5, point.getY(), point.getZ() + 0.5);
        r.setFlight(null);
        sequence(r, FlightSequencePayload.END, 0);
        for (Entity e : r.getPassengers()) {
            if (e instanceof ServerPlayer sp) {
                GravityService.apply(sp);
            }
        }
    }

    /**
     * Flug sofort abschließen ({@code skip}, Abmelden, Tod; 12.5): Aufstieg überspringen, teleportieren und landen.
     * Gibt die Rakete am Ziel zurück (nach einem Dimensionswechsel eine neue Instanz).
     */
    public static RocketEntity complete(RocketEntity r) {
        if (!(r.level() instanceof ServerLevel level) || r.flight() == null) {
            return r;
        }
        RocketEntity current = r;
        if (current.flight().phase() != RocketEntity.Phase.LANDING) {
            RocketEntity moved = arrive(level, current);
            current = moved == null ? current : moved;
        }
        if (current.level() instanceof ServerLevel target && current.flight() != null) {
            land(target, current);
        }
        return current;
    }

    // ---------------------------------------------------------------- Landeplatz

    /**
     * Landepunkt (12.5, 12.6): Landefeuer → Spiralsuche im Radius {@code beacon_landing_radius} ohne seinen Block,
     * sonst normale Landeplatzsuche um das Landefeuer; X/Z → Landeplatzsuche dort; nichts → Spawn der Dimension.
     */
    public static BlockPos landingPoint(ServerLevel level, Landing landing) {
        return landingPoint(level, landing, null);
    }

    /** Wie {@link #landingPoint(ServerLevel, Landing)}; im Orbit schwebt die Rakete (12.5, 7.), fremde Kerne meidet sie. */
    public static BlockPos landingPoint(ServerLevel level, Landing landing, @Nullable UUID agency) {
        Planet planet = PlanetResolver.planetFor(level.dimension().identifier(), PlanetRegistry.get());
        if (planet != null && planet.isOrbit()) {
            BlockPos at = landing.beacon().orElse(null);
            return OrbitLanding.find(level, at != null ? at.getX() : landing.x().orElse(0),
                    at != null ? at.getZ() : landing.z().orElse(0), agency);
        }
        if (CloudLayer.hasLayer(level)) {
            // SPEC-GAP #90: Landeort (Landefeuer, X/Z oder Spawn) gibt nur X/Z vor, gelandet wird in der Wolkenschicht
            BlockPos at = landing.beacon().orElse(null);
            int x = at != null ? at.getX() : landing.x().orElse(0);
            int z = at != null ? at.getZ() : landing.z().orElse(0);
            return CloudLayer.landingPoint(level, x, z);
        }
        if (landing.beacon().isPresent()) {
            BlockPos b = landing.beacon().get();
            int radius = rule("beacon_landing_radius");
            for (int r = 1; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, b.offset(dx, 0, dz));
                        BlockPos ground = top.below();
                        if (level.getBlockState(ground).isSolid() && level.getFluidState(ground).isEmpty()
                                && level.getFluidState(top).isEmpty() && !ground.equals(b)) {
                            return top;
                        }
                    }
                }
            }
            return LandingService.find(level, b.getX(), b.getZ());
        }
        if (landing.x().isPresent() && landing.z().isPresent()) {
            return LandingService.find(level, landing.x().get(), landing.z().get());
        }
        if (level.dimension().equals(Level.OVERWORLD)) {
            BlockPos spawn = level.getServer().getRespawnData().pos();
            return LandingService.find(level, spawn.getX(), spawn.getZ());
        }
        return LandingService.find(level, 0, 0);
    }

    // ---------------------------------------------------------------- Zerlegen, Abmelden

    /** Zerlegen (12.5): Teile und Fracht als Items, der Treibstoff geht verloren. */
    public static List<ItemStack> disassemble(ServerLevel level, RocketEntity r) {
        r.allowDismount = true;
        r.ejectPassengers();
        List<ItemStack> drops = new ArrayList<>(r.partItems());
        for (int i = 0; i < r.cargo().getContainerSize(); i++) {
            if (!r.cargo().getItem(i).isEmpty()) {
                drops.add(r.cargo().getItem(i).copy());
            }
        }
        for (ItemStack s : drops) {
            level.addFreshEntity(new ItemEntity(level, r.getX(), r.getY() + 0.5, r.getZ(), s.copy()));
        }
        r.discard();
        return drops;
    }

    /**
     * Abmelden (12.5, vor {@code PlayerList#remove}): Im Flug bekommt der Spieler {@code pending_flight}, steigt aus,
     * und der Flug wird sofort abgeschlossen; außerhalb eines Flugs steigt er nur aus.
     */
    public static void onLogout(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof RocketEntity r) {
            if (r.inFlight()) {
                r.allowDismount = true;
                player.stopRiding();
                r.allowDismount = false;
                RocketEntity landed = complete(r);
                Services.ATTACHMENTS.setPendingFlight(player, new PendingFlight(landed.level().dimension().identifier(),
                        landed.getUUID(), landed.blockPosition()));
            } else {
                player.stopRiding();
            }
        } else if (vehicle instanceof SeatEntity) {
            player.stopRiding();
        }
    }

    /** Spieler, deren {@link #onLogin} im nächsten Server-Tick läuft. */
    private static final java.util.Set<UUID> LOGINS = new java.util.LinkedHashSet<>();
    /** Spieler neben ihrer Rakete, die einsteigen, sobald deren Chunk sie sichtbar macht: Rakete und Rest-Ticks. */
    private static final java.util.Map<UUID, Boarding> BOARDING = new java.util.LinkedHashMap<>();

    private record Boarding(UUID rocket, int ticksLeft) {
    }

    /**
     * Join-Hook: {@link #onLogin} erst im nächsten Server-Tick. Fabric meldet den Join mitten in
     * {@code PlayerList#placeNewPlayer}, bevor der Spieler in der Welt steht; Teleport und Einsteigen gingen dort verloren.
     */
    public static void queueLogin(ServerPlayer player) {
        PendingFlight pending = Services.ATTACHMENTS.pendingFlight(player);
        if (pending != null && !pending.isNone()) {
            LOGINS.add(player.getUUID());
        }
    }

    /** Jeder Server-Tick: vorgemerkte Logins abarbeiten, wartende Spieler einsteigen lassen. */
    public static void tickLogins(MinecraftServer server) {
        if (!LOGINS.isEmpty()) {
            List<UUID> due = new ArrayList<>(LOGINS);
            LOGINS.clear();
            for (UUID id : due) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    onLogin(p);
                }
            }
        }
        if (BOARDING.isEmpty()) {
            return;
        }
        for (var it = BOARDING.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            if (p == null || board(p, entry.getValue().rocket()) || entry.getValue().ticksLeft() <= 1) {
                it.remove();
            } else {
                entry.setValue(new Boarding(entry.getValue().rocket(), entry.getValue().ticksLeft() - 1));
            }
        }
    }

    private static boolean board(ServerPlayer player, UUID rocket) {
        if (player.isPassenger()) {
            return true;
        }
        Entity e = ((ServerLevel) player.level()).getEntity(rocket);
        return e instanceof RocketEntity r && !r.inFlight() && player.startRiding(r);
    }

    /**
     * Login: Wer sich im Flug abgemeldet hat, steht an der gelandeten Rakete und sitzt wieder darin, sobald ihr Chunk sie
     * sichtbar macht (frisch geladene Chunks und Entities nach einem Neustart erst nach einigen Ticks). SPEC-GAP #78:
     * gewartet wird höchstens {@code landing_ticks}; danach steht der Spieler daneben.
     */
    public static void onLogin(ServerPlayer player) {
        PendingFlight pending = Services.ATTACHMENTS.pendingFlight(player);
        if (pending == null || pending.isNone()) {
            return;
        }
        Services.ATTACHMENTS.setPendingFlight(player, PendingFlight.NONE);
        ServerLevel level = level(player.level().getServer(), pending.dimension());
        if (level == null) {
            return;
        }
        level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(pending.landing()), 1);
        Entity e = level.getEntity(pending.rocket());
        Vec3 at = e != null ? e.position() : Vec3.atBottomCenterOf(pending.landing());
        Entity moved = player.teleport(new TeleportTransition(level, at, Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.DO_NOTHING));
        if (moved instanceof ServerPlayer sp && !board(sp, pending.rocket())) {
            BOARDING.put(sp.getUUID(), new Boarding(pending.rocket(), rule("landing_ticks")));
        }
    }

    /** Für Meldungen. */
    public static Component targetName(Identifier dim) {
        return Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath());
    }

    /** Insassen einer Rakete (für Tests). */
    public static List<UUID> crew(RocketEntity r) {
        List<UUID> out = new ArrayList<>();
        r.getPassengers().forEach(e -> out.add(e.getUUID()));
        return out;
    }
}
