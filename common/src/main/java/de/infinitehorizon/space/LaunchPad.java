package de.infinitehorizon.space;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Startrampe {@code launch_pad_t1} (4.3, machines.json → {@code launch_pad_t1_multiblock}): 5×5 {@code launch_pad} um
 * die Mitte, eine Säule {@code launch_tower} waagerecht neben dem Rand (nicht diagonal), mindestens so hoch wie die
 * gestapelten Raketenteile, und mindestens eine angrenzende {@code fuel_pump}. Die Mitte ist der Block unter dem
 * Triebwerk bzw. der Sonde.
 */
public final class LaunchPad {

    /** Was über der Mitte steht. */
    public enum Load { NONE, PROBE, ROCKET }

    /**
     * Ergebnis einer Prüfung.
     *
     * @param stack        Raketenteile über der Mitte (von unten), bei einer Sonde nur sie
     * @param towerHeight  Höhe der höchsten Turmsäule (0 = keiner)
     * @param problems     erste Fehler (Schlüssel {@code gui.infinite_horizon.launch.error.<key>})
     */
    public record Check(BlockPos center, Load load, List<BlockPos> stack, int towerHeight, List<BlockPos> pumps,
                        List<RocketAssembler.Problem> problems) {
        public boolean valid() {
            return problems.isEmpty();
        }
    }

    private LaunchPad() {
    }

    private static JsonObject rules() {
        return BalanceManager.get().entry("machines", "launch_pad_t1_multiblock").orElse(new JsonObject());
    }

    /** Kantenlänge der Plattform (5). */
    public static int size() {
        JsonObject p = rules().has("platform") ? rules().getAsJsonObject("platform") : new JsonObject();
        return p.has("size") ? p.getAsJsonArray("size").get(0).getAsInt() : 0;
    }

    /** Umkreis von {@code launch_control} um die Mitte (8). */
    public static int controlRadius() {
        return rules().has("control_radius") ? rules().get("control_radius").getAsInt() : 0;
    }

    /** Mindesthöhe des Turms für eine Sonde (1). */
    private static int probeTowerHeight() {
        JsonObject t = rules().has("tower") ? rules().getAsJsonObject("tower") : new JsonObject();
        return t.has("min_height_probe") ? t.get("min_height_probe").getAsInt() : 1;
    }

    private static boolean is(Level level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }

