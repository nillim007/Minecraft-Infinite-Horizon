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
    public static final Supplier<StructurePieceType> GREAT_CRATER_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "great_crater",
            () -> (StructurePieceType.ContextlessType) GreatCraterStructure.Piece::new);
    public static final Supplier<StructurePieceType> LAVA_TUBE_PIECE = Services.REGISTRY.register(
            BuiltInRegistries.STRUCTURE_PIECE, "lava_tube",
            () -> (StructurePieceType.ContextlessType) LavaTubeStructure.Piece::new);

    static {
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "fluid_sphere", () -> FluidSphereFeature.CODEC);
        Services.REGISTRY.register(BuiltInRegistries.FEATURE_TYPE, "crater", () -> CraterFeature.CODEC);
    }

    private ModWorldgen() {
    }

    public static void init() {
    }
}
