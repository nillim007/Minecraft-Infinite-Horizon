package de.infinitehorizon.gametest;

import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.environment.RadiationZones;
import de.infinitehorizon.machine.FissionReactorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** GameTests für M8a (6.3, 4.3, 15): Kernreaktor. */
public final class IhGameTestsM8a {

    private static final BlockPos CONTROLLER = new BlockPos(5, 3, 2);
    private static final Identifier WATER = Identifier.withDefaultNamespace("water");

    private IhGameTestsM8a() {
    }

    /** Baut den 5×5×5-Kernreaktor (hohle Hülle aus {@code fission_casing}) und gibt den Controller zurück. */
    static FissionReactorBlockEntity build(GameTestHelper h) {
        for (int r = -2; r <= 2; r++) {
            for (int up = -2; up <= 2; up++) {
                for (int back = 0; back <= 4; back++) {
                    boolean shell = Math.abs(r) == 2 || Math.abs(up) == 2 || back == 0 || back == 4;
                    if (shell && !(r == 0 && up == 0 && back == 0)) {
                        h.setBlock(CONTROLLER.offset(r, up, back), ModBlocks.FISSION_CASING.get());
                    }
                }
            }
        }
        h.setBlock(CONTROLLER, ModBlocks.FISSION_REACTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        FissionReactorBlockEntity be = h.getBlockEntity(CONTROLLER, FissionReactorBlockEntity.class);
        be.refreshLayout();
        h.assertTrue(be.checkStructure(h.getLevel()), "Aufbau unvollständig: " + be.firstMissing());
        return be;
    }

    private static int fuelSlot(FissionReactorBlockEntity be) {
        return be.layout().fuelStart();
    }

    /** Mit Brennstab und Wasser läuft der Reaktor, erzeugt Strom und bleibt kalt; Brennstoff-Slots nehmen nur Brennstäbe. */
    public static void runsCooled(GameTestHelper h) {
        FissionReactorBlockEntity be = build(h);
        h.assertFalse(be.canPlaceItem(fuelSlot(be), new ItemStack(Items.COAL)), "Kohle als Brennstoff");
        h.assertTrue(be.canPlaceItem(fuelSlot(be), new ItemStack(ModItems.simple("fuel_rod").get())), "Brennstab abgelehnt");
        be.setItem(fuelSlot(be), new ItemStack(ModItems.simple("fuel_rod").get()));
        be.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(WATER, 16000));
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(0));
        h.runAfterDelay(20, () -> {
            h.assertTrue(be.status() == MachineStatus.RUNNING, "Status " + be.status());
            h.assertTrue(be.heat().heat() == 0, "Hitze " + be.heat().heat());
            h.assertTrue(be.getItem(fuelSlot(be)).isEmpty() && be.burnLeft() > 0, "Brennstab nicht eingesetzt");
            h.assertTrue(be.fluidTanks().getFirst().stack().amount() < 16000, "kein Wasser verbraucht");
            h.succeed();
        });
    }

    /**
     * Ohne Kühlung steigt die Hitze; bei {@code scram_at} Notabschaltung ({@code error}, keine Explosion) und eine
     * Strahlungszone mit {@code radiation_zone.radiation} im Umkreis.
     */
    public static void scramRadiation(GameTestHelper h) {
        FissionReactorBlockEntity be = build(h);
        be.setItem(fuelSlot(be), new ItemStack(ModItems.simple("fuel_rod").get()));
        be.setHeat(be.rules().scramAt() - 4);
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(0));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        h.runAfterDelay(10, () -> {
            try {
                h.assertTrue(be.heat().scrammed() && be.status() == MachineStatus.ERROR, "keine Notabschaltung: " + be.heat());
                h.assertTrue(h.getBlockState(CONTROLLER).is(ModBlocks.FISSION_REACTOR.get()), "Reaktor zerstört");
                BlockPos abs = h.absolutePos(CONTROLLER);
                h.assertTrue(RadiationZones.at(h.getLevel(), abs).isPresent(), "keine Strahlungszone");
                h.assertTrue(PlanetResolver.at(h.getLevel(), p.blockPosition()).zones().stream()
                        .anyMatch(z -> PlanetResolver.REACTOR_ZONE.equals(z.id())), "Zone fehlt beim Spieler");
                h.assertTrue(HazardTracker.evaluate(p, HazardTracker.rules()).hazards().contains(Hazard.RADIATION), "keine Strahlung");
                h.assertTrue(RadiationZones.at(h.getLevel(), abs.east(40)).isEmpty(), "Zone zu groß");
            } finally {
                RadiationZones.get(h.getLevel()).clear();
                IhTestPlayers.remove(p);
            }
            h.succeed();
        });
    }

    /** Ohne vollständige Hülle steht der Controller auf {@code error} (4.3). */
    public static void needsStructure(GameTestHelper h) {
        FissionReactorBlockEntity be = build(h);
        h.runAfterDelay(2, () -> h.setBlock(CONTROLLER.offset(1, 1, 4), Blocks.AIR));
        h.runAfterDelay(6, () -> {
            h.assertFalse(be.structureComplete(), "Aufbau gilt als vollständig");
            h.assertTrue(be.status() == MachineStatus.ERROR, "Status " + be.status());
            h.succeed();
        });
    }
}
