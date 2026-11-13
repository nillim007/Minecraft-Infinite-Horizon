package de.infinitehorizon.space;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.vehicle.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checkliste und Raketen-Computer von {@code launch_control} (12.4): Struktur gültig, Schub ≥ Gewicht, Treibstoff für
 * mindestens ein Ziel, mindestens ein Ziel freigeschaltet, Spieler mit vollständigem Anzug sitzt in der Kapsel. Bei
 * einer Sonde: Struktur und Ziel (12.3).
 */
public final class LaunchChecklist {

    public static final String STRUCTURE = "structure";
    public static final String THRUST = "thrust";
    public static final String FUEL = "fuel";
    public static final String TARGET = "target";
    public static final String CREW = "crew";

    /** Eine Zeile mit Haken oder Kreuz und dem ersten Fehler. */
    public record Line(String key, boolean ok, RocketAssembler.@Nullable Problem problem) {
    }

    /** Ein Raketenziel mit Kosten hin und zurück und Farbe (12.2). */
    public record Destination(Identifier dimension, long outbound, long back, FuelCalculator.Reach reach, boolean unlocked) {
    }

    /** Ein Sondenziel; {@code reached} = schon erreicht (dann ohne FP). */
    public record ProbeTarget(Identifier dimension, boolean reached) {
    }

    public record State(LaunchPad.@Nullable Check pad, @Nullable RocketConfig rocket, List<Line> lines,
                        List<Destination> destinations, List<ProbeTarget> probeTargets, double thrustRatio) {

        public boolean ready() {
            return !lines.isEmpty() && lines.stream().allMatch(Line::ok);
        }

        public LaunchPad.Load load() {
            return pad == null ? LaunchPad.Load.NONE : pad.load();
        }

        public boolean ok(String key) {
            return lines.stream().anyMatch(l -> l.key().equals(key) && l.ok());
        }
    }

    private LaunchChecklist() {
    }

    private static double minRatio() {
        return BalanceManager.get().entry("spaceflight", "thrust_to_weight")
                .filter(o -> o.has("min_ratio")).map(o -> o.get("min_ratio").getAsDouble()).orElse(1.0);
    }

    /** Prüft die nächste Rampe von {@code control} für die Agentur {@code agency} (Ziele aus ihrem Fortschritt). */
    public static State evaluate(ServerLevel level, BlockPos control, @Nullable Agency agency) {
        LaunchPad.Check pad = LaunchPad.nearest(level, control);
        List<Line> lines = new ArrayList<>();
        if (pad == null) {
            lines.add(new Line(STRUCTURE, false, new RocketAssembler.Problem("no_pad", 0, 0)));
            return new State(null, null, lines, List.of(), List.of(), 0);
        }
        if (pad.load() == LaunchPad.Load.PROBE) {
            return probe(pad, agency, lines);
        }
        if (pad.load() == LaunchPad.Load.NONE) {
            lines.add(new Line(STRUCTURE, false, new RocketAssembler.Problem("no_load", 0, 0)));
            return new State(pad, null, lines, List.of(), List.of(), 0);
        }
        RocketAssembler.Result result = RocketAssembler.assemble(snapshot(level, pad), RocketPartRegistry.get());
        RocketAssembler.Problem first = !pad.problems().isEmpty() ? pad.problems().getFirst()
                : result.problems().isEmpty() ? null : result.problems().getFirst();
        lines.add(new Line(STRUCTURE, first == null && result.config() != null, first));
        RocketConfig cfg = result.config();

        Planet here = PlanetResolver.planetFor(level.dimension().identifier(), PlanetRegistry.get());
        boolean orbit = here != null && here.isOrbit();
        double g = here == null ? 1.0 : here.gravity();
        double ratio = cfg == null ? 0 : cfg.thrustRatio(g);
        lines.add(new Line(THRUST, cfg != null && (orbit || ratio >= minRatio()),
                cfg == null ? null : new RocketAssembler.Problem("thrust", (int) Math.round(ratio * 100), (int) Math.round(minRatio() * 100))));

        List<Destination> dests = destinations(level, cfg, here, agency);
        boolean anyUnlocked = dests.stream().anyMatch(Destination::unlocked);
        boolean anyFuel = dests.stream().anyMatch(d -> d.unlocked() && d.reach() != FuelCalculator.Reach.NONE);
        lines.add(new Line(FUEL, anyFuel, new RocketAssembler.Problem("fuel", (int) (cfg == null ? 0 : cfg.usableFuel()), 0)));
        lines.add(new Line(TARGET, anyUnlocked, new RocketAssembler.Problem("target", 0, 0)));

        int seated = 0;
        int suited = 0;
        if (!pad.stack().isEmpty()) {
            for (SeatEntity seat : CapsuleBlock.seats(level, pad.stack().getLast())) {
                for (Entity e : seat.getPassengers()) {
                    if (e instanceof Player p) {
                        seated++;
                        if (Suits.worn(p) != null) {
                            suited++;
                        }
                    }
                }
            }
        }
        lines.add(new Line(CREW, suited > 0, new RocketAssembler.Problem(seated == 0 ? "crew" : "suit", seated, suited)));
        return new State(pad, cfg, List.copyOf(lines), dests, List.of(), ratio);
    }

