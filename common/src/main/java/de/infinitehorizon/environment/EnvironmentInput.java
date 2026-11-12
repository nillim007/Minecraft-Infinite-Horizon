package de.infinitehorizon.environment;

import de.infinitehorizon.data.Planet;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Eingabe von {@link EnvironmentCalculator#compute} (8.1).
 *
 * @param planet      Planet der Position ({@code null} = keine Daten geladen: gilt wie die Erde ohne Gefahren)
 * @param zones       alle Zonen des Planeten, die die Position enthalten, in Listenreihenfolge (25.1)
 * @param biome       Biom an der Position (für {@code biome_temperature_*}), {@code null} = unbekannt
 * @param dayTime     Tageszeit der Overworld in Ticks
 * @param coverBlocks feste Blöcke senkrecht über dem Spieler (bis zur Weltobergrenze gezählt, höchstens so viele wie nötig)
 * @param solarStorm  Sonnensturm aktiv (Kapitel 19)
 * @param dome        aktive Kuppel an der Position, {@code null} = keine
 * @param protection  Schutz der Ausrüstung
 */
public record EnvironmentInput(@Nullable Planet planet, List<Planet.Zone> zones, @Nullable Identifier biome, long dayTime,
                               int coverBlocks, boolean solarStorm, @Nullable DomeCover dome, Protection protection) {

    public EnvironmentInput {
        zones = List.copyOf(zones);
    }

    /**
     * Eine aktive Kuppel (auch {@code failing}, 10.1) über der Position.
     *
     * @param tier        Stufe der Kuppel
     * @param temperature Innentemperatur, wenn die Stufe zum Planeten passt; {@code null} = Kuppel regelt nicht
     */
    public record DomeCover(int tier, @Nullable Double temperature) {
    }
}
