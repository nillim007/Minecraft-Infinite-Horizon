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
 * ({@code data_version} bleibt 1). Weitere Felder (Sondenflüge, Probenzähler, Atlas, Landefeuer, Einladungen) kommen mit
 * ihren Teilschritten dazu.
 * <p>
 * Änderungen hier markieren die {@link AgencyData} nicht selbst; Aufrufer rufen danach {@code AgencyData#setDirty()}.
 */
public final class AgencyProgress {

    public static final MapCodec<AgencyProgress> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("fp", 0).forGetter(p -> p.fp),
            Identifier.CODEC.listOf().optionalFieldOf("researched", List.of()).forGetter(p -> List.copyOf(p.researched)),
            Codec.INT.listOf().optionalFieldOf("open_tiers", List.of()).forGetter(p -> List.copyOf(p.openTiers)),
            Identifier.CODEC.listOf().optionalFieldOf("probe_targets", List.of()).forGetter(p -> List.copyOf(p.probeTargets)),
            Identifier.CODEC.listOf().optionalFieldOf("probe_reached", List.of()).forGetter(p -> List.copyOf(p.probeReached)),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Identifier.CODEC.listOf())
                    .optionalFieldOf("pending_advancements", Map.of()).forGetter(AgencyProgress::pendingAdvancements)
    ).apply(i, AgencyProgress::new));

    private int fp;
    private final Set<Identifier> researched;
    private final SortedSet<Integer> openTiers;
    private final Set<Identifier> probeTargets;
    private final Set<Identifier> probeReached;
    private final Map<UUID, Set<Identifier>> pendingAdvancements = new LinkedHashMap<>();

    public AgencyProgress() {
        this(0, List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private AgencyProgress(int fp, List<Identifier> researched, List<Integer> openTiers, List<Identifier> probeTargets,
                           List<Identifier> probeReached, Map<UUID, List<Identifier>> pending) {
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

    /** Nimmt die offenen Erfolge eines Spielers heraus (beim Login). */
    public List<Identifier> takePendingAdvancements(UUID player) {
        Set<Identifier> ids = pendingAdvancements.remove(player);
        return ids == null ? List.of() : List.copyOf(ids);
    }
}
