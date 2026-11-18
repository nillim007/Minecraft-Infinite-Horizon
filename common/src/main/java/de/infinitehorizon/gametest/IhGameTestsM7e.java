package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.creature.FrostLurker;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.Hazard;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.machine.GasCollectorBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.LanderRoutes;
import de.infinitehorizon.space.LaunchChecklist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M7e, Teil 1 (13.3, 13.4, 9.1, 9.2, 18): Uranus- und Neptun-Orbit, Triton, Tiefraumanzug, Heizmodul. */
public final class IhGameTestsM7e {

    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final Identifier TRITON = InfiniteHorizon.id("triton");

    private IhGameTestsM7e() {
    }

    /** Gas-Sammler im Uranus-Orbit (13.3): Standard Methan, per Betriebsart Wasserstoff. */
    public static void uranusCollectorSwitch(GameTestHelper h) {
        h.setBlock(MACHINE, ModBlocks.GAS_COLLECTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        GasCollectorBlockEntity be = h.getBlockEntity(MACHINE, GasCollectorBlockEntity.class);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        h.assertTrue(be.modeCount() == 2, "Betriebsarten " + be.modeCount());
        h.assertTrue(InfiniteHorizon.id("methane").equals(be.gas(h.getLevel())), "Standard " + be.gas(h.getLevel()));
        be.setMode(1);
        h.assertTrue(InfiniteHorizon.id("hydrogen").equals(be.gas(h.getLevel())), "umgeschaltet " + be.gas(h.getLevel()));
        h.succeedWhen(() -> h.assertTrue(be.fluidTanks().getFirst().stack().is(InfiniteHorizon.id("hydrogen")),
                "kein Wasserstoff: " + be.fluidTanks().getFirst().stack()));
    }

    /** Frostkriecher (18): unsichtbar im Eis, bricht hervor, wenn ein Spieler näher als {@code emerge_distance} kommt. */
    public static void frostLurker(GameTestHelper h) {
        FrostLurker l = h.spawn(ModEntities.FROST_LURKER.get(), new Vec3(1.5, 1, 1.5));
        h.assertTrue(l.hidden() && l.isInvisible(), "nicht versteckt");
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(6.5, 1, 6.5));
        h.runAfterDelay(5, () -> {
            try {
                h.assertTrue(l.hidden(), "bricht zu früh hervor");
                p.teleportTo(l.getX() + 2, l.getY(), l.getZ());
            } catch (RuntimeException e) {
                IhTestPlayers.remove(p);
                throw e;
            }
        });
        h.runAfterDelay(10, () -> {
            try {
                h.assertFalse(l.hidden() || l.isInvisible(), "nicht hervorgebrochen");
                h.assertTrue(l.getTarget() == p, "greift nicht an");
            } finally {
                IhTestPlayers.remove(p);
                l.discard();
            }
            h.succeed();
        });
    }

    /** Triton (13.2): Tiefraumanzug allein zu kalt, mit Heizmodul geschützt. */
    public static void heatingModule(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.DEEP_SPACE_SUIT_HELMET.get()));
            ItemStack chest = new ItemStack(ModItems.DEEP_SPACE_SUIT_CHESTPLATE.get());
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.DEEP_SPACE_SUIT_LEGGINGS.get()));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.DEEP_SPACE_SUIT_BOOTS.get()));
            h.assertTrue(HazardTracker.evaluate(p, HazardTracker.rules()).hazards().contains(Hazard.COLD), "nicht zu kalt");
            chest.set(ModDataComponents.SUIT_MODULES.get(), List.of(InfiniteHorizon.id("heating_module")));
            h.assertFalse(HazardTracker.evaluate(p, HazardTracker.rules()).hazards().contains(Hazard.COLD), "Heizmodul wirkt nicht");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Landefähre (12.7): Triton hängt am Neptun-Orbit; Triton hat sein Geysir-Feature. */
    public static void landerNeptune(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            Agency agency = AgencyManager.ensureFor(p);
            agency.progress().addProbeReached(TRITON);
            List<LaunchChecklist.Destination> d = LanderRoutes.destinations(h.getLevel(), Long.MAX_VALUE,
                    PlanetRegistry.get().planet(InfiniteHorizon.id("neptune_orbit")).orElseThrow(), agency);
            h.assertTrue(d.stream().anyMatch(x -> x.dimension().equals(TRITON)), "Ziele vom Neptun-Orbit: " + d);
            h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                    .getValue(InfiniteHorizon.id("triton_geyser")) != null, "Geysir-Feature fehlt");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