    private static State probe(LaunchPad.Check pad, @Nullable Agency agency, List<Line> lines) {
        lines.add(new Line(STRUCTURE, pad.valid(), pad.problems().isEmpty() ? null : pad.problems().getFirst()));
        List<ProbeTarget> targets = new ArrayList<>();
        if (agency != null) {
            Set<Identifier> reached = agency.progress().probeReached();
            for (Identifier t : agency.progress().probeTargets()) {
                if (PlanetRegistry.get().planet(t).isPresent()) {
                    targets.add(new ProbeTarget(t, reached.contains(t)));
                }
            }
        }
        lines.add(new Line(TARGET, !targets.isEmpty(), new RocketAssembler.Problem("probe_target", 0, 0)));
        return new State(pad, null, List.copyOf(lines), List.of(), List.copyOf(targets), 0);
    }

    private static List<Destination> destinations(ServerLevel level, @Nullable RocketConfig cfg, @Nullable Planet here,
                                                  @Nullable Agency agency) {
        return destinations(level, cfg == null ? 0 : cfg.usableFuel(), cfg == null ? 0 : cfg.tier(), here, agency);
    }

    /**
     * Ziele (12.1): Planeten-Einträge, deren Dimension es gibt; freigeschaltet sind der eigene Körper, die Erde,
     * {@code earth_orbit} und Ziele in {@code probe_reached}, wenn die Stufe reicht.
     */
    public static List<Destination> destinations(ServerLevel level, long fuel, int tier, @Nullable Planet here,
                                                 @Nullable Agency agency) {
        if (here == null) {
            return List.of();
        }
        FuelCalculator.Constants c = FuelCalculator.Constants.from(BalanceManager.get());
        FuelCalculator.Body from = FuelCalculator.Body.of(here);
        Set<Identifier> reached = agency == null ? Set.of() : agency.progress().probeReached();
        List<Destination> out = new ArrayList<>();
        for (Planet p : PlanetRegistry.get().all()) {
            if (level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, p.dimension())) == null) {
                continue;
            }
            JsonObject raw = p.raw();
            int required = raw.has("required_rocket_tier") && !raw.get("required_rocket_tier").isJsonNull()
                    ? raw.get("required_rocket_tier").getAsInt() : 0;
            boolean longRange = raw.has("requires_long_range") && raw.get("requires_long_range").getAsBoolean();
            boolean known = p.dimension().equals(here.dimension()) || p.dimension().equals(PlanetResolver.EARTH)
                    || p.dimension().getPath().equals("earth_orbit") || reached.contains(p.dimension());
            boolean unlocked = known && tier >= required && !longRange;
            FuelCalculator.Body to = FuelCalculator.Body.of(p);
            out.add(new Destination(p.dimension(), FuelCalculator.cost(from, to, c), FuelCalculator.cost(to, from, c),
                    FuelCalculator.reach(fuel, from, to, c), unlocked));
        }
        return List.copyOf(out);
    }

    /** Blöcke der Rampe als Schnappschuss für {@link RocketAssembler}. */
    public static RocketAssembler.Snapshot snapshot(ServerLevel level, LaunchPad.Check pad) {
        List<Identifier> stack = LaunchPad.ids(level, pad.stack());
        List<@Nullable Identifier> fins = new ArrayList<>();
        BlockPos engine = pad.stack().isEmpty() ? pad.center().above() : pad.stack().getFirst();
        RocketPartRegistry reg = RocketPartRegistry.get();
        for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(engine.relative(d)).getBlock());
            fins.add(reg.part(id).isPresent() ? id : null);
        }
        List<long[]> fuel = new ArrayList<>();
        int cargo = 0;
        for (BlockPos p : pad.stack()) {
            if (level.getBlockEntity(p) instanceof RocketTankBlockEntity t) {
                fuel.add(t.fuel());
            } else if (level.getBlockEntity(p) instanceof CargoBayBlockEntity bay) {
                cargo += bay.usedSlots();
            }
        }
        return new RocketAssembler.Snapshot(stack, fins, fuel, cargo);
    }

    /** Zustand als JSON für {@code LaunchStatePayload}. */
    public static JsonObject toJson(State s, int countdown) {
        JsonObject o = new JsonObject();
        o.addProperty("load", s.load().name().toLowerCase(java.util.Locale.ROOT));
        o.addProperty("countdown", countdown);
        JsonArray lines = new JsonArray();
        for (Line l : s.lines()) {
            JsonObject j = new JsonObject();
            j.addProperty("key", l.key());
            j.addProperty("ok", l.ok());
            if (l.problem() != null) {
                j.addProperty("error", l.problem().key());
                j.addProperty("a", l.problem().a());
                j.addProperty("b", l.problem().b());
            }
            lines.add(j);
        }
        o.add("lines", lines);
        if (s.rocket() != null) {
            RocketConfig r = s.rocket();
            JsonObject j = new JsonObject();
            j.addProperty("tier", r.tier());
            j.addProperty("mass", r.mass());
            j.addProperty("thrust", r.thrust());
            j.addProperty("ratio", s.thrustRatio());
            j.addProperty("capacity", r.capacity());
            JsonArray fuel = new JsonArray();
            for (int i = 0; i < r.fuel().length; i++) {
                JsonObject f = new JsonObject();
                f.addProperty("fluid", r.fuelIds().get(i).toString());
                f.addProperty("amount", r.fuel()[i]);
                fuel.add(f);
            }
            j.add("fuel", fuel);
            JsonArray parts = new JsonArray();
            r.stack().forEach(p -> parts.add(p.block().toString()));
            j.add("parts", parts);
            j.addProperty("fins", r.fins());
            o.add("rocket", j);
        }
        JsonArray dests = new JsonArray();
        for (Destination d : s.destinations()) {
            JsonObject j = new JsonObject();
            j.addProperty("dimension", d.dimension().toString());
            j.addProperty("out", d.outbound());
            j.addProperty("back", d.back());
            j.addProperty("reach", d.reach().name().toLowerCase(java.util.Locale.ROOT));
            j.addProperty("unlocked", d.unlocked());
            dests.add(j);
        }
        o.add("destinations", dests);
        JsonArray probes = new JsonArray();
        for (ProbeTarget t : s.probeTargets()) {
            JsonObject j = new JsonObject();
            j.addProperty("dimension", t.dimension().toString());
            j.addProperty("reached", t.reached());
            probes.add(j);
        }
        o.add("probe_targets", probes);
        return o;
    }
}
