package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Spieltest M7 im echten Client (Rundgang aus {@code docs/dev/stand-m7.md}): ein Spieler reist zu allen Zielen von Ring 2,
 * steht dort im Überlebensmodus mit und ohne Ausrüstung, fliegt mit dem Jetpack, fällt unter den Asteroidengürtel, taucht
 * im Ozean der Europa, gleitet auf Titan, lässt sich vom Geysir heben, stirbt und erwacht in der Klon-Kammer und besiegt
 * den Wächter des Senders. Jeder Schritt prüft das Ergebnis und macht ein Bild; alle Fehler werden gesammelt gemeldet.
 */
public class IhClientPlaytestM7 extends IhPlaytestBase {

    @Override
    protected String name() {
        return "playtest_m7";
    }

    @Override
    protected void steps() {
        step("Ziele mit und ohne Ausrüstung", this::arrivals);
        step("Merkur Tagseite", this::mercuryDaySide);
        step("Asteroidengürtel: Jetpack und Fall unter y 0", this::asteroidBelt);
        step("Europa: Tauchen", this::europaDive);
        step("Titan: Gleiten", this::titanGlide);
        step("Enceladus: Geysir", this::enceladusGeyser);
        step("Klon-Kammer", this::cloneChamber);
        step("Pluto: Wächter des Senders", this::plutoSentinel);
    }

    // ------------------------------------------------------------------ Schritte

    /** Punkt 1–8: alle Ziele von Ring 2 mit der vorgesehenen Ausrüstung sicher, ohne Anzug Warnung im HUD. */
    private void arrivals() {
        String t1 = IhGameTestsM7f.T1;
        String t2 = IhGameTestsM7f.T2;
        String t3 = IhGameTestsM7f.T3;
        String shield = IhGameTestsM7f.SHIELD;
        String heating = IhGameTestsM7f.HEATING;
        Object[][] targets = {
                {"mercury", t1, new String[]{shield}}, {"mercury_orbit", t1, new String[0]},
                {"asteroid_belt", t1, new String[0]}, {"ceres", t1, new String[0]},
                {"jupiter_orbit", t1, new String[]{shield}}, {"io", t2, new String[]{shield}},
                {"europa", t1, new String[]{shield}}, {"saturn_orbit", t1, new String[0]},
                {"titan", t1, new String[0]}, {"enceladus", t2, new String[0]},
                {"uranus_orbit", t1, new String[0]}, {"neptune_orbit", t1, new String[0]},
                {"triton", t3, new String[]{heating}}, {"pluto", t2, new String[]{heating}}};
        for (Object[] t : targets) {
            String planet = (String) t[0];
            try {
                go(planet);
                equip((String) t[1], (String[]) t[2]);
                cmd("gamemode survival @p");
                float health = server(ServerPlayer::getHealth);
                context.waitTicks(100);
                String h = hazards();
                boolean alive = server(p -> p.isAlive() && p.getHealth() >= health);
                boolean free = server(p -> !p.level().getBlockState(BlockPos.containing(p.getEyePosition()))
                        .isSuffocating(p.level(), BlockPos.containing(p.getEyePosition())));
                boolean above = server(p -> p.getY() > p.level().getMinY());
                check("[]".equals(h), planet + " mit Ausrüstung gefährlich: " + h);
                check(alive, planet + " mit Ausrüstung Schaden genommen");
                check(free, planet + ": Spieler steckt in einem Block");
                check(above, planet + ": Spieler unter der Welt");
                shot("play_" + planet);
                equip(null);
                context.waitTicks(60);
                check(!"[]".equals(hazards()), planet + " ohne Anzug ungefährlich");
                check(hudMax() > 0, planet + " ohne Anzug keine Warnung im HUD");
            } catch (RuntimeException | AssertionError e) {
                problems.add(planet + ": " + e.getMessage());
            }
            cmd("gamemode creative @p");
            sp.getServer().runOnServer(s -> player(s).setHealth(player(s).getMaxHealth()));
        }
        // Punkt 8: ohne Ausrüstung nach der Aufbauzeit Schaden (einmal, auf Pluto)
        go("pluto");
        equip(null);
        cmd("gamemode survival @p");
        float full = server(ServerPlayer::getHealth);
        context.waitFor(c -> c.player.getHealth() < full || c.player.isDeadOrDying(), 2400);
        shot("play_pluto_unprotected");
        check(context.computeOnClient(c -> c.player.getHealth() < full), "Pluto ohne Anzug: kein Schaden nach 2 Minuten");
        cmd("gamemode creative @p");
        sp.getServer().runOnServer(s -> player(s).setHealth(player(s).getMaxHealth()));
    }

