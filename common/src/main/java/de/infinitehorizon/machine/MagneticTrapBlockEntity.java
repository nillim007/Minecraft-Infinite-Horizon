package de.infinitehorizon.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code magnetic_trap} (6.1): füllt im Jupiter-Orbit eine leere {@code antimatter_cell}. Ein- und Ausgabe stehen in
 * machines.json unter {@code process.input}/{@code process.output} (Item, Menge, ganzzahlige Komponenten; eine fehlende
 * Komponente zählt als 0). Dauer und Strom aus machines.json, Ortsbindung über {@code location}.
 */
public class MagneticTrapBlockEntity extends ProcessingMachineBlockEntity {

    public MagneticTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    private JsonObject process() {
        JsonObject m = BalanceManager.get().entry("machines", machineId()).orElse(new JsonObject());
        return m.has("process") ? m.getAsJsonObject("process") : new JsonObject();
    }

    /** Passt {@code stack} zur Angabe (Item und ganzzahlige Komponenten)? */
    static boolean matches(ItemStack stack, JsonObject spec) {
        if (stack.isEmpty() || !stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(spec.get("id").getAsString())))
                || stack.getCount() < spec.get("count").getAsInt()) {
            return false;
        }
        if (spec.has("components")) {
            for (Map.Entry<String, JsonElement> c : spec.getAsJsonObject("components").entrySet()) {
                Object v = stack.get(type(c.getKey()));
                int have = v instanceof Integer i ? i : 0;
                if (have != c.getValue().getAsInt()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Stapel nach Angabe (Item, Menge, ganzzahlige Komponenten). */
    @SuppressWarnings("unchecked")
    static ItemStack build(JsonObject spec) {
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(spec.get("id").getAsString())),
                spec.get("count").getAsInt());
        if (spec.has("components")) {
            for (Map.Entry<String, JsonElement> c : spec.getAsJsonObject("components").entrySet()) {
                s.set((DataComponentType<Integer>) type(c.getKey()), c.getValue().getAsInt());
            }
        }
        return s;
    }

    private static DataComponentType<?> type(String id) {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(id));
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        JsonObject p = process();
        return p.has("input") && stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(p.getAsJsonObject("input").get("id").getAsString())));
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        JsonObject p = process();
        if (!p.has("input") || !p.has("output") || items.isEmpty() || !matches(items.get(0), p.getAsJsonObject("input"))) {
            return Optional.empty();
        }
        return Optional.of(new Plan(new int[]{0}, new int[]{p.getAsJsonObject("input").get("count").getAsInt()}, List.of(),
                Optional.empty(), Optional.empty(), IhFluidStack.EMPTY, List.of(), List.of(build(p.getAsJsonObject("output")))));
    }
}
