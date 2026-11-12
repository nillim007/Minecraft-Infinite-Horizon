package de.infinitehorizon.gametest;

import de.infinitehorizon.core.fluid.ComponentContainers;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.Suits;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.OxygenGeneratorBlockEntity;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** GameTests für M3d (27.5): Anzug T1 im Vakuum, Sauerstoffgenerator füllt Flaschen, Druckflasche nur für Gase. */
public final class IhGameTestsM3d {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier OXYGEN = Identifier.fromNamespaceAndPath("infinite_horizon", "oxygen");

    private IhGameTestsM3d() {
    }

    private static void wearSuit(ServerPlayer p, int oxygen) {
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        ItemStack chest = new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get());
        Suits.setOxygen(chest, oxygen);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
    }

    /**
     * Vollständiger Anzug mit 600 mB im Override Mond: nach 200 t 0 % Belastung und noch 590 mB (27.5 M3d, Toleranz
     * 10 % der Zeit).
     */
    public static void suitProtectsOnMoon(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        Services.ATTACHMENTS.hazardState(p).reset();
        wearSuit(p, 600);
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 260);
        h.succeedWhen(() -> {
            h.assertTrue(h.getTick() >= 200, "noch keine 200 t");
            HazardState s = Services.ATTACHMENTS.hazardState(p);
            for (Hazard hz : Hazard.values()) {
                h.assertTrue(s.buildup(hz) == 0, "Belastung " + hz.id() + " = " + s.buildup(hz));
            }
            int o2 = Suits.oxygen(p.getItemBySlot(EquipmentSlot.CHEST));
            h.assertTrue(o2 == 590, "O₂ im Anzug " + o2 + " mB, erwartet 590");
            h.assertTrue(HazardTracker.lastResult(p) != null && !HazardTracker.lastResult(p).breathable(), "nicht im Vakuum");
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(p);
        });
    }

    /** Ohne Brustplatte ist der Anzug unvollständig und schützt nicht (9.1). */
    public static void incompleteSuitDoesNotProtect(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        wearSuit(p, 600);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        h.assertTrue(Suits.worn(p) == null, "drei Teile gelten als Anzug");
        wearSuit(p, 600);
        h.assertTrue(Suits.worn(p) != null && Suits.worn(p).tier() == 1, "vollständiger Anzug nicht erkannt");
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Eine leere {@code oxygen_canister} im Füllslot eines Sauerstoffgenerators mit 600 mB ist nach 1 Tick voll. */
    public static void generatorFillsCanister(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.OXYGEN_GENERATOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        OxygenGeneratorBlockEntity be = h.getBlockEntity(pos, OxygenGeneratorBlockEntity.class);
        be.refreshLayout();
        be.tank().setStackUnchecked(IhFluidStack.of(OXYGEN, 600));
        ItemStack can = new ItemStack(ModItems.simple("oxygen_canister").get());
        be.setItem(be.layout().fillStart(), can);
        h.succeedWhen(() -> {
            h.assertTrue(h.getTick() <= 2, "zu langsam");
            ItemStack now = be.getItem(be.layout().fillStart());
            long amount = ComponentContainers.content(now).amount();
            h.assertTrue(amount == 600 && ComponentContainers.isFull(now), "Flasche hat " + amount + " mB");
            h.assertTrue(be.tank().stack().isEmpty(), "Puffer nicht geleert");
        });
    }

    /** Die Druckflasche nimmt nur Gase (6.3). */
    public static void pressureBottleOnlyGas(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.PRESSURE_BOTTLE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        FluidTankBlockEntity be = h.getBlockEntity(pos, FluidTankBlockEntity.class);
        be.refreshLayout();
        h.assertTrue(be.tank().accepts(OXYGEN), "Sauerstoff abgelehnt");
        h.assertTrue(!be.tank().accepts(Identifier.withDefaultNamespace("water")), "Wasser angenommen");
        h.succeed();
    }
}
