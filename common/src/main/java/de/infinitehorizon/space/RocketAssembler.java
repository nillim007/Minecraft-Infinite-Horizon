package de.infinitehorizon.space;

import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.data.RocketPartRegistry.Part;
import de.infinitehorizon.data.RocketPartRegistry.Role;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Raketenbau (12.4) als reine Funktion: Schnappschuss der Blöcke über der Plattformmitte → {@link RocketConfig} oder
 * eine Fehlerliste für die Checkliste (z. B. „3 von 4 Flossen“). Regeln aus {@code rocket_parts.json}.
 */
public final class RocketAssembler {

    /**
     * Blöcke über der Plattformmitte.
     *
     * @param stack     Raketenteile von unten nach oben (lückenlos ab dem Block über der Mitte)
     * @param fins      Blöcke an N, O, S, W neben dem untersten Teil ({@code null}, wenn dort kein Raketenteil steht)
     * @param tankFuel  je Tank in {@code stack} der Inhalt je Komponente (mB)
     * @param cargoUsed belegte Frachtslots aller Frachträume
     */
    public record Snapshot(List<Identifier> stack, List<@Nullable Identifier> fins, List<long[]> tankFuel, int cargoUsed) {
    }

    /** Ein Fehler mit Übersetzungsschlüssel {@code gui.infinite_horizon.launch.error.<key>} und zwei Zahlen. */
    public record Problem(String key, int a, int b) {
    }

    public record Result(@Nullable RocketConfig config, List<Problem> problems) {
        public boolean valid() {
            return config != null && problems.isEmpty();
        }
    }

    private RocketAssembler() {
    }

    public static Result assemble(Snapshot s, RocketPartRegistry reg) {
        List<Problem> problems = new ArrayList<>();
        List<Part> parts = new ArrayList<>();
        for (Identifier id : s.stack()) {
            reg.part(id).ifPresent(parts::add);
        }
        if (parts.isEmpty() || parts.getFirst().role() != Role.ENGINE) {
            problems.add(new Problem("no_engine", 0, 0));
            return new Result(null, problems);
        }
        int tier = parts.getFirst().tier();
        RocketPartRegistry.Rule rule = reg.rule(tier).orElse(null);
        if (rule == null) {
            problems.add(new Problem("unknown_tier", tier, 0));
            return new Result(null, problems);
        }
        // Stufen nicht mischen (außer Frachtraum über usable_in_tiers)
        for (Part p : parts) {
            if (!p.usableInTiers().contains(tier)) {
                problems.add(new Problem("mixed_tier", p.tier(), tier));
                break;
            }
        }
        if (parts.getLast().role() != Role.CAPSULE) {
            problems.add(new Problem("no_capsule", 0, 0));
        }
        // Reihenfolge laut stack_order (nicht abnehmend)
        int last = -1;
        for (Part p : parts) {
            int idx = rule.stackOrder().indexOf(p.role());
            if (idx < last) {
                problems.add(new Problem("order", 0, 0));
                break;
            }
            last = Math.max(last, idx);
        }
        // Anzahlen je Rolle
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        parts.forEach(p -> counts.merge(p.role(), 1, Integer::sum));
        for (Role role : Role.values()) {
            if (role == Role.FIN) {
                continue;
            }
            int[] range = rule.range(role);
            int n = counts.getOrDefault(role, 0);
            if (n < range[0] || n > range[1]) {
                problems.add(new Problem("count." + role.key(), n, n < range[0] ? range[0] : range[1]));
            }
        }
        // Flossen: genau an N, O, S, W neben dem Triebwerk
        int fins = 0;
        for (Identifier f : s.fins()) {
            if (f != null && reg.part(f).filter(p -> p.role() == Role.FIN && p.usableInTiers().contains(tier)).isPresent()) {
                fins++;
            }
        }
        if (fins != rule.fins()) {
            problems.add(new Problem("fins", fins, rule.fins()));
        }

        // Werte
        double dry = 0;
        double fuelMass = 0;
        double cargoMass = 0;
        long capacity = 0;
        int thrust = 0;
        int seats = 0;
        long[] fuel = new long[rule.fuel().size()];
        int tank = 0;
        for (Part p : parts) {
            dry += p.mass();
            thrust += p.thrust();
            seats += p.seats();
            capacity += p.fuelCapacity();
            if (p.fuelCapacity() > 0) {
                long[] content = tank < s.tankFuel().size() ? s.tankFuel().get(tank) : new long[fuel.length];
                long sum = 0;
                for (int c = 0; c < fuel.length && c < content.length; c++) {
                    fuel[c] += content[c];
                    sum += content[c];
                }
                fuelMass += p.fuelMassFull() * sum / ((double) p.fuelCapacity() * Math.max(1, fuel.length));
                tank++;
            }
        }
        double perSlot = parts.stream().filter(p -> p.role() == Role.CARGO).mapToDouble(Part::massPerSlot).max().orElse(0);
        cargoMass = perSlot * s.cargoUsed();
        for (Identifier f : s.fins()) {
            if (f != null) {
                dry += reg.part(f).filter(p -> p.role() == Role.FIN).map(Part::mass).orElse(0.0);
            }
        }
        RocketConfig config = new RocketConfig(tier, List.copyOf(parts), fins, dry, fuelMass, cargoMass,
                thrust > 0 ? thrust : rule.thrust(), rule.fuel(), fuel, capacity, seats);
        return new Result(config, List.copyOf(problems));
    }
}
