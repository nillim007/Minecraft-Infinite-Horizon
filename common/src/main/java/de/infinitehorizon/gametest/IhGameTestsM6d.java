package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.machine.FusionReactorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.station.GravityPlateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** GameTests für M6d (6.3, 4.3, 12.9): Fusionsreaktor, Schwerkraftplatte. */
public final class IhGameTestsM6d {

    /** Controller in der Mitte der Vorderseite (Blick nach Norden); die Hülle reicht nach Süden. */
    private static final BlockPos CONTROLLER = new BlockPos(5, 3, 2);

    private IhGameTestsM6d() {
    }

    /** Baut den 5×5×5-Fusionsreaktor (hohle Hülle aus {@code fusion_casing}) und gibt den Controller zurück. */
    static FusionReactorBlockEntity buildFusion(GameTestHelper h) {
        for (int r = -2; r <= 2; r++) {
            for (int up = -2; up <= 2; up++) {
                for (int back = 0; back <= 4; back++) {
                    boolean shell = Math.abs(r) == 2 || Math.abs(up) == 2 || back == 0 || back == 4;
                    if (shell && !(r == 0 && up == 0 && back == 0)) {
                        h.setBlock(CONTROLLER.offset(r, up, back), ModBlocks.FUSION_CASING.get());
                    }
                }
            }
        }
        h.setBlock(CONTROLLER, ModBlocks.FUSION_REACTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        FusionReactorBlockEntity be = h.getBlockEntity(CONTROLLER, FusionReactorBlockEntity.class);
        be.refreshLayout();
        h.assertTrue(be.checkStructure(h.getLevel()), "Aufbau unvollständig: " + be.firstMissing());
        return be;
    }

    /**
     * Abnahme M6 (27): Der Fusionsreaktor schaltet 40 t nach Kühlungsausfall ab (Status {@code error}, keine Explosion);
     * Toleranz 10 %.
     */
    public static void fusionShutsDownWithoutCooling(GameTestHelper h) {
        FusionReactorBlockEntity be = buildFusion(h);
        // Reihenfolge der Tanks wie in generators.json: Wasserstoff, Helium, flüssiges Helium, Ausgabe
        be.fluidTanks().get(0).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("hydrogen"), 16000));
        be.fluidTanks().get(1).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("helium"), 16000));
        be.fluidTanks().get(2).setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("liquid_helium"), 1000));
        JsonObject safety = BalanceManager.get().entry("generators", InfiniteHorizon.id("fusion_reactor").toString())
                .orElseThrow().getAsJsonObject("safety");
        int limit = safety.get("shutdown_after_ticks_without_cooling").getAsInt();
        long[] cutAt = {-1};
        h.onEachTick(() -> {
            be.energyStorage().setAmountUnchecked(0); // will immer Strom erzeugen
            if (cutAt[0] < 0 && be.status() == MachineStatus.RUNNING) {
                be.fluidTanks().get(2).setStackUnchecked(IhFluidStack.EMPTY);
                cutAt[0] = h.getTick();
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(cutAt[0] >= 0, "Reaktor läuft nicht an: " + be.status() + ", Aufbau " + be.structureComplete());
            h.assertTrue(be.status() == MachineStatus.ERROR, "noch nicht abgeschaltet nach " + (h.getTick() - cutAt[0]) + " t");
            long elapsed = h.getTick() - cutAt[0];
            h.assertTrue(elapsed >= limit * 9 / 10 && elapsed <= limit * 11 / 10 + 1, "Abschaltung nach " + elapsed + " t");
            h.assertTrue(h.getBlockState(CONTROLLER).is(ModBlocks.FUSION_REACTOR.get()), "Reaktor zerstört");
        });
    }

    /** Ohne vollständige Hülle steht der Controller auf {@code error} und erzeugt nichts (4.3). */
    public static void fusionNeedsStructure(GameTestHelper h) {
        FusionReactorBlockEntity be = buildFusion(h);
        // erst entfernen, wenn der Controller einmal getickt und sich angemeldet hat (4.3: Gehäuse melden Änderungen)
        h.runAfterDelay(2, () -> h.setBlock(CONTROLLER.offset(1, 1, 4), net.minecraft.world.level.block.Blocks.AIR));
        h.runAfterDelay(6, () -> {
            h.assertFalse(be.structureComplete(), "Aufbau gilt als vollständig");
            h.assertTrue(be.status() == MachineStatus.ERROR, "Status " + be.status());
            h.succeed();
        });
    }

    /** Schwerkraftplatte im Orbit (12.9): über der laufenden Platte gilt 1 g, außerhalb des Bereichs die Orbit-Schwerkraft. */
    public static void gravityPlateOrbit(GameTestHelper h) {
        BlockPos plate = new BlockPos(4, 1, 4);
        h.setBlock(plate, ModBlocks.GRAVITY_PLATE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        GravityPlateBlockEntity be = h.getBlockEntity(plate, GravityPlateBlockEntity.class);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 3, 4.5));
        JsonObject misc = BalanceManager.get().entry("misc", "gravity").orElseThrow();
        double perG = misc.get("gravity_per_g").getAsDouble();
        double orbitG = misc.get("orbit_g").getAsDouble();
        h.succeedWhen(() -> {
            h.assertTrue(be.status() == MachineStatus.RUNNING, "Platte läuft nicht: " + be.status());
            p.teleportTo(h.absoluteVec(new Vec3(4.5, 3, 4.5)).x, h.absoluteVec(new Vec3(4.5, 3, 4.5)).y,
                    h.absoluteVec(new Vec3(4.5, 3, 4.5)).z);
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - perG) < 1e-9,
                    "über der Platte " + p.getAttributeValue(Attributes.GRAVITY));
            Vec3 out = h.absoluteVec(new Vec3(4.5, 12, 4.5));
            p.teleportTo(out.x, out.y, out.z);
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - perG * orbitG) < 1e-9,
                    "über dem Bereich " + p.getAttributeValue(Attributes.GRAVITY));
            IhTestPlayers.remove(p);
        });
    }
}
