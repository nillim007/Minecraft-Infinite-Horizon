package de.sternenwerft.gametest;

import com.google.gson.JsonPrimitive;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.machine.CombustionGeneratorBlockEntity;
import de.sternenwerft.machine.ProcessingMachineBlockEntity;
import de.sternenwerft.platform.Services;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTests aus Kapitel 27, Meilenstein 1. Die Logik steht hier; die Loader registrieren nur
 * (Fabric: Annotation, NeoForge: RegisterGameTestsEvent). Namen nach 3A.8.
 */
public final class SwGameTests {

    /** Struktur unter data/sternenwerft/structure/gametest/empty.nbt (leer, 8×8×8). */
    public static final String STRUCTURE = "sternenwerft:gametest/empty";

    public static final String CRUSHER_CHAIN = "crusher_rawiron_twodustwithin220ticks";
    public static final int CRUSHER_CHAIN_TICKS = 220;
    public static final String BALANCE_RELOAD = "crusher_balancechange_changesduration";
    public static final int BALANCE_RELOAD_TICKS = 20;
    public static final String PLATFORM_CONSUMER = "cable_platformconsumer_receivesenergy";
    public static final int PLATFORM_CONSUMER_TICKS = 100;

    private static final BlockPos GENERATOR = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE_A = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE_B = new BlockPos(3, 1, 1);
    private static final BlockPos END = new BlockPos(4, 1, 1);

    private SwGameTests() {
    }

    private static CombustionGeneratorBlockEntity generatorWithCoal(GameTestHelper helper) {
        helper.setBlock(GENERATOR, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(CABLE_A, ModBlocks.COPPER_CABLE.get());
        helper.setBlock(CABLE_B, ModBlocks.COPPER_CABLE.get());
        CombustionGeneratorBlockEntity gen = helper.getBlockEntity(GENERATOR, CombustionGeneratorBlockEntity.class);
        gen.setItem(0, new ItemStack(Items.COAL));
        return gen;
    }

    /** Generator mit Kohle → Kabel → Zerkleinerer: 1 raw_iron → 2 iron_dust in höchstens 220 t. */
    public static void crusherRawIronTwoDust(GameTestHelper helper) {
        generatorWithCoal(helper);
        helper.setBlock(END, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity crusher = helper.getBlockEntity(END, ProcessingMachineBlockEntity.class);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        helper.succeedWhen(() -> helper.assertTrue(crusher.countOutput(ModItems.IRON_DUST.get()) == 2,
                "Erwartet 2 iron_dust, vorhanden: " + crusher.countOutput(ModItems.IRON_DUST.get())));
    }

    /**
     * Geänderter Wert in machines.json ändert die Vorgangsdauer. Der Test ersetzt die geladenen Werte genau so,
     * wie es der Reload-Listener nach /reload tut (BalanceManager.set), und stellt sie im selben Tick wieder her.
     */
    public static void crusherBalanceChangesDuration(GameTestHelper helper) {
        helper.setBlock(END, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity crusher = helper.getBlockEntity(END, ProcessingMachineBlockEntity.class);
        ItemStack input = new ItemStack(Items.RAW_IRON);
        BalanceManager original = BalanceManager.get();
        int before = crusher.durationFor(helper.getLevel(), input);
        int wanted = before * 2;
        int after;
        try {
            BalanceManager.set(original.withValue("machines", crusher.machineId(), "ticks", new JsonPrimitive(wanted)));
            after = crusher.durationFor(helper.getLevel(), input);
        } finally {
            BalanceManager.set(original);
        }
        helper.assertTrue(before > 0, "Kein crushing-Rezept für raw_iron gefunden");
        helper.assertTrue(after == wanted, "Dauer vorher " + before + ", nachher " + after + ", erwartet " + wanted);
        helper.succeed();
    }

    /** Ein Verbraucher, der nur die Loader-API nutzt, erhält Strom aus dem Kabel. */
    public static void cablePlatformConsumerReceivesEnergy(GameTestHelper helper) {
        generatorWithCoal(helper);
        helper.setBlock(END, Blocks.TARGET);
        BlockPos abs = helper.absolutePos(END);
        helper.succeedWhen(() -> {
            long amount = Services.ENERGY.testConsumerAmount(helper.getLevel(), abs);
            helper.assertTrue(amount > 0, "Test-Verbraucher hat keine Energie erhalten (" + amount + ")");
        });
    }
}
