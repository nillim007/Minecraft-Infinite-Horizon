package de.infinitehorizon.machine;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.energy.IhEnergyStorage;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.BalanceManager.MachineStats;
import de.infinitehorizon.data.BalanceManager.TankSpec;
import de.infinitehorizon.recipe.MachineRecipeInput;
import de.infinitehorizon.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Verarbeitungsmaschine (Zerkleinerer, Presse, Raffinerie, Elektrolyseur, Montagetisch …). Slots und Tanks laut
 * machines.json ({@code item_slots}, {@code fluid_tanks}, {@code container_slots}); Rezeptarten als
 * {@link ProcessingRecipe.Kind}. Dauer und Verbrauch: Rezept, sonst Maschine (25.4), dann Upgrades (4.2). Der
 * Fortschritt ist ein Anteil (0–1), damit er beim Ein- und Ausbau von Upgrades erhalten bleibt.
 *
 * <p>Tanks (4.1): Ein Eingangstank nimmt nur Fluide an, die in einem Rezept dieser Maschine als Eingabe stehen (oder die
 * Liste {@code fluids} aus den Daten); Ausgangstanks nehmen von außen nichts an und geben über die Ausgangsseite ab.
 *
 * <p>Addon-Maschine: {@code (pos, state) -> new ProcessingMachineBlockEntity(TYP, pos, state, () -> List.of(KIND))},
 * ein {@link MachineBlock} und ein Eintrag in {@code data/<ns>/infinite_horizon/balance/machines.json}.
 * Für andere Rezeptquellen {@link #findPlan(ServerLevel)} überschreiben.
 */
public class ProcessingMachineBlockEntity extends MachineBlockEntity {

    /**
     * Ein konkreter Vorgang: welche Slots wie viel verbrauchen, was herauskommt, Rezept-Überschreibungen, dazu die
     * Fluid-Zutat (oder {@link IhFluidStack#EMPTY}) und die Fluid-Ergebnisse.
     */
    public record Plan(int[] consumeSlots, int[] consumeCounts, List<ProcessingRecipe.ChanceResult> results,
                       Optional<Integer> ticks, Optional<Long> energyPerTick, IhFluidStack fluidIn,
                       List<IhFluidStack> fluidResults) {

        public Plan(int[] consumeSlots, int[] consumeCounts, List<ProcessingRecipe.ChanceResult> results,
                    Optional<Integer> ticks, Optional<Long> energyPerTick) {
            this(consumeSlots, consumeCounts, results, ticks, energyPerTick, IhFluidStack.EMPTY, List.of());
        }
    }

    private static final double DONE = 1.0 - 1e-9;
    /** Eingabe-Fluide je Rezeptart, zwischengespeichert je Rezeptmanager (neu nach /reload). */
    private static final Map<RecipeManager, Map<ProcessingRecipe.Kind, Set<Identifier>>> INPUT_FLUIDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final Supplier<List<ProcessingRecipe.Kind>> kinds;
    private final List<IhFluidTank> inTanks = new ArrayList<>();
    private final List<IhFluidTank> outTanks = new ArrayList<>();
    private double progress;
    private int currentTicks;
    /** Zuletzt passendes Rezept; nach /reload ein neues Objekt aus dem neuen Rezeptmanager. */
    private @Nullable ProcessingRecipe lastRecipe;
    private @Nullable Object lastRecipeSource;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        Supplier<List<ProcessingRecipe.Kind>> kinds) {
        super(type, pos, state);
        this.kinds = kinds;
    }

    public MachineStats stats() {
        return BalanceManager.get().machine(machineId());
    }

    public List<ProcessingRecipe.Kind> kinds() {
        return kinds.get();
    }

    /** Fortschritt des laufenden Vorgangs als Anteil 0–1. */
    public double progress() {
        return progress;
    }

    @Override
    public MachineLayout layout() {
        if (BalanceManager.get().isEmpty()) {
            return MachineLayout.EMPTY;
        }
        MachineStats s = stats();
        int[] containers = BalanceManager.get().containerSlots("machines", machineId());
        return new MachineLayout(s.inputSlots(), s.moldSlots(), s.outputSlots(), s.upgradeSlots(), 0,
                containers[0], containers[1]);
    }

    @Override
    protected void configureEnergy(IhEnergyStorage storage) {
        MachineStats s = stats();
        storage.reconfigure(s.buffer(), s.buffer(), 0);
        configureTanks(BalanceManager.get().tanks("machines", machineId()));
    }

    /** Legt die Tanks laut Daten an (Inhalt bleibt erhalten, solange die Zahl gleich bleibt). */
    protected void configureTanks(List<TankSpec> specs) {
        int in = 0;
        int out = 0;
        for (TankSpec spec : specs) {
            List<IhFluidTank> list = spec.output() ? outTanks : inTanks;
            int idx = spec.output() ? out++ : in++;
            if (list.size() <= idx) {
                list.add(new IhFluidTank(0, id -> false, this::setChanged));
            }
            List<String> allowed = spec.fluids();
            list.get(idx).reconfigure(spec.capacity(), spec.output() ? id -> false
                    : allowed != null ? id -> allowed.contains(id.toString()) : this::acceptsInputFluid);
        }
        while (inTanks.size() > in) {
            inTanks.removeLast();
        }
        while (outTanks.size() > out) {
            outTanks.removeLast();
        }
    }

    /** Steht dieses Fluid in mindestens einem Rezept dieser Maschine als Eingabe (4.1)? */
    public boolean acceptsInputFluid(Identifier fluid) {
        if (!(level instanceof ServerLevel sl) || !(sl.recipeAccess() instanceof RecipeManager rm)) {
            return false;
        }
        Map<ProcessingRecipe.Kind, Set<Identifier>> byKind = INPUT_FLUIDS.computeIfAbsent(rm, k -> {
            Map<ProcessingRecipe.Kind, Set<Identifier>> m = new java.util.HashMap<>();
            for (RecipeHolder<?> h : rm.getRecipes()) {
                if (h.value() instanceof ProcessingRecipe r && r.fluid().isPresent()) {
                    m.computeIfAbsent(r.kind(), x -> new HashSet<>()).add(r.fluid().get().fluid());
                }
            }
            return m;
        });
        for (ProcessingRecipe.Kind kind : kinds.get()) {
            if (byKind.getOrDefault(kind, Set.of()).contains(fluid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<IhFluidTank> inputTanks() {
        return inTanks;
    }

    @Override
    protected List<IhFluidTank> outputTanks() {
        return outTanks;
    }

    @Override
    protected long syncValue(int index) {
        return switch (index) {
            case 2 -> Math.round(progress * currentTicks);
            case 3 -> currentTicks;
            default -> super.syncValue(index);
        };
    }

    @Override
    public boolean acceptsEnergyFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    public boolean providesEnergyTo(@Nullable Direction side) {
        return false;
    }

    /** Aktuelle Eingabe: alle Eingabeslots, der Formslot und der Inhalt der Eingangstanks. */
    public MachineRecipeInput recipeInput() {
        MachineLayout l = layout();
        List<ItemStack> in = new ArrayList<>(l.inputs());
        for (int i = 0; i < l.inputs(); i++) {
            in.add(items.get(i));
        }
        ItemStack mold = l.molds() > 0 ? items.get(l.moldStart()) : ItemStack.EMPTY;
        List<IhFluidStack> fluids = new ArrayList<>();
        for (IhFluidTank t : inTanks) {
            fluids.add(t.stack());
        }
        return new MachineRecipeInput(in, mold, fluids);
    }

    /** Sucht den nächsten Vorgang über alle Rezeptarten der Maschine. */
    protected Optional<Plan> findPlan(ServerLevel level) {
        MachineRecipeInput input = recipeInput();
        if (input.isEmpty()) {
            return Optional.empty();
        }
        if (lastRecipeSource != level.recipeAccess()) {
            lastRecipe = null;
            lastRecipeSource = level.recipeAccess();
        }
        // zuerst das zuletzt passende Rezept (spart die Suche über alle Rezepte, Leistungstest 27.2)
        if (lastRecipe != null) {
            ProcessingRecipe.Match m = lastRecipe.match(input);
            if (m != null) {
                return Optional.of(planOf(lastRecipe, m));
            }
        }
        for (ProcessingRecipe.Kind kind : kinds.get()) {
            Optional<RecipeHolder<ProcessingRecipe>> found = level.recipeAccess().getRecipeFor(kind.type(), input, level);
            if (found.isPresent()) {
                ProcessingRecipe r = found.get().value();
                ProcessingRecipe.Match m = r.match(input);
                if (m != null) {
                    lastRecipe = r;
                    return Optional.of(planOf(r, m));
                }
            }
        }
        return Optional.empty();
    }

    private static Plan planOf(ProcessingRecipe r, ProcessingRecipe.Match m) {
        return new Plan(m.slots(), m.counts(), r.results(), r.ticks(), r.energyPerTick(),
                r.fluid().orElse(IhFluidStack.EMPTY), r.fluidResults());
    }

    /** Wirksame Dauer und Verbrauch für einen Vorgang (reine Rechnung, 4.2). */
    public ProcessingMath.Effective effective(Plan plan) {
        MachineStats s = stats();
        MachineStats withRecipe = s.withProcess(plan.energyPerTick().orElse(s.energyPerTick()), plan.ticks().orElse(s.ticks()));
        return ProcessingMath.effective(withRecipe, installedUpgrades());
    }

    /** Wirksame Dauer und Verbrauch (mit eingebauten Upgrades), wenn {@code input} im ersten Eingabeslot läge; sonst null. */
    public ProcessingMath.@Nullable Effective effectiveFor(ServerLevel level, ItemStack input) {
        ensureLayout();
        ItemStack old = items.get(0);
        items.set(0, input);
        try {
            return findPlan(level).map(this::effective).orElse(null);
        } finally {
            items.set(0, old);
        }
    }

    /** Dauer in Ticks, wenn {@code input} im ersten Eingabeslot läge; -1 ohne passendes Rezept. */
    public int durationFor(ServerLevel level, ItemStack input) {
        ensureLayout();
        ItemStack old = items.get(0);
        items.set(0, input);
        try {
            return findPlan(level).map(p -> effective(p).ticks()).orElse(-1);
        } finally {
            items.set(0, old);
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!ensureLayout()) {
            return;
        }
        if (!inTanks.isEmpty() || !outTanks.isEmpty()) {
            handleContainers();
            pushFluids(level);
        }
        process(level);
    }

    /** Ein Verarbeitungsschritt (Rezept suchen, Strom verbrauchen, Fortschritt, Ergebnis ablegen). */
    protected void process(ServerLevel level) {
        Optional<Plan> found = findPlan(level);
        if (found.isEmpty()) {
            progress = 0;
            currentTicks = 0;
            setStatus(MachineStatus.IDLE);
            return;
        }
        Plan plan = found.get();
        int[] fluidTargets = fluidTargets(plan.fluidResults());
        if (!outputsFit(plan.results()) || fluidTargets == null) {
            setStatus(MachineStatus.ERROR);
            return;
        }
        ProcessingMath.Effective eff = effective(plan);
        currentTicks = eff.ticks();
        if (!energy.useInternal(eff.energyPerTick())) {
            setStatus(MachineStatus.IDLE);
            return;
        }
        progress += 1.0 / eff.ticks();
        if (progress >= DONE) {
            progress = 0;
            for (int k = 0; k < plan.consumeSlots().length; k++) {
                items.get(plan.consumeSlots()[k]).shrink(plan.consumeCounts()[k]);
            }
            try (Transaction tx = Transaction.open()) {
                if (!plan.fluidIn().isEmpty()) {
                    for (IhFluidTank t : inTanks) {
                        if (t.stack().is(plan.fluidIn().fluid()) && t.stack().amount() >= plan.fluidIn().amount()) {
                            t.extract(plan.fluidIn().fluid(), plan.fluidIn().amount(), tx);
                            break;
                        }
                    }
                }
                for (int k = 0; k < plan.fluidResults().size(); k++) {
                    outTanks.get(fluidTargets[k]).fill(plan.fluidResults().get(k), tx);
                }
                tx.commit();
            }
            for (ProcessingRecipe.ChanceResult r : plan.results()) {
                if (r.chance() >= 1.0f || level.getRandom().nextFloat() < r.chance()) {
                    insertOutput(r.toStack());
                }
            }
        }
        setStatus(MachineStatus.RUNNING);
        setChanged();
    }

    /**
     * Ausgangstank je Fluid-Ergebnis: zuerst ein Tank mit demselben Fluid, sonst der erste leere (jeder Tank höchstens
     * einmal je Vorgang). Null, wenn ein Ergebnis keinen Platz hat (Statuslampe error, 5.3).
     */
    protected int @Nullable [] fluidTargets(List<IhFluidStack> results) {
        int[] targets = new int[results.size()];
        boolean[] taken = new boolean[outTanks.size()];
        for (int k = 0; k < results.size(); k++) {
            IhFluidStack r = results.get(k);
            int found = -1;
            for (int t = 0; t < outTanks.size() && found < 0; t++) {
                if (!taken[t] && outTanks.get(t).stack().is(r.fluid())) {
                    found = t;
                }
            }
            for (int t = 0; t < outTanks.size() && found < 0; t++) {
                if (!taken[t] && outTanks.get(t).stack().isEmpty()) {
                    found = t;
                }
            }
            if (found < 0 || outTanks.get(found).space() < r.amount()) {
                return null;
            }
            taken[found] = true;
            targets[k] = found;
        }
        return targets;
    }

    private boolean outputsFit(List<ProcessingRecipe.ChanceResult> results) {
        MachineLayout l = layout();
        List<ItemStack> copy = new ArrayList<>();
        for (int i = l.outputStart(); i < l.upgradeStart(); i++) {
            copy.add(items.get(i).copy());
        }
        for (ProcessingRecipe.ChanceResult r : results) {
            ItemStack rest = r.toStack();
            for (int i = 0; i < copy.size() && !rest.isEmpty(); i++) {
                rest = merge(copy, i, rest);
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void insertOutput(ItemStack stack) {
        MachineLayout l = layout();
        ItemStack rest = stack;
        for (int i = l.outputStart(); i < l.upgradeStart() && !rest.isEmpty(); i++) {
            rest = merge(items, i, rest);
        }
    }

    /** Legt {@code stack} in Slot {@code i} der Liste und gibt den Rest zurück. */
    private static ItemStack merge(List<ItemStack> list, int i, ItemStack stack) {
        ItemStack slot = list.get(i);
        if (slot.isEmpty()) {
            list.set(i, stack.copy());
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(slot, stack)) {
            return stack;
        }
        int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
        if (move <= 0) {
            return stack;
        }
        slot.grow(move);
        ItemStack rest = stack.copy();
        rest.shrink(move);
        return rest;
    }

    /** Anzahl eines Items in den Ausgabeslots (für Tests und Anzeigen). */
    public int countOutput(Item item) {
        MachineLayout l = layout();
        int n = 0;
        for (int i = l.outputStart(); i < l.upgradeStart() && i < items.size(); i++) {
            if (items.get(i).is(item)) {
                n += items.get(i).getCount();
            }
        }
        return n;
    }

    /** Nimmt bis zu {@code max} Stück eines Items aus den Ausgabeslots (für Tests und Logistik). */
    public ItemStack takeOutput(Item item, int max) {
        MachineLayout l = layout();
        for (int i = l.outputStart(); i < l.upgradeStart() && i < items.size(); i++) {
            if (items.get(i).is(item)) {
                return removeItem(i, max);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("progress", progress);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getDoubleOr("progress", 0.0);
    }
}
