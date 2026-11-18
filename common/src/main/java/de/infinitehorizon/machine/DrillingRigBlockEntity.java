package de.infinitehorizon.machine;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * {@code drilling_rig} (20.2): zählt beim Aufstellen alle Erzblöcke ({@code #c:ores} und Planeten-Erze) im Chunk unter
 * sich und erzeugt je Vorgang 1 Erz-Item, zufällig gewichtet nach diesen Anzahlen. Die Welt bleibt unverändert. Ohne Erze:
 * Status {@code error}. Werte aus logistics.json und machines.json.
 */
public class DrillingRigBlockEntity extends ProcessingMachineBlockEntity {

    public static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    public static final TagKey<Block> PLANET_ORES = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("planet_ores"));
    private static final Codec<Map<String, Integer>> COUNTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    /** Erzblöcke unter dem Turm nach Block-ID (sortiert, damit die Auswahl reproduzierbar ist). */
    private final TreeMap<String, Integer> counts = new TreeMap<>();
    private boolean scanned;
    private @Nullable Item next;

    public DrillingRigBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private static JsonObject rules() {
        return BalanceManager.get().entry("logistics", InfiniteHorizon.id("drilling_rig").toString()).orElse(new JsonObject());
    }

    public Map<String, Integer> counts() {
        return java.util.Collections.unmodifiableMap(counts);
    }

    /** Ist der Block ein Erz für den Bohrturm? */
    public static boolean isOre(BlockState s) {
        JsonObject r = rules();
        boolean planet = !r.has("includes_planet_ores") || r.get("includes_planet_ores").getAsBoolean();
        return s.is(C_ORES) || planet && s.is(PLANET_ORES);
    }

    /** Zählt die Erze im Chunk unter dem Turm (vom Boden der Welt bis unter den Turm). */
    public void scan(ServerLevel level) {
        counts.clear();
        ChunkPos c = ChunkPos.containing(getBlockPos());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
            for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
                for (int y = level.getMinY(); y < getBlockPos().getY(); y++) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (isOre(s) && s.getBlock().asItem() != Items.AIR) {
                        counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                    }
                }
            }
        }
        scanned = true;
        next = null;
        setChanged();
    }

    /** Erz nach Anzahl gewichtet ziehen. */
    private @Nullable Item pick(ServerLevel level) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return null;
        }
        int r = level.getRandom().nextInt(total);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return BuiltInRegistries.BLOCK.getValue(Identifier.parse(e.getKey())).asItem();
            }
        }
        return null;
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        if (next == null) {
            next = pick(level);
        }
        if (next == null) {
            return Optional.empty();
        }
        JsonObject m = BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
        int per = m.has("process") ? m.getAsJsonObject("process").get("output_per_cycle").getAsInt() : 1;
        return Optional.of(new Plan(new int[0], new int[0], List.of(), Optional.empty(), Optional.empty(), IhFluidStack.EMPTY,
                List.of(), List.of(new ItemStack(next, per))));
    }

    @Override
    protected void beforeConsume(Plan plan, ServerLevel level) {
        next = null; // nächster Vorgang zieht neu
    }

    @Override
    protected void process(ServerLevel level) {
        if (!scanned) {
            scan(level);
        }
        super.process(level);
        if (counts.isEmpty()) {
            setStatus(MachineStatus.ERROR); // 20.2: ohne Erze error
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("scanned", scanned);
        output.store("ores", COUNTS_CODEC, Map.copyOf(counts));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        scanned = input.getBooleanOr("scanned", false);
        counts.clear();
        input.read("ores", COUNTS_CODEC).ifPresent(counts::putAll);
    }
}
