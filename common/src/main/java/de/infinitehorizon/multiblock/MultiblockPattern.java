package de.infinitehorizon.multiblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Muster eines Multiblocks (4.3), gelesen aus machines.json (z. B. Eintrag {@code refinery_multiblock}):
 * Maße B × T × H, Form {@code solid} oder {@code hollow_shell} (Innenraum Luft), Lage des Controllers
 * ({@code side_face_bottom_center} oder {@code side_face_center}), Gehäuseblock und erlaubte Ersatzblöcke (Ports).
 *
 * <p>Der Controller steht in der Vorderseite ({@code facing}); das Muster reicht von ihm nach links und rechts, gegen
 * die Blickrichtung nach hinten und nach oben (bzw. ober- und unterhalb). Addons legen eigene Multiblöcke nur über
 * Daten an: ein Eintrag mit diesen Feldern und beim Controller {@code "multiblock": "<eintrag>"}.
 */
public record MultiblockPattern(String key, Identifier controller, int width, int depth, int height, boolean hollow,
                                boolean bottom, Identifier casing, List<Identifier> replacements) {

    public static final Identifier AIR = Identifier.withDefaultNamespace("air");

    /** Musterplatz: Gehäuse (oder Ersatz) bzw. Luft im Innenraum. */
    public enum SlotKind { PART, AIR }

    /** Ein Musterplatz relativ zum Controller: rechts (quer zur Blickrichtung), oben, hinten. */
    public record Slot(int right, int up, int back, SlotKind kind) {
    }

    /** Ein falscher Platz: Weltposition und der erwartete Block (Gehäuse bzw. Luft). */
    public record Missing(BlockPos pos, Identifier expected) {
    }

    /** Muster des Controllers {@code machineId} (Feld {@code multiblock} seines machines.json-Eintrags). */
    public static Optional<MultiblockPattern> forMachine(BalanceManager balance, String machineId) {
        // Controller als Maschine (Raffinerie) oder als Generator (Fusionsreaktor); das Muster steht in machines.json
        return balance.entry("machines", machineId).or(() -> balance.entry("generators", machineId))
                .filter(e -> e.has("multiblock") && e.get("multiblock").isJsonPrimitive())
                .map(e -> e.get("multiblock").getAsString())
                .flatMap(key -> balance.entry("machines", key).map(o -> fromJson(key, o)));
    }

    public static MultiblockPattern fromJson(String key, JsonObject o) {
        JsonObject size = o.getAsJsonObject("size");
        List<Identifier> repl = new ArrayList<>();
        if (o.has("casing_replaceable_by") && o.get("casing_replaceable_by").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("casing_replaceable_by")) {
                repl.add(Identifier.parse(e.getAsString()));
            }
        }
        String pos = o.has("controller_position") ? o.get("controller_position").getAsString() : "side_face_bottom_center";
        return new MultiblockPattern(key, Identifier.parse(o.get("controller").getAsString()),
                size.get("width").getAsInt(), size.get("depth").getAsInt(), size.get("height").getAsInt(),
                o.has("shape") && o.get("shape").getAsString().startsWith("hollow"),
                pos.endsWith("bottom_center"), Identifier.parse(o.get("casing").getAsString()), List.copyOf(repl));
    }

    /** Alle Musterplätze ohne den Controller, in Prüfreihenfolge (Schicht, Tiefe, quer). Reine Rechnung. */
    public List<Slot> slots() {
        List<Slot> out = new ArrayList<>();
        int left = -(width - 1) / 2;
        int right = left + width - 1;
        int down = bottom ? 0 : -(height - 1) / 2;
        int top = down + height - 1;
        for (int up = down; up <= top; up++) {
            for (int back = 0; back < depth; back++) {
                for (int r = left; r <= right; r++) {
                    if (r == 0 && up == 0 && back == 0) {
                        continue;
                    }
                    boolean shell = r == left || r == right || up == down || up == top || back == 0 || back == depth - 1;
                    out.add(new Slot(r, up, back, !hollow || shell ? SlotKind.PART : SlotKind.AIR));
                }
            }
        }
        return out;
    }

    /** Weltposition eines Musterplatzes für einen Controller mit Vorderseite {@code facing}. */
    public static BlockPos toWorld(BlockPos controller, Direction facing, Slot s) {
        return controller.relative(facing.getClockWise(), s.right()).relative(Direction.UP, s.up())
                .relative(facing.getOpposite(), s.back());
    }

    /** Darf dieser Block an einem Gehäuseplatz stehen (Gehäuse oder Port)? */
    public boolean allows(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return casing.equals(id) || replacements.contains(id);
    }

    /**
     * Vergleicht das Muster mit der Welt. {@code all} = false liefert höchstens den ersten Fehler (für Statuslampe und
     * Tooltip), true alle (Geisterprojektion). Leer = vollständig.
     */
    public List<Missing> check(BlockGetter level, BlockPos controllerPos, Direction facing, boolean all) {
        List<Missing> out = new ArrayList<>();
        for (Slot s : slots()) {
            BlockPos p = toWorld(controllerPos, facing, s);
            BlockState state = level.getBlockState(p);
            boolean ok = s.kind() == SlotKind.AIR ? state.isAir() : allows(state.getBlock());
            if (!ok) {
                out.add(new Missing(p, s.kind() == SlotKind.AIR ? AIR : casing));
                if (!all) {
                    break;
                }
            }
        }
        return out;
    }

    /** Positionen aller Gehäuseplätze (für die Verknüpfung der Ports). */
    public List<BlockPos> partPositions(BlockPos controllerPos, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (Slot s : slots()) {
            if (s.kind() == SlotKind.PART) {
                out.add(toWorld(controllerPos, facing, s));
            }
        }
        return out;
    }
}
