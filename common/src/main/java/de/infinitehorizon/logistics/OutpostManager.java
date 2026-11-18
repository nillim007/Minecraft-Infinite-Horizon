package de.infinitehorizon.logistics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Außenposten (20.1): Produzenten im Umkreis eines {@code cargo_terminal} geben direkt ins nächstgelegene Terminal.
 * Ihr Zustand steht in {@link OutpostData}; nach dem Laden wird die Zeit ohne geladenen Chunk mit
 * {@link OfflineProduction} nachgerechnet.
 */
public final class OutpostManager {

    /** Takt, in dem Produzenten ihren Schnappschuss schreiben und ihr Terminal suchen (technisch, keine Spielregel). */
    public static final int SNAPSHOT_INTERVAL = 20;

    private static @Nullable BalanceManager cachedFor;
    private static Set<String> producers = Set.of();

    private OutpostManager() {
    }

    private static JsonObject entry(String key) {
        return BalanceManager.get().entry("logistics", key).orElse(new JsonObject());
    }

    public static int radius() {
        JsonObject o = entry("outpost");
        return o.has("radius") ? o.get("radius").getAsInt() : 0;
    }

    /** Gehört die Maschine zu den Produzenten eines Außenpostens ({@code outpost.producers})? */
    public static boolean isProducer(String machineId) {
        BalanceManager b = BalanceManager.get();
        if (b != cachedFor) {
            Set<String> s = new HashSet<>();
            JsonObject o = entry("outpost");
            if (o.has("producers")) {
                for (JsonElement e : o.getAsJsonArray("producers")) {
                    s.add(e.getAsString());
                }
            }
            producers = Set.copyOf(s);
            cachedFor = b;
        }
        return producers.contains(machineId);
    }

    /** Einstellung {@code outposts_offline}. */
    public static boolean offlineEnabled() {
        JsonObject o = entry("offline_production");
        return !o.has("enabled_config_key") || ServerConfig.get().getBoolean(o.get("enabled_config_key").getAsString());
    }

    /** Einstellung {@code outpost_offline_factor}, sonst {@code factor} aus der Balance. */
    public static double factor() {
        JsonObject o = entry("offline_production");
        if (o.has("factor_config_key")) {
            return ServerConfig.get().getDouble(o.get("factor_config_key").getAsString());
        }
        return o.has("factor") ? o.get("factor").getAsDouble() : 0;
    }

    static Identifier dimension(ServerLevel level) {
        return level.dimension().identifier();
    }

