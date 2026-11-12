package de.infinitehorizon.environment;

import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Planet und Zonen an einer Position (3A.3, 8.1): die einzige Quelle für Umwelt, Schwerkraft, Kuppeltemperatur, 8.5
 * und Ortsbindung. Im GameTest-Betrieb gilt zuerst {@link EnvironmentOverrides}. Dimensionen ohne Planeten-Eintrag
 * (Nether, End, andere Mods) gelten wie die Erde.
 */
public final class PlanetResolver {

    /** Umkreis von {@code near_block} in Blöcken (25.1). SPEC-GAP #42: steht nur im Text, nicht in spec/data. */
    public static final int NEAR_BLOCK_RADIUS = 4;

    /** Planeten-Eintrag der Erde; gilt für Dimensionen ohne eigenen Eintrag. */
    public static final Identifier EARTH = Identifier.withDefaultNamespace("overworld");

    /** Ergebnis: Planet ({@code null} = keine Daten), passende Zonen, Biom, Ortsbindung aus dem Override. */
    public record Resolved(@Nullable Planet planet, List<Planet.Zone> zones, @Nullable Identifier biome,
                           @Nullable String binding) {
    }

    private PlanetResolver() {
    }

    public static Resolved at(ServerLevel level, BlockPos pos) {
        PlanetRegistry registry = PlanetRegistry.get();
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Identifier biome = biomeHolder.unwrapKey().map(k -> k.identifier()).orElse(null);
        Optional<EnvironmentOverrides.Entry> override = EnvironmentOverrides.at(level, pos);
        Planet planet;
        String forcedZone = null;
        String binding = null;
        if (override.isPresent()) {
            planet = registry.planet(override.get().planet()).orElse(null);
            forcedZone = override.get().zone();
            binding = override.get().binding();
        } else {
            planet = planetFor(level.dimension().identifier(), registry);
        }
        List<Planet.Zone> zones = planet == null ? List.of()
                : zonesAt(planet, pos.getY(), forcedZone, id -> biomeHolder.is(id), id -> nearBlock(level, pos, id));
        return new Resolved(planet, zones, biome, binding);
    }

    /** Planet einer Dimension; ohne eigenen Eintrag gilt der Eintrag der Overworld (8.1). */
    public static @Nullable Planet planetFor(Identifier dimension, PlanetRegistry registry) {
        return registry.planet(dimension).or(() -> registry.planet(EARTH)).orElse(null);
    }

    /**
     * Zonen an einer Höhe (25.1): y-Bereich, optional Biom und Nähe zu einem Block. Ist {@code forcedZone} gesetzt
     * (Override), gilt nur die Zone mit dieser ID, ohne Biom- und Blockprüfung.
     */
    public static List<Planet.Zone> zonesAt(Planet planet, int y, @Nullable String forcedZone,
                                            Predicate<Identifier> inBiome, Predicate<Identifier> nearBlock) {
        List<Planet.Zone> out = new ArrayList<>();
        for (Planet.Zone z : planet.zones()) {
            if (forcedZone != null) {
                if (forcedZone.equals(z.id())) {
                    out.add(z);
                }
                continue;
            }
            if (!z.containsY(y)) {
                continue;
            }
            if (z.biome() != null && !inBiome.test(z.biome())) {
                continue;
            }
            if (z.nearBlock() != null && !nearBlock.test(z.nearBlock())) {
                continue;
            }
            out.add(z);
        }
        return out;
    }

    private static boolean nearBlock(ServerLevel level, BlockPos center, Identifier blockId) {
        var block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-NEAR_BLOCK_RADIUS, -NEAR_BLOCK_RADIUS, -NEAR_BLOCK_RADIUS),
                center.offset(NEAR_BLOCK_RADIUS, NEAR_BLOCK_RADIUS, NEAR_BLOCK_RADIUS))) {
            if (level.getBlockState(p).is(block)) {
                return true;
            }
        }
        return false;
    }
}
