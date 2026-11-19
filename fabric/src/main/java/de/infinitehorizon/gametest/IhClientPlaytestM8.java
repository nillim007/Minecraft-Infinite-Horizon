package de.infinitehorizon.gametest;

import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.machine.DrillingRigBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.shipyard.Finale;
import de.infinitehorizon.shipyard.ShipyardDockBlockEntity;
import de.infinitehorizon.shipyard.ShipyardMath;
import de.infinitehorizon.shipyard.ShipyardState;
import de.infinitehorizon.shipyard.ShipyardTerminalBlockEntity;
import de.infinitehorizon.station.StationData;
import de.infinitehorizon.vehicle.StarshipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Spieltest M8 im echten Client: Außenposten auf dem Mars mit Bohrturm und Frachtterminal, Offline-Produktion nach
 * einem Ausflug in eine andere Dimension, eine Route über das GUI des Logistik-Computers samt Frachtflug, die
 * Orbitalwerft mit „Start“, das Finale und ein Sprung mit dem Sternenschiff (Einsteigen und Inventartaste).
 */
public class IhClientPlaytestM8 extends IhPlaytestBase {

    private BlockPos terminalA;
    private BlockPos terminalB;
    private BlockPos rig;

    @Override
    protected String name() {
        return "playtest_m8";
    }

    @Override
    protected void steps() {
        step("Außenposten und Offline-Produktion", this::outpost);
        step("Logistik-Computer über das GUI", this::logistics);
        step("Werft, Start und Finale", this::shipyard);
        step("Sternenschiff: Einsteigen und Sprung", this::starship);
    }

    private int total(BlockPos terminal) {
        return server(p -> {
            if (!(p.level().getBlockEntity(terminal) instanceof CargoTerminalBlockEntity t)) {
                return -1;
            }
            int n = 0;
            for (int i = 0; i < t.storageSlots(); i++) {
                n += t.getItem(i).getCount();
            }
            return n;
        });
    }

    private void fillRig() {
        sp.getServer().runOnServer(s -> {
            if (player(s).level().getBlockEntity(rig) instanceof DrillingRigBlockEntity r) {
                r.energyStorage().setAmountUnchecked(r.energyStorage().getCapacity());
            }
        });
    }

    /** Wartet {@code ticks} und hält den Bohrturm unter Strom. */
    private void runRig(int ticks) {
        for (int t = 0; t < ticks; t += 20) {
            fillRig();
            context.waitTicks(20);
        }
    }

    /** Bohrturm über Eisenerz und Frachtterminal; nach einem Ausflug hat das Terminal die Offline-Ausbeute. */
    private void outpost() {
        go("mars");
        BlockPos[] at = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            BlockPos base = p.blockPosition();
            BlockPos t = base.east(3);
            BlockPos r = base.east(6);
            for (int k = 1; k <= 10; k++) {
                l.setBlockAndUpdate(r.below(k), Blocks.IRON_ORE.defaultBlockState());
            }
            l.setBlockAndUpdate(t, ModBlocks.CARGO_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            l.setBlockAndUpdate(r, ModBlocks.DRILLING_RIG.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            return new BlockPos[]{t, r};
        });
        terminalA = at[0];
        rig = at[1];
        runRig(260);
        int live = total(terminalA);
        check(live > 0, "Bohrturm liefert nicht ins Terminal (" + live + ")");
        shot("play8_outpost");
        // Ausflug: der Mars-Chunk tickt nicht mehr (oder wird entladen)
        go("minecraft:overworld");
        context.waitTicks(600);
        go("mars");
        int back = total(terminalA);
        check(back - live >= 2, "keine Offline-Ausbeute nach dem Ausflug: " + live + " → " + back);
        shot("play8_outpost_offline");
    }