    /** Punkt 1: Merkur-Tagseite (Glutebenen) nur mit Hitzeanzug und Strahlenschild. */
    private void mercuryDaySide() {
        go("mercury", new int[]{3000, 0});
        equip(IhGameTestsM7f.T2, IhGameTestsM7f.SHIELD);
        cmd("gamemode survival @p");
        context.waitTicks(60);
        String h = hazards();
        check("[]".equals(h), "Glutebenen mit Hitzeanzug und Strahlenschild gefährlich: " + h);
        shot("play_mercury_scorched");
        equip(IhGameTestsM7f.T1, IhGameTestsM7f.SHIELD);
        context.waitTicks(20);
        check(!"[]".equals(hazards()), "Glutebenen im Druckanzug ungefährlich");
    }

    /** Punkt 2: Jetpack mit gehaltener Sprungtaste, Fall unter y 0 bringt zurück auf den Spawn-Asteroiden. */
    private void asteroidBelt() {
        go("asteroid_belt");
        equip(IhGameTestsM7f.T1, "jetpack");
        sp.getServer().runOnServer(s -> player(s).getItemBySlot(EquipmentSlot.CHEST).set(ModDataComponents.ENERGY.get(), 1_000_000L));
        cmd("gamemode survival @p");
        context.waitTicks(20);
        double y0 = clientY();
        context.getInput().holdKey(o -> o.keyJump);
        context.waitTicks(30);
        double y1 = clientY();
        shot("play_asteroid_jetpack");
        context.getInput().releaseKey(o -> o.keyJump);
        check(y1 - y0 >= 3, "Jetpack hebt nicht: " + String.format("%.1f → %.1f", y0, y1));
        long energy = server(p -> {
            Long e = p.getItemBySlot(EquipmentSlot.CHEST).get(ModDataComponents.ENERGY.get());
            return e == null ? 0L : e;
        });
        check(energy < 1_000_000L, "Jetpack verbraucht keine Energie");
        // Fall unter y 0
        cmd("execute as @p at @s run tp @s ~ -20 ~");
        context.waitTicks(60);
        double y = server(ServerPlayer::getY);
        boolean slow = server(p -> p.hasEffect(MobEffects.SLOW_FALLING));
        check(y > 0, "Fall unter y 0: Spieler bleibt bei y " + (int) y);
        check(slow, "Fall unter y 0: kein Langsamer Fall");
        shot("play_asteroid_void_reset");
    }

