package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.event.EventService;
import de.infinitehorizon.event.PlanetEvents;
import de.infinitehorizon.event.WeatherStations;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** GameTests für M6c (6.1, 11.4, 19): Schwerelos-Fertiger, Tor zu Stufe 4, Weltraumwetter-Station. */
public final class IhGameTestsM6c {

    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    private IhGameTestsM6c() {
    }

    private static <T extends MachineBlockEntity> T machine(GameTestHelper h, Block block, Class<T> type) {
        h.setBlock(MACHINE, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        T be = h.getBlockEntity(MACHINE, type);
        be.refreshLayout();
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    private static void fill(ProcessingMachineBlockEntity be) {
        be.setItem(0, new ItemStack(ModItems.simple("magnesium_ingot").get(), 2));
        be.setItem(1, new ItemStack(ModItems.simple("copper_wire").get(), 2));
        be.fluidTanks().getFirst().setStackUnchecked(IhFluidStack.of(InfiniteHorizon.id("liquid_helium"), 1000));
    }

    /** Abnahme M6 (27): {@code zero_g_fabricator} außerhalb des Orbits = {@code error}. */
    public static void zeroGErrorOutsideOrbit(GameTestHelper h) {
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.ZERO_G_FABRICATOR.get(), ProcessingMachineBlockEntity.class);
        fill(be);
        h.runAfterDelay(20, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + "/" + be.statusHint());
            h.assertTrue(be.countOutput(ModItems.simple("superconductor_wire").get()) == 0, "auf der Erde gefertigt");
            h.succeed();
        });
    }

    /**
     * Im Erdorbit: Supraleiterdraht in höchstens 220 t (200 t + 10 %); der erste abgeschlossene Vorgang öffnet Stufe 4
     * (Tor {@code machine_completed}, 11.4).
     */
    public static void zeroGWireOpensTierFour(GameTestHelper h) {
        AgencyData data = AgencyData.get(h.getLevel().getServer());
        Agency agency = data.ensureFor(UUID.randomUUID(), 0);
        for (int t = 1; t <= 3; t++) {
            agency.progress().openTier(t);
        }
        agency.progress().research(InfiniteHorizon.id("superconductors"));
        ProcessingMachineBlockEntity be = machine(h, ModBlocks.ZERO_G_FABRICATOR.get(), ProcessingMachineBlockEntity.class);
        be.setOwner(agency.id());
        fill(be);
        h.succeedWhen(() -> {
            h.assertTrue(be.countOutput(ModItems.simple("superconductor_wire").get()) == 1,
                    "kein Draht, Status " + be.status() + "/" + be.statusHint());
            h.assertTrue(agency.progress().openTiers().contains(4), "Stufe 4 nicht offen: " + agency.progress().openTiers());
        });
    }

    /** Weltraumwetter-Station (19): Die eigene Agentur wird {@code warning_ticks_with_station} vorher gewarnt. */
    public static void weatherStationEarlyWarning(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 2, 4.5));
        MachineBlockEntity be = machine(h, ModBlocks.SPACE_WEATHER_STATION.get(), MachineBlockEntity.class);
        be.setOwner(AgencyManager.ensureFor(p).id());
        long ahead = BalanceManager.get().entry("events", "event_planning").orElseThrow().get("warning_ticks_with_station").getAsLong();
        h.succeedWhen(() -> {
            h.assertTrue(be.status() == MachineStatus.RUNNING, "Station läuft nicht: " + be.status() + "/" + be.statusHint());
            PlanetEvents events = PlanetEvents.get(h.getLevel());
            long start = h.getLevel().getGameTime() + ahead;
            PlanetEvents.Planned planned = new PlanetEvents.Planned(EventService.SOLAR_STORM, start, start + 100, false);
            events.setPlanned(planned);
            try {
                EventService.earlyWarn(h.getLevel(), planned);
                h.assertTrue(WeatherStations.warned(p, start), "keine Frühwarnung");
            } finally {
                events.setPlanned(null);
                WeatherStations.reset(h.getLevel());
            }
            IhTestPlayers.remove(p);
        });
    }

    /** Auf der Erde arbeitet die Station nicht (6.1: überall außer Erde). */
    public static void weatherStationNotOnEarth(GameTestHelper h) {
        MachineBlockEntity be = machine(h, ModBlocks.SPACE_WEATHER_STATION.get(), MachineBlockEntity.class);
        be.setOwner(UUID.randomUUID());
        h.runAfterDelay(10, () -> {
            h.assertTrue(be.status() == MachineStatus.ERROR && be.statusHint() == MachineBlockEntity.HINT_LOCATION,
                    "Status " + be.status() + "/" + be.statusHint());
            h.succeed();
        });
    }
}
