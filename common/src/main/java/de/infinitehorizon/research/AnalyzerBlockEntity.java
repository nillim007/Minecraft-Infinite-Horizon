package de.infinitehorizon.research;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Analysator (6.1, 11.1, SPEC-GAP #69): verbraucht je Vorgang 1 Probe und schreibt ihre FP am Ende der Agentur
 * {@code OwnershipResolver.current(owner)} gut. Dauer und Verbrauch aus machines.json, Upgrades wie bei allen Maschinen.
 * Ohne Besitzer steht er auf {@code error} (Tooltip {@code status.no_owner}) und macht keinen Fortschritt. Der
 * Eingangsslot nimmt nur Proben an, die FP geben.
 */
public class AnalyzerBlockEntity extends ProcessingMachineBlockEntity {

    private boolean noOwner;

    public AnalyzerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, List::of);
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        SampleData data = stack.get(ModDataComponents.SAMPLE.get());
        return data != null && FpLedger.givesFp(data, PlanetRegistry.get(), FpLedger.rules());
    }

    @Override
    protected Optional<Plan> findPlan(ServerLevel level) {
        ItemStack in = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        if (in.isEmpty() || !acceptsInput(0, in)) {
            return Optional.empty();
        }
        // Dauer und Verbrauch aus machines.json (keine Rezept-Überschreibung)
        return Optional.of(new Plan(new int[]{0}, new int[]{1}, List.of(), Optional.empty(), Optional.empty()));
    }

    @Override
    protected void process(ServerLevel level) {
        noOwner = owner() == null && !items.isEmpty() && !items.get(0).isEmpty();
        if (noOwner) {
            setStatus(MachineStatus.ERROR);
            return;
        }
        super.process(level);
    }

    @Override
    protected void beforeConsume(Plan plan, ServerLevel level) {
        SampleData data = items.get(0).get(ModDataComponents.SAMPLE.get());
        UUID owner = owner();
        if (data == null || owner == null) {
            return;
        }
        FpLedger.Rules rules = FpLedger.rules();
        OptionalInt ring = FpLedger.ringOf(data.dimension(), PlanetRegistry.get(), rules);
        AgencyData agencies = AgencyData.get(level.getServer());
        Agency agency = agencies.byId(owner).orElse(null);
        if (ring.isEmpty() || agency == null) {
            return;
        }
        int credited = FpLedger.creditSample(agency.progress(), FpLedger.sampleKey(data.block(), data.dimension(), rules),
                ring.getAsInt(), rules);
        if (credited > 0) {
            agencies.setDirty();
            ResearchService.syncAgency(level.getServer(), agency);
        }
    }

    @Override
    public int statusHint() {
        return noOwner ? HINT_NO_OWNER : super.statusHint();
    }
}
