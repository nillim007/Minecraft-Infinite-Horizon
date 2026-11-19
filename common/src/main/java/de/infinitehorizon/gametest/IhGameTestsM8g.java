package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.shipyard.Finale;
import de.infinitehorizon.shipyard.ShipyardState;
import de.infinitehorizon.shipyard.ShipyardTerminalBlockEntity;
import de.infinitehorizon.station.StationData;
import de.infinitehorizon.vehicle.StarshipEntity;
import de.infinitehorizon.vehicle.StarshipService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M8g (21.3): Start nach Abschnitt 8, Erfolg, Sternenschiff, Sprünge zwischen Orbits. */
public final class IhGameTestsM8g {

    private static final BlockPos CORE = new BlockPos(4, 1, 4);
    private static final BlockPos TERMINAL = new BlockPos(2, 1, 2);

    private IhGameTestsM8g() {
    }

    private static ShipyardState.Yard yard(GameTestHelper h) {
        StationData.get(h.getLevel()).put(h.absolutePos(CORE), null);
        h.setBlock(TERMINAL, ModBlocks.SHIPYARD_TERMINAL.get().defaultBlockState());
        return h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).update(h.getLevel());
    }

    /** 21.3: „Start“ erst nach Abschnitt 8; der Start vergibt den Erfolg und setzt das Sternenschiff neben die Station. */
    public static void finaleStart(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 4.5));
        try {
            ShipyardState.Yard y = yard(h);
            BlockPos terminal = h.absolutePos(TERMINAL);
            ShipyardState data = ShipyardState.get(h.getLevel().getServer());
            int sections = ShipyardState.sections().size();
            data.forceBuilt(y, sections - 1);
            h.assertTrue(Finale.start(p, terminal) == Finale.Start.NOT_READY, "Start vor Abschnitt 8");
            y = data.forceBuilt(data.yard(h.getLevel().dimension().identifier(), terminal), sections);
            y = h.getBlockEntity(TERMINAL, ShipyardTerminalBlockEntity.class).update(h.getLevel());
            h.assertTrue(y.notified(), "Fertigmeldung nicht vermerkt");
            h.assertTrue(Finale.start(p, terminal) == Finale.Start.OK, "Start nach Abschnitt 8 abgelehnt");
            h.assertTrue(data.yard(h.getLevel().dimension().identifier(), terminal).launched(), "nicht als gestartet vermerkt");
            h.assertTrue(Finale.start(p, terminal) == Finale.Start.ALREADY, "zweiter Start");
            List<StarshipEntity> ships = h.getLevel().getEntitiesOfClass(StarshipEntity.class, new AABB(terminal).inflate(16));
            h.assertTrue(ships.size() == 1, "Sternenschiffe neben der Station: " + ships.size());
            Identifier adv = Identifier.parse(Finale.rules().get("advancement").getAsString());
            var holder = h.getLevel().getServer().getAdvancements().get(adv);
            h.assertTrue(holder != null && p.getAdvancements().getOrStartProgress(holder).isDone(), "Erfolg " + adv + " fehlt");
            ships.forEach(s -> s.discard());
        } finally {
            IhTestPlayers.remove(p);
            StationData.get(h.getLevel()).remove(h.absolutePos(CORE));
        }
        h.succeed();
    }

    /** 21.3: Sprung nur für die Besatzung, kostet eine gefüllte Antimaterie-Zelle, Ziel nur Orbits. */
    public static void starshipJump(GameTestHelper h) {
        StarshipEntity ship = ModEntities.STARSHIP.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = h.absoluteVec(new Vec3(4.5, 3, 4.5));
        ship.snapTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(ship);
        Identifier here = h.getLevel().dimension().identifier();
        List<Identifier> targets = StarshipService.targets(here);
        h.assertFalse(targets.contains(here) || targets.isEmpty(), "Ziele " + targets);
        h.assertFalse(targets.contains(InfiniteHorizon.id("mars")), "Oberfläche als Ziel");
        Identifier mars = InfiniteHorizon.id("mars_orbit");
        // 4 Besatzungsplätze von Spielern besetzt (Spieler steigen in Reihenfolge ein): der fünfte ist Passagier
        List<ServerPlayer> crew = new java.util.ArrayList<>();
        for (int i = 0; i < ship.crewSeats(); i++) {
            ServerPlayer c = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 2, 4.5));
            c.startRiding(ship, true, false);
            crew.add(c);
        }
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(4.5, 2, 4.5));
        try {
            p.startRiding(ship, true, false);
            h.assertTrue(ship.getPassengers().indexOf(p) == ship.crewSeats(), "Platz " + ship.getPassengers().indexOf(p));
            h.assertTrue(StarshipService.jump(p, ship, mars) == StarshipService.Jump.NOT_CREW, "Passagier darf springen");
            for (ServerPlayer c : crew) {
                c.stopRiding();
                IhTestPlayers.remove(c);
            }
            crew.clear();
            h.assertTrue(ship.isCrew(p), "nicht Besatzung auf Platz 1");
            h.assertTrue(StarshipService.jump(p, ship, mars) == StarshipService.Jump.NO_CELL, "Sprung ohne Zelle");
            ItemStack empty = new ItemStack(ModItems.ANTIMATTER_CELL.get());
            empty.set(ModDataComponents.CHARGE.get(), 0);
            p.getInventory().add(empty);
            h.assertTrue(StarshipService.jump(p, ship, mars) == StarshipService.Jump.NO_CELL, "Sprung mit leerer Zelle");
            h.assertTrue(StarshipService.jump(p, ship, InfiniteHorizon.id("mars")) == StarshipService.Jump.BAD_TARGET, "Oberfläche");
            ItemStack full = new ItemStack(ModItems.ANTIMATTER_CELL.get());
            full.set(ModDataComponents.CHARGE.get(), 1);
            p.getInventory().add(full);
            h.assertTrue(StarshipService.jump(p, ship, mars) == StarshipService.Jump.OK, "Sprung abgelehnt");
            h.assertTrue(p.level().dimension().identifier().equals(mars), "Spieler in " + p.level().dimension().identifier());
            h.assertTrue(p.getVehicle() instanceof StarshipEntity, "Spieler nicht mehr im Schiff");
            int charged = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(ModItems.ANTIMATTER_CELL.get()) && Integer.valueOf(1).equals(s.get(ModDataComponents.CHARGE.get()))) {
                    charged += s.getCount();
                }
            }
            h.assertTrue(charged == 0, "Zelle nicht verbraucht");
            if (p.getVehicle() instanceof StarshipEntity moved) {
                p.stopRiding();
                moved.discard();
            }
        } finally {
            crew.forEach(IhTestPlayers::remove);
            IhTestPlayers.remove(p);
            if (!ship.isRemoved()) {
                ship.discard();
            }
        }
        h.succeed();
    }
}