    /** Liegt um {@code center} die volle Fläche aus {@code launch_pad} auf gleicher Höhe? */
    public static boolean fullPlatform(Level level, BlockPos center) {
        int half = size() / 2;
        if (half <= 0) {
            return false;
        }
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (!is(level, center.offset(dx, 0, dz), ModBlocks.LAUNCH_PAD.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isRocketPart(Level level, BlockPos pos, RocketPartRegistry reg) {
        return reg.part(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())).isPresent();
    }

    /** Was über einer Mitte steht. */
    public static Load loadAt(Level level, BlockPos center) {
        BlockPos above = center.above();
        if (is(level, above, ModBlocks.PROBE.get())) {
            return Load.PROBE;
        }
        return isRocketPart(level, above, RocketPartRegistry.get()) ? Load.ROCKET : Load.NONE;
    }

    /** Prüft die Rampe mit dieser Mitte (4.3). */
    public static Check check(Level level, BlockPos center) {
        List<RocketAssembler.Problem> problems = new ArrayList<>();
        int half = size() / 2;
        int pads = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (is(level, center.offset(dx, 0, dz), ModBlocks.LAUNCH_PAD.get())) {
                    pads++;
                }
            }
        }
        int needPads = size() * size();
        if (pads < needPads) {
            problems.add(new RocketAssembler.Problem("platform", pads, needPads));
        }
        Load load = loadAt(level, center);
        List<BlockPos> stack = new ArrayList<>();
        if (load == Load.PROBE) {
            stack.add(center.above());
        } else if (load == Load.ROCKET) {
            RocketPartRegistry reg = RocketPartRegistry.get();
            BlockPos p = center.above();
            while (isRocketPart(level, p, reg) && level.isInsideBuildHeight(p.getY())) {
                stack.add(p);
                p = p.above();
            }
        }
        // Turm: eine der Positionen waagerecht neben dem Rand (nicht diagonal), unterster Block auf Plattformhöhe
        int tower = 0;
        List<BlockPos> towerBlocks = new ArrayList<>();
        for (BlockPos base : towerPositions(center, half)) {
            int h = 0;
            List<BlockPos> column = new ArrayList<>();
            BlockPos p = base;
            while (is(level, p, ModBlocks.LAUNCH_TOWER.get())) {
                column.add(p);
                h++;
                p = p.above();
            }
            if (h > tower) {
                tower = h;
            }
            towerBlocks.addAll(column);
        }
        int needTower = load == Load.ROCKET ? Math.max(probeTowerHeight(), stack.size()) : probeTowerHeight();
        if (tower < needTower) {
            problems.add(new RocketAssembler.Problem("tower", tower, needTower));
        }
        // Pumpe: grenzt mit einer Fläche an Plattform oder Turm
        Set<BlockPos> pumps = new LinkedHashSet<>();
        Set<BlockPos> structure = new HashSet<>(towerBlocks);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                structure.add(center.offset(dx, 0, dz));
            }
        }
        for (BlockPos s : structure) {
            for (Direction d : Direction.values()) {
                BlockPos n = s.relative(d);
                if (!structure.contains(n) && is(level, n, ModBlocks.FUEL_PUMP.get())) {
                    pumps.add(n.immutable());
                }
            }
        }
        if (pumps.isEmpty()) {
            problems.add(new RocketAssembler.Problem("pump", 0, 1));
        }
        return new Check(center.immutable(), load, List.copyOf(stack), tower, List.copyOf(pumps), List.copyOf(problems));
    }

    /** Die 4 × (Kantenlänge) Positionen waagerecht neben dem Rand der Fläche. */
    static List<BlockPos> towerPositions(BlockPos center, int half) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = -half; i <= half; i++) {
            out.add(center.offset(i, 0, -half - 1));
            out.add(center.offset(i, 0, half + 1));
            out.add(center.offset(-half - 1, 0, i));
            out.add(center.offset(half + 1, 0, i));
        }
        return out;
    }

    /**
     * Rampe für {@code launch_control} an {@code control}: Mitten mit voller Fläche im Umkreis {@code control_radius}
     * (euklidisch); bevorzugt eine mit Last, dann die gültige, dann die nächste.
     */
    public static @Nullable Check nearest(Level level, BlockPos control) {
        int r = controlRadius();
        List<BlockPos> centers = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(control.offset(-r, -r, -r), control.offset(r, r, r))) {
            if (p.distSqr(control) <= (double) r * r && is(level, p, ModBlocks.LAUNCH_PAD.get()) && fullPlatform(level, p)) {
                centers.add(p.immutable());
            }
        }
        if (centers.isEmpty()) {
            return null;
        }
        return centers.stream().map(c -> check(level, c))
                .min(Comparator.comparing((Check c) -> c.load() == Load.NONE)
                        .thenComparing(c -> !c.valid())
                        .thenComparingDouble(c -> c.center().distSqr(control)))
                .orElse(null);
    }

    /**
     * Rampe einer Pumpe (12.4): über angrenzende {@code launch_pad}/{@code launch_tower} die nächste Mitte mit Last
     * suchen, deren Prüfung diese Pumpe enthält.
     */
    public static @Nullable Check forPump(Level level, BlockPos pump) {
        int half = size() / 2;
        int reach = half * 2 + 2;
        Check best = null;
        for (BlockPos p : BlockPos.betweenClosed(pump.offset(-reach, -1, -reach), pump.offset(reach, 1, reach))) {
            if (!is(level, p, ModBlocks.LAUNCH_PAD.get()) || loadAt(level, p) == Load.NONE || !fullPlatform(level, p)) {
                continue;
            }
            Check c = check(level, p.immutable());
            if (c.pumps().contains(pump) && (best == null || c.center().distSqr(pump) < best.center().distSqr(pump))) {
                best = c;
            }
        }
        // Pumpe neben einem hohen Turm: Mitte unterhalb des Turms suchen
        if (best == null) {
            for (Direction d : Direction.values()) {
                BlockPos n = pump.relative(d);
                if (is(level, n, ModBlocks.LAUNCH_TOWER.get())) {
                    BlockPos base = n;
                    while (is(level, base.below(), ModBlocks.LAUNCH_TOWER.get())) {
                        base = base.below();
                    }
                    Check c = forPumpNear(level, pump, base, reach);
                    if (c != null) {
                        return c;
                    }
                }
            }
        }
        return best;
    }

    private static @Nullable Check forPumpNear(Level level, BlockPos pump, BlockPos base, int reach) {
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-reach, 0, -reach), base.offset(reach, 0, reach))) {
            if (is(level, p, ModBlocks.LAUNCH_PAD.get()) && loadAt(level, p) != Load.NONE && fullPlatform(level, p)) {
                Check c = check(level, p.immutable());
                if (c.pumps().contains(pump)) {
                    return c;
                }
            }
        }
        return null;
    }

    /** Block-IDs eines Stapels (für {@link RocketAssembler}). */
    public static List<Identifier> ids(Level level, List<BlockPos> stack) {
        List<Identifier> out = new ArrayList<>();
        stack.forEach(p -> out.add(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock())));
        return out;
    }
}
