package de.infinitehorizon.shipyard;

import com.google.gson.JsonObject;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.machine.MachineLocation;
import de.infinitehorizon.network.ShipyardStatePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.station.StationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orbitalwerft (21.1): Ortsregeln (Terminal nur in einer Station im Erdorbit, Dock in einer Station), Zuordnung
 * Dock → Werft derselben Station und der Zustand für das Werft-GUI.
 */
public final class ShipyardService {

    private ShipyardService() {
    }

    /** Stationskern im Umkreis {@code station_core_radius} oder null. */
    public static @Nullable BlockPos core(ServerLevel level, BlockPos pos) {
        StationData.Core c = StationData.get(level).nearest(pos, ShipyardState.stationRadius());
        return c == null ? null : c.pos();
    }

    /** Darf hier ein Werft-Terminal stehen ({@code terminal_location}, Station)? */
    public static boolean terminalAllowed(ServerLevel level, BlockPos pos) {
        JsonObject r = ShipyardState.rules();
        JsonObject loc = new JsonObject();
        if (r.has("terminal_location")) {
            loc.add("location", r.get("terminal_location"));
        }
        boolean station = !r.has("terminal_requires_station") || !r.get("terminal_requires_station").getAsBoolean()
                || core(level, pos) != null;
        return MachineLocation.allowed(loc, level, pos) && station;
    }

    /** Werft derselben Station wie das Dock (gleicher Stationskern); bei mehreren die nächstgelegene, sonst null. */
    public static ShipyardState.@Nullable Yard yardFor(ServerLevel level, BlockPos dock) {
        BlockPos c = core(level, dock);
        if (c == null) {
            return null;
        }
        ShipyardState.Yard best = null;
        for (ShipyardState.Yard y : ShipyardState.get(level.getServer()).yards()) {
            if (y.dimension().equals(level.dimension().identifier()) && c.equals(core(level, y.pos()))
                    && (best == null || y.pos().distSqr(dock) < best.pos().distSqr(dock))) {
                best = y;
            }
        }
        return best;
    }

    /** Hat die Werft mindestens ein Dock in derselben Station (21.1)? */
    public static boolean hasDock(ServerLevel level, ShipyardState.Yard y) {
        for (de.infinitehorizon.logistics.OutpostData.Terminal d : de.infinitehorizon.logistics.OutpostData
                .get(level.getServer()).docks()) {
            if (d.dimension().equals(y.dimension()) && y.pos().equals(posOf(yardFor(level, d.pos())))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable BlockPos posOf(ShipyardState.@Nullable Yard y) {
        return y == null ? null : y.pos();
    }

    /** Darf die Werft eine Sternenkarte dieser Agentur annehmen (nur die eigene, 18)? */
    public static boolean chartAllowed(ServerLevel level, ShipyardState.Yard y, @Nullable UUID chartAgency) {
        if (chartAgency == null) {
            return false;
        }
        if (y.owner().isEmpty()) {
            return true;
        }
        AgencyData data = AgencyData.get(level.getServer());
        return data.resolve(y.owner().get()).equals(data.resolve(chartAgency));
    }

    /** Zustand für das GUI: Abschnitte mit Materialien, geliefert und benötigt, Baufortschritt. */
    public static ShipyardStatePayload state(ServerLevel level, BlockPos terminal) {
        ShipyardState data = ShipyardState.get(level.getServer());
        ShipyardState.Yard y = data.yard(level.dimension().identifier(), terminal);
        long now = level.getServer().overworld().getGameTime();
        List<ShipyardStatePayload.Section> sections = new ArrayList<>();
        List<ShipyardMath.Section> secs = ShipyardState.sections();
        ShipyardMath.State st = y == null ? ShipyardMath.State.empty() : data.advance(y, now).state();
        for (int i = 0; i < secs.size(); i++) {
            List<ShipyardStatePayload.Material> mats = new ArrayList<>();
            for (ShipyardMath.Material m : secs.get(i).materials()) {
                long have = i < st.built() ? m.amount() : st.delivered(i, m.id());
                mats.add(new ShipyardStatePayload.Material(m.id(), m.amount(), Math.min(m.amount(), have), m.fluid()));
            }
            sections.add(new ShipyardStatePayload.Section(mats));
        }
        boolean valid = y != null && terminalAllowed(level, terminal);
        return new ShipyardStatePayload(terminal, st.built(), st.buildEnd() > 0 ? Math.max(0, st.buildEnd() - now) : 0,
                sections, valid, y != null && hasDock(level, y));
    }

    public static void send(ServerPlayer player, BlockPos terminal) {
        Services.NETWORK.sendToPlayer(player, state((ServerLevel) player.level(), terminal));
    }
}
