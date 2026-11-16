package de.infinitehorizon.registry;

import de.infinitehorizon.platform.Services;
import de.infinitehorizon.world.CraterFeature;
import de.infinitehorizon.world.FluidSphereFeature;
import de.infinitehorizon.world.GreatCraterStructure;
import de.infinitehorizon.world.LavaTubeStructure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.function.Supplier;

/**
 * Eigene Worldgen-Typen (Feature-, Struktur- und Strukturteil-Codecs). Die Features und Strukturen selbst stehen als
 * JSON unter worldgen/ (aus spec/data erzeugt).
 */
public final class ModWorldgen {

    public static final Supplier<StructureType<GreatCraterStructure>> GREAT_CRATER = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "great_crater", () -> () -> GreatCraterStructure.CODEC);
    public static final Supplier<StructureType<LavaTubeStructure>> LAVA_TUBE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "lava_tube", () -> () -> LavaTubeStructure.CODEC);
    /** Ring 1 (13.4): Riesenvulkan, Vulkanschlot, Meteoritenfeld mit einer gemeinsamen Klasse. */
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> GIANT_VOLCANO = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "giant_volcano", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> VOLCANIC_VENT = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "volcanic_vent", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> METEORITE_FIELD = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "meteorite_field", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    /** Ring 2 (13.4): Eisvulkan der Ceres, große Asteroiden und Spawn-Asteroid. */
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> ICE_VOLCANO = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "ice_volcano", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> LARGE_ASTEROID = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "large_asteroid", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    /** Quellenfeld der Europa (13.4): Cluster aus hydrothermalen Quellen mit Riffwächter. */
    public static final Supplier<StructureType<de.infinitehorizon.world.SurfaceStructure>> HYDROTHERMAL_FIELD = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_TYPE, "hydrothermal_field", () -> () -> de.infinitehorizon.world.SurfaceStructure.CODEC);
    public static final Supplier<StructurePieceType> ASTEROID_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "asteroid",
            () -> (StructurePieceType.ContextlessType) de.infinitehorizon.world.SurfaceStructure.AsteroidPiece::new);
    public static final Supplier<StructurePieceType> SURFACE_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "surface_structure",
            () -> (StructurePieceType.ContextlessType) de.infinitehorizon.world.SurfaceStructure.Piece::new);
    public static final Supplier<StructurePieceType> GREAT_CRATER_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "great_crater",
            () -> (StructurePieceType.ContextlessType) GreatCraterStructure.Piece::new);
    public static final Supplier<StructurePieceType> LAVA_TUBE_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "lava_tube",
            () -> (StructurePieceType.ContextlessType) LavaTubeStructure.Piece::new);

    static {
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "fluid_sphere", () -> FluidSphereFeature.CODEC);
        // Merkur (13.4): Biome nach X-Koordinate
        Services.REGISTRY.register(BuiltInRegistries.BIOME_SOURCE, "x_bands", () -> de.infinitehorizon.world.XBandsBiomeSource.CODEC);
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "crater", () -> CraterFeature.CODEC);
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "asteroid", () -> de.infinitehorizon.world.AsteroidFeature.CODEC);
        // Enceladus (13.4): Geysire in der Oberfläche
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "geyser", () -> de.infinitehorizon.world.GeyserFeature.CODEC);
        // Europa (13.1): Eisdecke mit Rissen
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "ice_shell", () -> de.infinitehorizon.world.IceShellFeature.CODEC);
        // feste Lage (13.4: genau eine je Dimension)
        Services.REGISTRY.register(BuiltInRegistries.STRUCTURE_PLACEMENT, "fixed",
                () -> de.infinitehorizon.world.FixedStructurePlacement.CODEC);
    }

    private ModWorldgen() {
    }

    public static void init() {
    }
}