    /** Route über die Knöpfe des Logistik-Computers anlegen; die Rakete fliegt die Fracht zum zweiten Terminal. */
    private void logistics() {
        if (terminalA == null) {
            throw new AssertionError("kein Außenposten aus dem vorigen Schritt");
        }
        BlockPos computer = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            terminalB = terminalA.south(5);
            l.setBlockAndUpdate(terminalB, ModBlocks.CARGO_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            if (l.getBlockEntity(terminalA) instanceof CargoTerminalBlockEntity a) {
                ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
                CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
                a.setItem(a.layout().moldStart(), rocket);
            }
            BlockPos c = p.blockPosition().west(2);
            l.setBlockAndUpdate(c, ModBlocks.LOGISTICS_COMPUTER.get().defaultBlockState());
            return c;
        });
        context.waitTicks(25); // Terminals melden sich an
        tpHere(computer.getX() + 2.5, computer.getY(), computer.getZ() + 0.5, 90, 0);
        context.waitTicks(5);
        context.getInput().lookAt(computer);
        context.waitTick();
        context.getInput().pressKey(o -> o.keyUse);
        context.waitForScreen(de.infinitehorizon.client.LogisticsScreen.class);
        context.waitTicks(5);
        check(click(Component.translatable("gui.infinite_horizon.logistics.new")), "Knopf „Neu“ fehlt");
        String a = " " + terminalA.getX() + " " + terminalA.getZ();
        String b = " " + terminalB.getX() + " " + terminalB.getZ();
        cycleUntil("gui.infinite_horizon.logistics.start", a);
        cycleUntil("gui.infinite_horizon.logistics.target", b);
        String every = context.computeOnClient(c -> Component.translatable("gui.infinite_horizon.logistics.condition.every_n_ticks").getString());
        for (int i = 0; i < 4; i++) {
            String cond = buttonText("gui.infinite_horizon.logistics.condition.");
            if (cond.isEmpty() || cond.equals(every)) {
                break;
            }
            clickWhere(m -> m.equals(cond));
        }
        check(buttonText("gui.infinite_horizon.logistics.condition.").equals(every), "Bedingung lässt sich nicht umschalten");
        check(clickWhere(m -> m.equals("1")), "Knopf für Rakete 1 fehlt");
        check(click(Component.translatable("gui.infinite_horizon.logistics.save")), "Knopf „Speichern“ fehlt");
        context.waitTicks(5);
        Identifier mars = Identifier.parse("infinite_horizon:mars");
        boolean saved = sp.getServer().computeOnServer(s -> CargoFlights.get(s).routes().stream().anyMatch(r ->
                r.home().equals(terminalA) && r.target().equals(terminalB) && r.homeDimension().equals(mars) && r.rockets().contains(0)));
        check(saved, "Route aus dem GUI nicht gespeichert");
        sp.getServer().runOnServer(CargoFlights::process);
        context.waitTicks(45); // Statusliste aktualisiert sich
        shot("play8_logistics");
        context.setScreen(() -> null);
        boolean flying = sp.getServer().computeOnServer(s -> CargoFlights.get(s).flightOf(mars, terminalA, 0).isPresent());
        check(flying, "Rakete nicht gestartet");
        sp.getServer().runOnServer(s -> CargoFlights.get(s).flightOf(mars, terminalA, 0).ifPresent(f -> {
            CargoFlights.get(s).arriveNow(f.id(), s.overworld().getGameTime());
            CargoFlights.process(s);
        }));
        context.waitTicks(5);
        check(total(terminalB) > 0, "Fracht nicht am Ziel angekommen");
        sp.getServer().runOnServer(s -> CargoFlights.get(s).routes().forEach(r -> CargoFlights.get(s).removeRoute(r.id())));
    }

    /** Text des sichtbaren Knopfs, dessen Text mit der Übersetzung von {@code keyPrefix…} übereinstimmt (für Wechselknöpfe). */
    private String buttonText(String keyPrefix) {
        return context.computeOnClient(c -> {
            var screen = c.gui.screen();
            if (screen == null) {
                return "";
            }
            for (var e : screen.children()) {
                if (e instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                    String m = w.getMessage().getString();
                    for (String id : List.of("when_full", "every_n_ticks", "when_target_below", "until_shipyard_demand_met")) {
                        if (keyPrefix.endsWith("condition.") && m.equals(Component.translatable(keyPrefix + id).getString())) {
                            return m;
                        }
                    }
                }
            }
            return "";
        });
    }

    /** Wechselknopf (Start, Ziel) drücken, bis er {@code want} zeigt. */
    private void cycleUntil(String key, String want) {
        String label = context.computeOnClient(c -> Component.translatable(key, "").getString());
        for (int i = 0; i < 8; i++) {
            String current = context.computeOnClient(c -> {
                for (var e : c.gui.screen().children()) {
                    if (e instanceof net.minecraft.client.gui.components.AbstractWidget w && w.getMessage().getString().startsWith(label)) {
                        return w.getMessage().getString();
                    }
                }
                return "";
            });
            if (current.endsWith(want)) {
                return;
            }
            if (current.isEmpty()) {
                break;
            }
            clickWhere(m -> m.equals(current));
        }
        check(false, "Knopf " + key + " zeigt nie" + want);
    }

    /** Werft im Erdorbit: Abschnitt 1 im Bau, dann alle fertig, „Start“ im GUI, Finale bis zum Ende. */
    private void shipyard() {
        go("earth_orbit");
        BlockPos terminal = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            BlockPos base = p.blockPosition();
            StationData.get(l).put(base.below(2), null);
            BlockPos t = base.east(3);
            l.setBlockAndUpdate(t.below(), ModBlocks.STATION_HULL.get().defaultBlockState());
            l.setBlockAndUpdate(t, ModBlocks.SHIPYARD_TERMINAL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            BlockPos d = t.south(2);
            l.setBlockAndUpdate(d, ModBlocks.SHIPYARD_DOCK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            if (l.getBlockEntity(t) instanceof ShipyardTerminalBlockEntity te) {
                te.claimFor(p);
                te.update(l);
            }
            if (l.getBlockEntity(d) instanceof ShipyardDockBlockEntity dock) {
                dock.refreshLayout();
                for (ShipyardMath.Material m : ShipyardState.sections().getFirst().materials()) {
                    dock.insertItem(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(m.id())), (int) m.amount()), false);
                }
            }
            return t;
        });
        tpHere(terminal.getX() - 2.5, terminal.getY(), terminal.getZ() + 0.5, -90, 0);
        context.waitTicks(25); // das Dock meldet sich im Takt von 1 s an
        context.getInput().lookAt(terminal);
        context.waitTick();
        context.getInput().pressKey(o -> o.keyUse);
        context.waitForScreen(de.infinitehorizon.client.ShipyardScreen.class);
        context.waitTicks(5);
        var st = context.computeOnClient(c -> ((de.infinitehorizon.client.ShipyardScreen) c.gui.screen()).state());
        check(st.valid() && st.hasDock(), "Werft ungültig oder ohne Dock: " + st.valid() + "/" + st.hasDock());
        check(st.remaining() > 0, "Abschnitt 1 baut nicht");
        shot("play8_shipyard");
        sp.getServer().runOnServer(s -> {
            ServerLevel l = (ServerLevel) player(s).level();
            ShipyardState data = ShipyardState.get(s);
            data.forceBuilt(data.yard(l.dimension().identifier(), terminal), ShipyardState.sections().size());
            if (l.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity te) {
                te.update(l);
            }
        });
        context.waitTicks(45); // GUI holt den neuen Stand, „Start“ erscheint
        shot("play8_shipyard_complete");
        check(click(Component.translatable("gui.infinite_horizon.shipyard.start")), "Knopf „Start“ fehlt");
        context.waitForScreen(de.infinitehorizon.client.FinaleScreen.class);
        context.waitTicks(60);
        shot("play8_finale");
        int end = context.computeOnClient(c -> {
            var f = (de.infinitehorizon.client.FinaleScreen) c.gui.screen();
            return f.sequenceTicks() + de.infinitehorizon.client.FinaleScreen.replyTicks() + 2000;
        });
        context.runOnClient(c -> ((de.infinitehorizon.client.FinaleScreen) c.gui.screen()).skipTo(end - 1000));
        context.waitFor(c -> !(c.gui.screen() instanceof de.infinitehorizon.client.FinaleScreen), 1200);
        Identifier adv = Identifier.parse(Finale.rules().get("advancement").getAsString());
        boolean done = server(p -> {
            var h = p.level().getServer().getAdvancements().get(adv);
            return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
        });
        check(done, "Erfolg " + adv + " nicht vergeben");
        int ships = server(p -> p.level().getEntitiesOfClass(StarshipEntity.class, new AABB(terminal).inflate(16)).size());
        check(ships == 1, "Sternenschiffe neben der Station: " + ships);
    }

    /** Einsteigen per Rechtsklick, Inventartaste, Sprungziel Mars-Orbit; Spieler und Schiff kommen zusammen an. */
    private void starship() {
        double[] ship = server(p -> p.level().getEntitiesOfClass(StarshipEntity.class, p.getBoundingBox().inflate(32)).stream()
                .findFirst().map(s -> new double[]{s.getX(), s.getY(), s.getZ()}).orElse(null));
        if (ship == null) {
            throw new AssertionError("kein Sternenschiff in der Nähe");
        }
        cmd("gamemode survival @p");
        cmd("give @p infinite_horizon:antimatter_cell[infinite_horizon:charge=1]");
        tpHere(ship[0] - 3.5, ship[1], ship[2], -90, 10);
        context.waitTicks(10);
        context.getInput().lookAt(BlockPos.containing(ship[0], ship[1] + 1, ship[2]));
        context.waitTick();
        context.getInput().pressKey(o -> o.keyUse);
        context.waitTicks(10);
        boolean riding = context.computeOnClient(c -> c.player.getVehicle() instanceof StarshipEntity);
        check(riding, "Einsteigen per Rechtsklick klappt nicht");
        shot("play8_starship_seat");
        context.getInput().pressKey(o -> o.keyInventory);
        context.waitForScreen(de.infinitehorizon.client.StarshipJumpScreen.class);
        context.waitTicks(5);
        shot("play8_starship_jump");
        check(click(Component.translatable("planet.infinite_horizon.mars_orbit")), "Ziel Mars-Orbit fehlt");
        context.waitTicks(60);
        sp.getConnection().waitForChunksRender();
        String dim = context.computeOnClient(c -> c.level.dimension().identifier().toString());
        boolean still = context.computeOnClient(c -> c.player.getVehicle() instanceof StarshipEntity);
        check("infinite_horizon:mars_orbit".equals(dim), "Sprung landet in " + dim);
        check(still, "nach dem Sprung nicht mehr im Sternenschiff");
        int cells = context.computeOnClient(c -> c.player.getInventory().countItem(ModItems.ANTIMATTER_CELL.get()));
        check(cells == 0, "Antimaterie-Zelle nicht verbraucht (" + cells + ")");
        shot("play8_mars_orbit");
    }
}
