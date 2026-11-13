package de.infinitehorizon.space;

import com.mojang.serialization.Codec;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.data.RocketPartRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Raketentank als Block (12.4): Inhalt je Treibstoffkomponente in mB (Reihenfolge wie {@code fuel} der Stufe in
 * rocket_parts.json), Fassungsvermögen je Komponente aus {@code fuel_capacity}. Gefüllt wird er nur über die
 * {@code fuel_pump}; beim Abbauen geht der Inhalt verloren (SPEC-GAP #72).
 */
public class RocketTankBlockEntity extends IhBlockEntity {

    /** Zahl der Treibstoffkomponenten (12.2: Brennstoff und Sauerstoff). */
    public static final int COMPONENTS = 2;

    private final long[] fuel = new long[COMPONENTS];

    public RocketTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Fassungsvermögen je Komponente. */
    public long capacity() {
        return RocketPartRegistry.get().part(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()))
                .map(RocketPartRegistry.Part::fuelCapacity).orElse(0);
    }

    public long fuel(int component) {
        return component >= 0 && component < COMPONENTS ? fuel[component] : 0;
    }

    public long[] fuel() {
        return fuel.clone();
    }

    /** Füllt Komponente {@code component} um bis zu {@code amount} mB; gibt die angenommene Menge zurück. */
    public long fill(int component, long amount) {
        if (component < 0 || component >= COMPONENTS || amount <= 0) {
            return 0;
        }
        long accepted = Math.min(amount, capacity() - fuel[component]);
        if (accepted > 0) {
            fuel[component] += accepted;
            setChanged();
        }
        return Math.max(0, accepted);
    }

    /** Für Tests und Befehle: Inhalt direkt setzen (begrenzt auf das Fassungsvermögen). */
    public void setFuel(int component, long amount) {
        if (component >= 0 && component < COMPONENTS) {
            fuel[component] = Math.max(0, Math.min(amount, capacity()));
            setChanged();
        }
    }

    public boolean isFull() {
        long cap = capacity();
        for (long f : fuel) {
            if (f < cap) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<Long> list = new ArrayList<>();
        for (long f : fuel) {
            list.add(f);
        }
        output.store("fuel", Codec.LONG.listOf(), list);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<Long> list = input.read("fuel", Codec.LONG.listOf()).orElse(List.of());
        for (int i = 0; i < COMPONENTS; i++) {
            fuel[i] = i < list.size() ? list.get(i) : 0;
        }
    }
}
