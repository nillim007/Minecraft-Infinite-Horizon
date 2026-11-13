package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.UUID;

/** GameTests für M5b: Hochtemperaturofen, Regolith-Erhitzer mit Ortsbindung, Kryo-Verflüssiger, Tor zu Stufe 3. */
public final class IhGameTestsM5b {

    private static final Identifier MOON = InfiniteHorizon.id("moon");
    private static final Identifier HELIUM = InfiniteHorizon.id("helium");
    private static final Identifier LIQUID_HELIUM = InfiniteHorizon.id("liquid_helium");

    private IhGameTestsM5b() {
    }

    private static ProcessingMachineBlockEntity machine(GameTestHelper h, Block block) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ProcessingMachineBlockEntity be = h.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    private static long amount(ProcessingMachineBlockEntity be, Identifier fluid) {
        long sum = 0;
        for (IhFluidTank t : be.fluidTanks()) {
            if (t.stack().is(fluid)) {
                sum += t.stack().amount();
            }
        }
        return sum;
    }

    /** {@code raw_tungsten} → {@code tungsten_ingot} im Hochtemperaturofen (400 t, ± 10 %). */
    public static void highTempFurnaceTungsten(GameTestHelper h) {
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.HIGH_TEMPERATURE_FURNACE.get());
        be.setItem(0, new ItemStack(ModItems.simple("raw_tungsten").get()));
        h.succeedWhen(() -> h.assertTrue(be.getItem(be.layout().outputStart()).is(ModItems.simple("tungsten_ingot").get()),
                "kein Wolframbarren"));
    }

    /** Ortsbindung (6.1): Der Regolith-Erhitzer arbeitet nur auf dem Mond; in der Overworld steht er auf {@code error}. */
    public static void regolithHeaterNeedsMoon(GameTestHelper h) {
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.REGOLITH_HEATER.get());
        be.setItem(0, new ItemStack(ModBlocks.simple("moon_regolith").get(), 4));
        h.runAfterDelay(20, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + ", Hinweis " + be.statusHint());
            h.assertTrue(amount(be, HELIUM) == 0, "Helium außerhalb des Mondes");
            h.succeed();
        });
    }

    /** Override Mond: 1 {@code moon_regolith} → 50 mB Helium nach 100 t (± 10 %). */
    public static void regolithHeaterMoonHelium(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 120);
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.REGOLITH_HEATER.get());
        be.setItem(0, new ItemStack(ModBlocks.simple("moon_regolith").get()));
        h.succeedWhen(() -> {
            h.assertTrue(amount(be, HELIUM) == 50, "Helium " + amount(be, HELIUM) + " mB");
            EnvironmentOverrides.remove(override);
        });
    }

    /** 40 mB Helium → 10 mB flüssiges Helium im Kryo-Verflüssiger (20 t). */
    public static void cryoLiquefiesHelium(GameTestHelper h) {
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.CRYO_LIQUEFIER.get());
        be.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(HELIUM, 40));
        h.succeedWhen(() -> h.assertTrue(amount(be, LIQUID_HELIUM) == 10 && amount(be, HELIUM) == 0,
                "flüssiges Helium " + amount(be, LIQUID_HELIUM) + ", Helium " + amount(be, HELIUM)));
    }

    /** Tor zu Stufe 3 (11.4): Magnesium- und Wolframbarren und Helium hergestellt → Stufe 3 offen. */
    public static void tierThreeGate(GameTestHelper h) {
        AgencyData data = AgencyData.get(h.getLevel().getServer());
        Agency a = data.ensureFor(UUID.randomUUID(), 0);
        a.progress().openTier(1);
        a.progress().openTier(2);
        ResearchService.onCrafted(h.getLevel().getServer(), a.id(), List.of(InfiniteHorizon.id("magnesium_ingot"),
                InfiniteHorizon.id("tungsten_ingot")));
        h.assertFalse(a.progress().openTiers().contains(3), "Stufe 3 ohne Helium offen");
        ResearchService.onCrafted(h.getLevel().getServer(), a.id(), List.of(HELIUM));
        h.assertTrue(a.progress().openTiers().contains(3), "Stufe 3 nicht offen: " + a.progress().openTiers());
        h.succeed();
    }
}
