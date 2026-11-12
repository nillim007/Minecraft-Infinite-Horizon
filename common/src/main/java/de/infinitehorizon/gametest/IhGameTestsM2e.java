package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.api.energy.EnergyTarget;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.machine.BatteryBlockEntity;
import de.infinitehorizon.machine.FluidFuelGeneratorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * GameTests für M2e (27.3): Treibstoffgenerator, Akku mit Ausgangsseite und Ladeslot, Leistungstest mit 100
 * Zerkleinerern (27.2). Registrierung über {@link IhTestCatalog} (erzeugt aus {@code common/gametests.json}).
 */
public final class IhGameTestsM2e {

    private IhGameTestsM2e() {
    }

    private static final Identifier KEROSENE = InfiniteHorizon.id("kerosene");

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, BlockPos pos, Block block, Direction facing,
                                                            Class<T> type) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(MachineBlock.FACING)) {
            s = s.setValue(MachineBlock.FACING, facing);
        }
        h.setBlock(pos, s);
        T be = h.getBlockEntity(pos, type);
        be.refreshLayout();
        return be;
    }

    // ------------------------------------------------------------------ Treibstoffgenerator (27.2, 27.3 M2e)

    /** Kerosin aus einem kerosene_bucket im Leerslot: der Generator liefert 80 J/t, der leere Eimer bleibt. */
    public static void fuelGeneratorKeroseneBucket(GameTestHelper h) {
        FluidFuelGeneratorBlockEntity g = machine(h, new BlockPos(1, 1, 1), ModBlocks.FUEL_GENERATOR.get(), Direction.NORTH,
                FluidFuelGeneratorBlockEntity.class);
        int drain = g.layout().drainStart();
        g.setItem(drain, new ItemStack(ModItems.simple("kerosene_bucket").get()));
        long[] start = new long[1];
        h.runAfterDelay(3, () -> {
            h.assertTrue(g.getItem(drain).is(Items.BUCKET), "Leerslot: " + g.getItem(drain));
            h.assertTrue(g.fluidTanks().getFirst().stack().is(KEROSENE), "Tank: " + g.fluidTanks().getFirst().stack());
            h.assertTrue(g.status() == MachineStatus.RUNNING, "Status " + g.status());
            start[0] = g.energyStorage().getAmount();
        });
        h.runAfterDelay(13, () -> {
            long produced = g.energyStorage().getAmount() - start[0];
            h.assertTrue(produced == 800, "in 10 t erzeugt: " + produced + " J, erwartet 800 (80 J/t)");
            h.succeed();
        });
    }

    /** 4 mB Kerosin reichen 16 t mit 80 J/t (27.2: 1 mB je 4 t), danach idle. */
    public static void fuelGeneratorFourMbSixteenTicks(GameTestHelper h) {
        FluidFuelGeneratorBlockEntity g = machine(h, new BlockPos(1, 1, 1), ModBlocks.FUEL_GENERATOR.get(), Direction.NORTH,
                FluidFuelGeneratorBlockEntity.class);
        g.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(KEROSENE, 4));
        h.runAfterDelay(25, () -> {
            h.assertTrue(g.energyStorage().getAmount() == 16 * 80, "Energie " + g.energyStorage().getAmount() + ", erwartet 1280");
            h.assertTrue(g.fluidTanks().getFirst().stack().isEmpty(), "Tank nicht leer: " + g.fluidTanks().getFirst().stack());
            h.assertTrue(g.status() == MachineStatus.IDLE, "Status " + g.status());
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Akku (6.3, 4.4)

    /** Der Akku gibt nur über die Ausgangsseite ab: der Zerkleinerer davor bekommt Energie, der seitliche nicht. */
    public static void batteryOutputSideOnly(GameTestHelper h) {
        BatteryBlockEntity b = machine(h, new BlockPos(2, 1, 2), ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        b.energyStorage().setAmountUnchecked(100_000);
        ProcessingMachineBlockEntity front = machine(h, new BlockPos(2, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        ProcessingMachineBlockEntity side = machine(h, new BlockPos(3, 1, 2), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        h.runAfterDelay(5, () -> {
            h.assertTrue(front.energyStorage().getAmount() > 0, "Zerkleinerer vor der Ausgangsseite hat keine Energie");
            h.assertTrue(side.energyStorage().getAmount() == 0, "Zerkleinerer an der Eingangsseite bekam "
                    + side.energyStorage().getAmount() + " J");
            h.succeed();
        });
    }

    /**
     * Ein- und Ausgang höchstens 512 J/t (27.2, je Tick geprüft); über die Loader-API nimmt die Ausgangsseite nichts an,
     * und ein Verbraucher nur mit Loader-API ({@code minecraft:target}) bekommt 512 J/t.
     */
    public static void batteryIoLimitAndPlatform(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        BatteryBlockEntity b = machine(h, pos, ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        long io = b.energyStorage().getMaxInsert();
        h.assertTrue(io == 512, "io_per_tick " + io);
        BlockPos abs = h.absolutePos(pos);
        EnergyTarget in = Services.ENERGY.findTarget(h.getLevel(), abs, Direction.SOUTH);
        h.assertTrue(in != null, "Loader-API: Eingangsseite fehlt");
        long first = in.insert(1000, false);
        long second = in.insert(1000, false);
        h.assertTrue(first == 512 && second == 0, "in einem Tick angenommen: " + first + " + " + second + ", erwartet 512 + 0");
        EnergyTarget out = Services.ENERGY.findTarget(h.getLevel(), abs, Direction.NORTH);
        h.assertTrue(out == null || out.insert(1000, true) == 0, "Ausgangsseite nimmt über die Loader-API an");
        h.setBlock(pos.north(), Blocks.TARGET);
        BlockPos consumer = h.absolutePos(pos.north());
        b.energyStorage().setAmountUnchecked(100_000);
        long[] start = new long[1];
        h.runAfterDelay(3, () -> start[0] = Services.ENERGY.testConsumerAmount(h.getLevel(), consumer));
        h.runAfterDelay(13, () -> {
            long got = Services.ENERGY.testConsumerAmount(h.getLevel(), consumer) - start[0];
            h.assertTrue(got == 10 * 512, "Verbraucher bekam in 10 t " + got + " J, erwartet 5120");
            h.succeed();
        });
    }

    /** Ladeslot (6.3): lädt ein Item mit Loader-Energie (Test-Item) und einen Akku als Item (Komponente). */
    public static void batteryChargeSlot(GameTestHelper h) {
        BatteryBlockEntity a = machine(h, new BlockPos(1, 1, 1), ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        BatteryBlockEntity b = machine(h, new BlockPos(3, 1, 1), ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        a.energyStorage().setAmountUnchecked(100_000);
        b.energyStorage().setAmountUnchecked(100_000);
        h.assertTrue(!a.canPlaceItem(0, new ItemStack(Items.STONE)), "Ladeslot nimmt Stein an");
        h.assertTrue(a.canPlaceItem(0, new ItemStack(Items.REDSTONE_BLOCK)), "Ladeslot lehnt das Test-Item ab");
        h.assertTrue(a.canPlaceItem(0, new ItemStack(ModItems.BATTERY.get())), "Ladeslot lehnt einen Akku ab");
        long before = Services.ENERGY.testItemAmount();
        a.setItem(0, new ItemStack(Items.REDSTONE_BLOCK));
        b.setItem(0, new ItemStack(ModItems.BATTERY.get()));
        h.runAfterDelay(4, () -> {
            long charged = Services.ENERGY.testItemAmount() - before;
            h.assertTrue(charged > 0 && charged % 512 == 0 && charged <= 4 * 512, "Test-Item geladen: " + charged + " J");
            Long inItem = b.getItem(0).get(ModDataComponents.ENERGY.get());
            h.assertTrue(inItem != null && inItem > 0 && inItem % 512 == 0, "Akku-Item geladen: " + inItem);
            h.assertTrue(b.energyStorage().getAmount() == 100_000 - inItem, "Energie nicht abgezogen: " + b.energyStorage().getAmount());
            h.succeed();
        });
    }

    /** Abgebauter Akku behält seine Energie als {@code infinite_horizon:energy} (4.1). */
    public static void batteryKeepsEnergyWhenBroken(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BatteryBlockEntity b = machine(h, pos, ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        b.energyStorage().setAmountUnchecked(123_456);
        List<ItemStack> drops = Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), b, null, ItemStack.EMPTY);
        h.assertTrue(drops.size() == 1, "Drops: " + drops);
        Long stored = drops.getFirst().get(ModDataComponents.ENERGY.get());
        h.assertTrue(stored != null && stored == 123_456, "Komponente am Item: " + stored);
        BatteryBlockEntity b2 = machine(h, new BlockPos(3, 1, 1), ModBlocks.BATTERY.get(), Direction.NORTH, BatteryBlockEntity.class);
        b2.applyComponentsFromItemStack(drops.getFirst());
        h.assertTrue(b2.energyStorage().getAmount() == 123_456, "Wiederhergestellt: " + b2.energyStorage().getAmount());
        h.succeed();
    }

    // ------------------------------------------------------------------ Leistung (27.2)

    // SPEC-GAP: Grenzwerte des Leistungstests (1 ms, × 3 auf GitHub) stehen nur in 27.2 (spec-gaps #30)
    private static final double LIMIT_MS = 1.0;
    private static final double CI_FACTOR = 3.0;
    private static final int ROWS = 10;
    private static final int PER_ROW = 10;
    /** Aufwärmen vor der Messung: läuft auf Fabric als letzter Durchgang, Reste vorheriger Tests klingen ab. */
    private static final int WARMUP = 200;
    private static final int MEASURE = 200;

    /**
     * 100 laufende Zerkleinerer, versorgt über 10 Kupferkabel-Netze (je 10 × 20 J/t ≤ 256 J/t) aus je einem Akku. Gemessen
     * wird 200 t lang die Zeit je Server-Tick (die Server-eigene Messung mit {@code System.nanoTime}); Mittelwert ≤ 1 ms,
     * auf GitHub-Runnern ≤ 3 ms. Der Test läuft in eigener Umgebung allein in seinem Durchgang.
     */
    public static void performanceHundredCrushers(GameTestHelper h) {
        List<ProcessingMachineBlockEntity> crushers = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            int z = 1 + 2 * r;
            BatteryBlockEntity b = machine(h, new BlockPos(1, 1, z), ModBlocks.BATTERY.get(), Direction.EAST, BatteryBlockEntity.class);
            b.energyStorage().setAmountUnchecked(b.energyStorage().getCapacity());
            for (int i = 0; i < PER_ROW; i++) {
                h.setBlock(new BlockPos(2 + i, 1, z), ModBlocks.COPPER_CABLE.get());
                ProcessingMachineBlockEntity c = machine(h, new BlockPos(2 + i, 2, z), ModBlocks.CRUSHER.get(), Direction.NORTH,
                        ProcessingMachineBlockEntity.class);
                c.setItem(0, new ItemStack(Items.RAW_IRON, 64));
                crushers.add(c);
            }
        }
        // Zeit je Server-Tick misst der Server selbst (Mittel der letzten 100 Ticks); zwei Fenster = 200 t
        long[] sum = {0};
        int[] windows = {0};
        h.onEachTick(() -> {
            long t = h.getTick();
            if (t == WARMUP + MEASURE / 2 || t == WARMUP + MEASURE) {
                sum[0] += h.getLevel().getServer().getAverageTickTimeNanos();
                windows[0]++;
            }
        });
        h.runAfterDelay(WARMUP + MEASURE + 2, () -> {
            double meanMs = sum[0] / (double) Math.max(1, windows[0]) / 1_000_000.0;
            boolean ci = "true".equalsIgnoreCase(System.getenv("CI"));
            double limit = ci ? LIMIT_MS * CI_FACTOR : LIMIT_MS;
            InfiniteHorizon.LOG.info("[Leistungstest] 100 Zerkleinerer: {} ms je Tick im Mittel über {} t (Grenze {} ms)",
                    String.format(java.util.Locale.ROOT, "%.3f", meanMs), windows[0] * MEASURE / 2, limit);
            long running = crushers.stream().filter(c -> c.status() == MachineStatus.RUNNING).count();
            h.assertTrue(running == crushers.size(), "laufend: " + running + " von " + crushers.size());
            h.assertTrue(meanMs <= limit, String.format(java.util.Locale.ROOT, "Mittel %.3f ms je Tick > %.1f ms", meanMs, limit));
            h.succeed();
        });
    }
}
