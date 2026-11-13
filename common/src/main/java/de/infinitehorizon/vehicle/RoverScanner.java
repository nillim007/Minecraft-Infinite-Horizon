package de.infinitehorizon.vehicle;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.research.ExplorationService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Modul {@code rover_scanner} (16): alle {@code interval_ticks} Erze im Umkreis {@code ore_radius} zählen und
 * Erkundungsziele im Umkreis {@code exploration_radius} melden. SPEC-GAP #91: Umrandung durch Wände und Markierung im
 * Atlas folgen; bis dahin nennt der Scanner den Insassen Zahl und Lage.
 */
public final class RoverScanner {

    private RoverScanner() {
    }

    /** Erz = Block, dessen ID auf {@code _ore} endet (Mod und Vanilla). */
    static boolean isOre(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_ore");
    }

    /** Zahl der Erzblöcke im Würfel um {@code center}; Abschnitte ohne Erz werden übersprungen. */
    public static int countOres(ServerLevel level, BlockPos center, int radius) {
        Predicate<BlockState> ore = RoverScanner::isOre;
        int count = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int size = SectionPos.SECTION_SIZE;
        for (int cx = SectionPos.blockToSectionCoord(center.getX() - radius); cx <= SectionPos.blockToSectionCoord(center.getX() + radius); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(center.getZ() - radius); cz <= SectionPos.blockToSectionCoord(center.getZ() + radius); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    if (sections[i].hasOnlyAir() || !sections[i].maybeHas(ore)) {
                        continue;
                    }
                    int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                    for (int x = 0; x < size; x++) {
                        for (int y = 0; y < size; y++) {
                            for (int z = 0; z < size; z++) {
                                p.set(SectionPos.sectionToBlockCoord(cx) + x, baseY + y, SectionPos.sectionToBlockCoord(cz) + z);
                                if (Math.abs(p.getX() - center.getX()) <= radius && Math.abs(p.getY() - center.getY()) <= radius
                                        && Math.abs(p.getZ() - center.getZ()) <= radius && ore.test(sections[i].getBlockState(x, y, z))) {
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    static void scan(ServerLevel level, RoverEntity rover) {
        JsonObject m = RoverEntity.module(RoverEntity.SCANNER);
        int ores = countOres(level, rover.blockPosition(), m.has("ore_radius") ? m.get("ore_radius").getAsInt() : 0);
        int r = m.has("exploration_radius") ? m.get("exploration_radius").getAsInt() : 0;
        List<String> found = new ArrayList<>();
        var targets = ExplorationService.targets();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (int cx = SectionPos.blockToSectionCoord(rover.getBlockX() - r); cx <= SectionPos.blockToSectionCoord(rover.getBlockX() + r); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(rover.getBlockZ() - r); cz <= SectionPos.blockToSectionCoord(rover.getBlockZ() + r); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, StructureStart> e : chunk.getAllStarts().entrySet()) {
                    Identifier id = structures.getKey(e.getKey());
                    if (id != null && targets.contains(id) && e.getValue().isValid()) {
                        BlockPos c = e.getValue().getBoundingBox().getCenter();
                        found.add(Component.translatable("structure." + id.getNamespace() + "." + id.getPath()).getString()
                                + " " + c.getX() + "/" + c.getZ());
                    }
                }
            }
        }
        Component msg = Component.translatable("message." + InfiniteHorizon.MOD_ID + ".rover.scan", ores,
                found.isEmpty() ? "-" : String.join(", ", found));
        for (var e : rover.getPassengers()) {
            if (e instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(msg);
            }
        }
    }
}