    /** Overworld-Spielzeit (gemeinsam für alle Dimensionen). */
    public static long now(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    public static OutpostData data(ServerLevel level) {
        return OutpostData.get(level.getServer());
    }

    public static void registerTerminal(ServerLevel level, BlockPos pos, java.util.@Nullable UUID owner) {
        data(level).addTerminal(dimension(level), pos, owner);
    }

    /** Block abgebaut: Terminal bzw. Produzent aus dem Register nehmen. */
    public static void onRemoved(ServerLevel level, BlockPos pos) {
        data(level).remove(dimension(level), pos);
    }

    /** Nächstgelegenes Terminal im Umkreis (auch nicht geladen) oder null. */
    public static @Nullable BlockPos assignedTerminal(ServerLevel level, BlockPos producer) {
        OutpostData d = data(level);
        while (true) {
            BlockPos t = d.nearestTerminal(dimension(level), producer, radius());
            // veralteter Eintrag (Block ohne Nebenwirkung entfernt): aus dem Register nehmen
            if (t != null && level.isLoaded(t) && !(level.getBlockEntity(t) instanceof CargoTerminalBlockEntity)) {
                d.remove(dimension(level), t);
                continue;
            }
            return t;
        }
    }

    /** Terminal an {@code pos}, wenn sein Chunk geladen ist. */
    public static @Nullable CargoTerminalBlockEntity loadedTerminal(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof CargoTerminalBlockEntity t ? t : null;
    }

    /** Schnappschuss des Produzenten schreiben (oder löschen, wenn er zu keinem Terminal gehört). */
    public static void snapshot(ServerLevel level, ProcessingMachineBlockEntity p, boolean assigned) {
        OutpostData d = data(level);
        if (!assigned) {
            if (d.producer(dimension(level), p.getBlockPos()) != null) {
                d.remove(dimension(level), p.getBlockPos());
            }
            return;
        }
        OfflineProduction.Snapshot s = p.offlineSnapshot(level);
        d.putProducer(new OutpostData.Producer(dimension(level), p.getBlockPos().immutable(), p.machineId(),
                s.running() && p.status() == MachineStatus.RUNNING, s.outputPerCycle(), s.cycleTicks(), now(level),
                p.outpostToken()));
    }

    /**
     * Rechnet die Zeit seit dem letzten Schnappschuss nach (20.1): Vorgänge = {@link OfflineProduction#cycles}, die
     * Ausbeute geht ins Terminal (wenn geladen) oder in die eigenen Ausgaben; Eingaben und Platz begrenzen.
     * Gibt die Zahl der Vorgänge zurück und setzt den letzten Tick auf jetzt.
     */
    public static long catchUp(ServerLevel level, ProcessingMachineBlockEntity p) {
        OutpostData d = data(level);
        OutpostData.Producer snap = d.producer(dimension(level), p.getBlockPos());
        BlockPos terminal = assignedTerminal(level, p.getBlockPos());
        // anderer Block an derselben Stelle (Token passt nicht): nichts nachrechnen
        if (snap == null || terminal == null || snap.token() != p.outpostToken()) {
            return 0;
        }
        long now = now(level);
        long elapsed = now - snap.lastTick();
        long done = 0;
        if (offlineEnabled() && elapsed > 0) {
            long cycles = OfflineProduction.cycles(snap.snapshot(), factor(), elapsed);
            if (cycles > 0) {
                done = p.produceOffline(level, cycles, loadedTerminal(level, terminal));
            }
        }
        d.putProducer(new OutpostData.Producer(snap.dimension(), snap.pos(), snap.machine(), snap.running(),
                snap.outputPerCycle(), snap.cycleTicks(), now, snap.token()));
        return done;
    }

    /**
     * Rechnet alle Produzenten des Außenpostens nach (20.1: Logistik-Abfrage, Frachtlandung). Ihre Chunks werden dazu
     * geladen; Produzenten mit frischem Schnappschuss (laufen gerade) bleiben unberührt.
     */
    public static void catchUpOutpost(ServerLevel level, BlockPos terminal) {
        long now = now(level);
        for (OutpostData.Producer p : data(level).producersOf(dimension(level), terminal, radius())) {
            if (now - p.lastTick() <= 2L * SNAPSHOT_INTERVAL) {
                continue;
            }
            level.getChunkAt(p.pos());
            if (level.getBlockEntity(p.pos()) instanceof ProcessingMachineBlockEntity pm) {
                catchUp(level, pm);
            }
        }
    }

    /**
     * Zustand eines Produzenten (nicht gespeichert): Nachrechnen beim ersten Tick nach dem Laden, Schnappschuss im Takt,
     * Abgabe ans Terminal.
     */
    public static final class Link {
        private boolean caughtUp;
        private boolean assigned;
        private int waited;
        private @Nullable BlockPos terminal;

        public void tick(ServerLevel level, ProcessingMachineBlockEntity p) {
            BlockPos pos = p.getBlockPos();
            long now = now(level);
            if (!caughtUp) {
                OutpostData.Producer snap = data(level).producer(dimension(level), pos);
                if (snap != null && snap.token() == p.outpostToken() && now - snap.lastTick() > 2L * SNAPSHOT_INTERVAL) {
                    // Terminal im Nachbar-Chunk: kurz warten, bis es mitgeladen ist
                    if (loadedTerminal(level, assignedTerminal(level, pos)) == null && waited++ < SNAPSHOT_INTERVAL) {
                        return;
                    }
                    catchUp(level, p);
                }
                caughtUp = true;
            }
            if (!assigned || Math.floorMod(now + pos.asLong(), SNAPSHOT_INTERVAL) == 0) {
                terminal = assignedTerminal(level, pos);
                assigned = true;
                snapshot(level, p, terminal != null);
            }
            CargoTerminalBlockEntity sink = loadedTerminal(level, terminal);
            if (sink != null) {
                p.deliverTo(sink);
            }
        }

        /** Für Tests und nach dem Nachrechnen von außen: beim nächsten Tick neu prüfen. */
        public void reset() {
            caughtUp = false;
            assigned = false;
            waited = 0;
        }
    }
}
