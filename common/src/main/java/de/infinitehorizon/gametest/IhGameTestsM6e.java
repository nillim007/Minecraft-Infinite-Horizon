package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.FlightService;
import de.infinitehorizon.space.FuelCalculator;
import de.infinitehorizon.space.LanderEntity;
import de.infinitehorizon.space.LanderRoutes;
import de.infinitehorizon.space.LaunchChecklist;
import de.infinitehorizon.space.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M6e (12.7): Landefähre. */
public final class IhGameTestsM6e {

    private static final Identifier MOON = InfiniteHorizon.id("moon");
    private static final Identifier MARS = InfiniteHorizon.id("mars");
    private static final BlockPos FLOOR = new BlockPos(3, 1, 3);

    private IhGameTestsM6e() {
    }

    private static ServerPlayer suited(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PRESSURE_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PRESSURE_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PRESSURE_SUIT_BOOTS.get()));
        return p;
    }

    private static boolean useLander(GameTestHelper h, ServerPlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LANDER.get()));
        BlockPos abs = h.absolutePos(FLOOR);
        return p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false))).consumesAction();
    }

    private static List<LanderEntity> landers(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(LanderEntity.class, new AABB(h.absolutePos(FLOOR)).inflate(3));
    }

    /** Auf der Erde lässt sich die Fähre nicht absetzen, und von der Erde gibt es keine Ziele (12.7). */
    public static void landerNotOnEarth(GameTestHelper h) {
        h.setBlock(FLOOR, Blocks.STONE);
        ServerPlayer p = suited(h);
        try {
            h.assertFalse(useLander(h, p) && !landers(h).isEmpty(), "Fähre auf der Erde abgesetzt");
            h.assertTrue(landers(h).isEmpty(), "Fähre auf der Erde");
            Agency agency = AgencyManager.ensureFor(p);
            List<LaunchChecklist.Destination> d = LanderRoutes.destinations(h.getLevel(), Long.MAX_VALUE,
                    PlanetRegistry.get().planet(PlanetResolver.EARTH).orElseThrow(), agency);
            h.assertTrue(d.isEmpty(), "Ziele von der Erde: " + d);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /**
     * Im Erdorbit (12.7): Fähre absetzen, betanken, Ziele = Körper unter dem Orbit (Erde, Mond, nicht Mars), Flug zum Mond
     * mit Kosten ohne Transfer; Zerlegen gibt das Item zurück.
     */
    public static void landerOrbitToMoon(GameTestHelper h) {
        h.setBlock(FLOOR, Blocks.STONE);
        ServerPlayer p = suited(h);
        Agency agency = AgencyManager.ensureFor(p);
        agency.progress().addProbeReached(MOON);
        agency.progress().addProbeReached(MARS);
        h.assertTrue(useLander(h, p), "Fähre nicht abgesetzt");
        List<LanderEntity> found = landers(h);
        h.assertTrue(found.size() == 1, "Fähren: " + found.size());
        LanderEntity lander = found.getFirst();
        int seats = LanderRoutes.rules().get("seats").getAsInt();
        h.assertTrue(lander.seats() == seats && lander.capacity() == LanderRoutes.rules().get("tank_per_component").getAsLong(),
                "Sitze " + lander.seats() + ", Tank " + lander.capacity());
        lander.refuel(0, lander.capacity());
        lander.refuel(1, lander.capacity());
        List<LaunchChecklist.Destination> dests = FlightService.destinationsFor(h.getLevel(), lander, agency);
        List<Identifier> ids = dests.stream().map(LaunchChecklist.Destination::dimension).toList();
        h.assertTrue(ids.contains(MOON) && ids.contains(PlanetResolver.EARTH) && !ids.contains(MARS), "Ziele " + ids);
        p.startRiding(lander, true, true);
        String error = FlightService.launchFromRocket(h.getLevel(), lander, p, MOON, FlightService.Landing.DEFAULT);
        h.assertTrue(error == null, "Start abgelehnt: " + error);
        RocketEntity r = FlightService.complete((RocketEntity) p.getVehicle());
        h.assertTrue(r instanceof LanderEntity && r.level().dimension().identifier().equals(MOON),
                "nicht auf dem Mond: " + r + " in " + r.level().dimension());
        PlanetRegistry planets = PlanetRegistry.get();
        long cost = FuelCalculator.landerCost(FuelCalculator.Body.of(planets.planet(InfiniteHorizon.id("earth_orbit")).orElseThrow()),
                FuelCalculator.Body.of(planets.planet(MOON).orElseThrow()), FuelCalculator.Constants.from(BalanceManager.get()));
        h.assertTrue(r.fuel(0) == r.capacity() - cost && r.fuel(1) == r.capacity() - cost,
                "Treibstoff " + r.fuel(0) + ", erwartet " + (r.capacity() - cost));
        List<ItemStack> drops = FlightService.disassemble((ServerLevel) r.level(), r);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.LANDER.get()), "Zerlegen: " + drops);
        IhTestPlayers.remove(p);
        h.succeed();
    }
}