    /** Punkt 3: Im Ozean der Europa unter y 80 schützt nur das Tauchmodul vor dem Druck. */
    private void europaDive() {
        go("europa");
        BlockPos water = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            for (int dx = 0; dx <= 64; dx += 8) {
                for (int dz = 0; dz <= 64; dz += 8) {
                    BlockPos at = new BlockPos(p.getBlockX() + dx, 70, p.getBlockZ() + dz);
                    l.getChunkAt(at);
                    if (l.getFluidState(at).is(net.minecraft.tags.FluidTags.WATER)
                            && l.getFluidState(at.above()).is(net.minecraft.tags.FluidTags.WATER)) {
                        return at;
                    }
                }
            }
            return null;
        });
        if (water == null) {
            throw new AssertionError("kein Ozeanwasser bei y 70 gefunden");
        }
        tpHere(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 0, 0);
        context.waitTicks(20);
        sp.getConnection().waitForChunksRender();
        equip(IhGameTestsM7f.T1, IhGameTestsM7f.SHIELD, IhGameTestsM7f.DIVING);
        cmd("gamemode survival @p");
        cmd("execute as @p at @s run summon infinite_horizon:ice_jelly ~2 ~ ~4");
        context.waitTicks(60);
        String h = hazards();
        check("[]".equals(h), "Tiefsee mit Tauchmodul gefährlich: " + h);
        shot("play_europa_dive");
        equip(IhGameTestsM7f.T1, IhGameTestsM7f.SHIELD);
        context.waitTicks(20);
        check(!"[]".equals(hazards()), "Tiefsee ohne Tauchmodul ungefährlich");
    }

    /** Sinkhöhe beim Gleiten mit Elytren über 30 Ticks (Blick waagerecht). */
    private double glideDrop() {
        cmd("execute as @p at @s run tp @s ~ ~80 ~ 0 0");
        context.waitTicks(5);
        // Sprungtaste in der Luft halten: der Client startet das Gleiten (Vanilla-Eingabe)
        context.getInput().holdKey(o -> o.keyJump);
        context.waitTicks(3);
        context.getInput().releaseKey(o -> o.keyJump);
        context.waitFor(c -> c.player.isFallFlying(), 40);
        context.runOnClient(c -> c.player.setXRot(0));
        double y0 = clientY();
        context.waitTicks(30);
        return y0 - clientY();
    }

    /** Punkt 5: Auf Titan sinkt man beim Gleiten langsamer als auf der Erde. */
    private void titanGlide() {
        go("minecraft:overworld");
        cmd("gamemode survival @p");
        cmd("item replace entity @p armor.chest with minecraft:elytra");
        double earth = glideDrop();
        cmd("gamemode creative @p");
        go("titan");
        equip(IhGameTestsM7f.T1);
        cmd("item replace entity @p armor.chest with minecraft:elytra");
        cmd("gamemode survival @p");
        double titan = glideDrop();
        shot("play_titan_glide");
        boolean flying = context.computeOnClient(c -> c.player.isFallFlying());
        check(flying, "Titan: Gleiten startet nicht");
        check(earth > 0 && titan < earth * 0.8, String.format("Titan sinkt nicht langsamer: Erde %.1f, Titan %.1f Blöcke", earth, titan));
    }

    /** Punkt 5: Der Ammoniak-Geysir hebt den Spieler an. */
    private void enceladusGeyser() {
        go("enceladus");
        equip(IhGameTestsM7f.T2);
        cmd("gamemode survival @p");
        context.waitTicks(20);
        BlockPos geyser = server(p -> {
            BlockPos below = p.blockPosition().below();
            p.level().setBlockAndUpdate(below, ModBlocks.AMMONIA_GEYSER.get().defaultBlockState());
            return below;
        });
        double y0 = clientY();
        sp.getServer().runOnServer(s -> de.infinitehorizon.world.AmmoniaGeyserBlock.erupt(
                (ServerLevel) player(s).level(), geyser, true));
        context.waitTicks(12);
        double y1 = clientY();
        shot("play_enceladus_geyser");
        check(y1 - y0 >= 2, String.format("Geysir hebt nicht: %.1f → %.1f", y0, y1));
    }

    /** Punkt 4: Klon-Kammer mit Kultur und Strom binden, auf Europa sterben, in der Kammer erwachen. */
    private void cloneChamber() {
        go("minecraft:overworld");
        BlockPos chamber = server(p -> {
            BlockPos at = p.blockPosition().east(3);
            p.level().setBlockAndUpdate(at, ModBlocks.CLONE_CHAMBER.get().defaultBlockState());
            p.level().setBlockAndUpdate(at.above(), Blocks.AIR.defaultBlockState());
            p.level().setBlockAndUpdate(at.above(2), Blocks.AIR.defaultBlockState());
            if (p.level().getBlockEntity(at) instanceof de.infinitehorizon.station.CloneChamberBlockEntity be) {
                be.refreshLayout();
                be.setItem(0, new ItemStack(de.infinitehorizon.station.CloneChamberBlockEntity.culture(), 4));
                be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
                de.infinitehorizon.station.CloneChambers.get(p.level().getServer()).bind(p.getUUID(), be.globalPos());
            }
            return at;
        });
        context.waitTicks(10);
        go("europa");
        equip(null);
        cmd("gamemode survival @p");
        cmd("kill @p");
        context.waitFor(c -> c.player.isDeadOrDying(), 40);
        context.waitTicks(10);
        context.runOnClient(c -> c.player.respawn());
        context.waitTicks(10);
        context.setScreen(() -> null);
        context.waitTicks(30);
        sp.getConnection().waitForChunksRender();
        String dim = context.computeOnClient(c -> c.level.dimension().identifier().toString());
        BlockPos at = context.computeOnClient(c -> c.player.blockPosition());
        shot("play_clone_respawn");
        check("minecraft:overworld".equals(dim) && at.closerThan(chamber.above(), 2.5),
                "Klon-Kammer: Spieler erwacht in " + dim + " bei " + at + ", Kammer " + chamber);
    }

    /** Punkt 7: Sender auf Pluto, Wächter beschwören, Phase 2, besiegen, Sternenkarte aufheben. */
    private void plutoSentinel() {
        go("pluto", new int[]{3000, 3000});
        BlockPos origin = server(p -> p.blockPosition());
        sp.getServer().runOnServer(s -> IhGameTestsM4b.placeStructure((ServerLevel) player(s).level(),
                InfiniteHorizon.id("signal_transmitter"), origin));
        context.waitTicks(20);
        BlockPos core = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-48, -40, -48), c.offset(48, 24, 48))) {
                if (l.getBlockState(q).is(ModBlocks.TRANSMITTER_CORE.get())) {
                    return q.immutable();
                }
            }
            return null;
        });
        if (core == null) {
            throw new AssertionError("kein Senderkern in der gesetzten Struktur gefunden");
        }
        equip(IhGameTestsM7f.T2, IhGameTestsM7f.HEATING);
        tpHere(core.getX() + 0.5, core.getY() + 1, core.getZ() + 6.5, 180, 10);
        context.waitTicks(20);
        sp.getConnection().waitForChunksRender();
        cmd("gamemode survival @p");
        Integer sentinelId = server(p -> {
            var s = de.infinitehorizon.world.TransmitterCoreBlock.summon((ServerLevel) p.level(), core, p);
            return s == null ? null : s.getId();
        });
        if (sentinelId == null) {
            throw new AssertionError("Wächter erscheint nicht");
        }
        context.waitTicks(20);
        boolean seen = context.computeOnClient(c -> c.level.getEntity(sentinelId) != null);
        check(seen, "Wächter im Client nicht sichtbar");
        shot("play_pluto_sentinel");
        // Phase 2 unter halber Gesundheit
        sp.getServer().runOnServer(s -> {
            if (player(s).level().getEntity(sentinelId) instanceof de.infinitehorizon.creature.SignalSentinel w) {
                w.setHealth(w.getMaxHealth() * 0.4f);
                w.hurtServer((ServerLevel) w.level(), player(s).damageSources().playerAttack(player(s)), 1);
            }
        });
        context.waitTicks(30);
        int lurkers = server(p -> p.level().getEntitiesOfClass(de.infinitehorizon.creature.FrostLurker.class,
                p.getBoundingBox().inflate(48)).size());
        check(lurkers > 0, "Wächter Phase 2: keine Frostkriecher");
        shot("play_pluto_sentinel_phase2");
        sp.getServer().runOnServer(s -> {
            if (player(s).level().getEntity(sentinelId) instanceof de.infinitehorizon.creature.SignalSentinel w) {
                w.hurtServer((ServerLevel) w.level(), player(s).damageSources().playerAttack(player(s)), Float.MAX_VALUE);
            }
        });
        context.waitTicks(30);
        BlockPos chart = server(p -> p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(64),
                e -> e.getItem().is(ModItems.STAR_CHART.get())).stream().findFirst().map(ItemEntity::blockPosition).orElse(null));
        if (chart == null) {
            throw new AssertionError("Wächter besiegt, aber keine Sternenkarte");
        }
        tpHere(chart.getX() + 0.5, chart.getY(), chart.getZ() + 0.5, 0, 0);
        context.waitTicks(40);
        int charts = context.computeOnClient(c -> c.player.getInventory().countItem(ModItems.STAR_CHART.get()));
        shot("play_pluto_star_chart");
        check(charts == 1, "Sternenkarte nicht aufgehoben (" + charts + ")");
    }
}
