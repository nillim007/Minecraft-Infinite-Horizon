package de.infinitehorizon.agency;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Geteilter Fortschritt einer Agentur ab M4 (23.1, Tabelle „Agentur-Felder ab M4“). Alle Felder sind optional mit
 * leerem Standard; sie stehen in der gespeicherten Agentur flach neben {@code id}, {@code name} usw.
 * ({@code data_version} bleibt 1). Weitere Felder (Sondenflüge, besuchte Biome, Landefeuer, Einladungen) kommen mit
 * ihren Teilschritten dazu.
 * <p>
 * Änderungen hier markieren die {@link AgencyData} nicht selbst; Aufrufer rufen danach {@code AgencyData#setDirty()}.
 */
public final class AgencyProgress {

    /** Gefundenes Erkundungsziel (11.1, 24.2): Dimension, Struktur, Start-Chunk. */
    public record FoundStructure(Identifier dimension, Identifier structure, int chunkX, int chunkZ) {
        public static final Codec<FoundStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(FoundStructure::dimension),
                Identifier.CODEC.fieldOf("structure").forGetter(FoundStructure::structure),
                Codec.INT.fieldOf("chunk_x").forGetter(FoundStructure::chunkX),
                Codec.INT.fieldOf("chunk_z").forGetter(FoundStructure::chunkZ)
        ).apply(i, FoundStructure::new));
    }

    public static final MapCodec<AgencyProgress> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("fp", 0).forGetter(p -> p.fp),
            Identifier.CODEC.listOf().optionalFieldOf("researched", List.of()).forGetter(p -> List.copyOf(p.researched)),
            Codec.INT.listOf().optionalFieldOf("open_tiers", List.of()).forGetter(p -> List.copyOf(p.openTiers)),
            Identifier.CODEC.listOf().optionalFieldOf("probe_targets", List.of()).forGetter(p -> List.copyOf(p.probeTargets)),
            Identifier.CODEC.listOf().optionalFieldOf("probe_reached", List.of()).forGetter(p -> List.copyOf(p.probeReached)),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Identifier.CODEC.listOf())
                    .optionalFieldOf("pending_advancements", Map.of()).forGetter(AgencyProgress::pendingAdvancements),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("sample_counts", Map.of())
                    .forGetter(p -> Map.copyOf(p.sampleCounts)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("structure_counts", Map.of())
                    .forGetter(p -> Map.copyOf(p.structureCounts)),
            FoundStructure.CODEC.listOf().optionalFieldOf("found_structures", List.of())
                    .forGetter(p -> List.copyOf(p.foundStructures))
    ).apply(i, AgencyProgress::new));

    private int fp;
    private final Set<Identifier> researched;
    private final SortedSet<Integer> openTiers;
    private final Set<Identifier> probeTargets;
    private final Set<Identifier> probeReached;
    private final Map<UUID, Set<Identifier>> pendingAdvancements = new LinkedHashMap<>();
    private final Map<String, Integer> sampleCounts;
    private final Map<String, Integer> structureCounts;
    private final List<FoundStructure> foundStructures;

    public AgencyProgress() {
        this(0, List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    private AgencyProgress(int fp, List<Identifier> researched, List<Integer> openTiers, List<Identifier> probeTargets,
                           List<Identifier> probeReached, Map<UUID, List<Identifier>> pending, Map<String, Integer> samples,
                           Map<String, Integer> structures, List<FoundStructure> found) {
        this.sampleCounts = new LinkedHashMap<>(samples);
        this.structureCounts = new LinkedHashMap<>(structures);
        this.foundStructures = new java.util.ArrayList<>(found);
        this.fp = Math.max(0, fp);
        this.researched = new LinkedHashSet<>(researched);
        this.openTiers = new TreeSet<>(openTiers);
        this.probeTargets = new LinkedHashSet<>(probeTargets);
        this.probeReached = new LinkedHashSet<>(probeReached);
        pending.forEach((player, ids) -> pendingAdvancements.put(player, new LinkedHashSet<>(ids)));
    }

    /** FP in Hundertsteln (11.1). */
    public int fp() {
        return fp;
    }

    /** Schreibt FP gut (Hundertstel, &gt; 0) bzw. zieht sie ab (&lt; 0); nie unter 0. */
    public void addFp(int hundredths) {
        long next = (long) fp + hundredths;
        fp = (int) Math.max(0, Math.min(Integer.MAX_VALUE, next));
    }

    public Set<Identifier> researched() {
        return Set.copyOf(researched);
    }

    public boolean isResearched(Identifier node) {
        return researched.contains(node);
    }

    /** Merkt einen erforschten Knoten; true, wenn er neu war. */
    public boolean research(Identifier node) {
        return researched.add(node);
    }

    public SortedSet<Integer> openTiers() {
        return new TreeSet<>(openTiers);
    }

    public boolean isTierOpen(int tier) {
        return openTiers.contains(tier);
    }

    /** Öffnet eine Stufe (Tore schließen sich nie, 11.4); true, wenn sie neu offen ist. */
    public boolean openTier(int tier) {
        return openTiers.add(tier);
    }

    public Set<Identifier> probeTargets() {
        return Set.copyOf(probeTargets);
    }

    public boolean addProbeTarget(Identifier target) {
        return probeTargets.add(target);
    }

    public Set<Identifier> probeReached() {
        return Set.copyOf(probeReached);
    }

    public boolean addProbeReached(Identifier target) {
        return probeReached.add(target);
    }

    /** Spieler → Erfolge, die beim nächsten Login vergeben werden (24.3). */
    public Map<UUID, List<Identifier>> pendingAdvancements() {
        Map<UUID, List<Identifier>> out = new LinkedHashMap<>();
        pendingAdvancements.forEach((p, ids) -> out.put(p, List.copyOf(ids)));
        return out;
    }

    public void addPendingAdvancement(UUID player, Identifier advancement) {
        pendingAdvancements.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(advancement);
    }

    /** Zahl analysierter Proben dieser Probenart (11.1). */
    public int sampleCount(String sampleKey) {
        return sampleCounts.getOrDefault(sampleKey, 0);
    }

    public void countSample(String sampleKey) {
        sampleCounts.merge(sampleKey, 1, Integer::sum);
    }

    /** Zahl gefundener Instanzen dieses Strukturtyps (11.1). */
    public int structureCount(String structureType) {
        return structureCounts.getOrDefault(structureType, 0);
    }

    public void countStructure(String structureType) {
        structureCounts.merge(structureType, 1, Integer::sum);
    }

    public List<FoundStructure> foundStructures() {
        return List.copyOf(foundStructures);
    }

    /** Merkt ein gefundenes Erkundungsziel; true, wenn die Instanz neu ist. */
    public boolean addFoundStructure(FoundStructure found) {
        if (foundStructures.contains(found)) {
            return false;
        }
        foundStructures.add(found);
        return true;
    }

    /** Nimmt die offenen Erfolge eines Spielers heraus (beim Login). */
    public List<Identifier> takePendingAdvancements(UUID player) {
        Set<Identifier> ids = pendingAdvancements.remove(player);
        return ids == null ? List.of() : List.copyOf(ids);
    }
}
