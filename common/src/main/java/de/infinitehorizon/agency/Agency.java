package de.infinitehorizon.agency;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Eine Agentur (23.1, Agentur-Kern M3): ID, Name ({@code null} = „Agentur von &lt;Gründer&gt;“), Mitglieder mit Rolle und
 * Beitritts-Tick, {@code merged_into} (Besitz ging an diese Agentur über) und besuchte Dimensionen. Spätere Felder
 * (FP, Forschung, Atlas …) kommen optional mit Standardwert dazu.
 */
public final class Agency {

    /** Ein Mitglied: Spieler-UUID, Rolle, Server-Tick des Beitritts. */
    public record Member(UUID player, Role role, long joinedTick) {
        public static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Member::player),
                Role.CODEC.fieldOf("role").forGetter(Member::role),
                Codec.LONG.fieldOf("joined_tick").forGetter(Member::joinedTick)
        ).apply(i, Member::new));
    }

    public static final Codec<Agency> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Agency::id),
            Codec.STRING.optionalFieldOf("name").forGetter(a -> Optional.ofNullable(a.name)),
            Member.CODEC.listOf().fieldOf("members").forGetter(a -> List.copyOf(a.members)),
            UUIDUtil.CODEC.optionalFieldOf("merged_into").forGetter(a -> Optional.ofNullable(a.mergedInto)),
            Identifier.CODEC.listOf().optionalFieldOf("visited_dimensions", List.of()).forGetter(a -> List.copyOf(a.visitedDimensions)),
            // Felder ab M4 (23.1) flach daneben, alle optional
            AgencyProgress.MAP_CODEC.forGetter(Agency::progress)
    ).apply(i, (id, name, members, merged, visited, progress) ->
            new Agency(id, name.orElse(null), members, merged.orElse(null), visited, progress)));

    private final UUID id;
    private @Nullable String name;
    private final List<Member> members;
    private @Nullable UUID mergedInto;
    private final Set<Identifier> visitedDimensions;
    private final AgencyProgress progress;

    public Agency(UUID id, @Nullable String name, List<Member> members, @Nullable UUID mergedInto, List<Identifier> visited) {
        this(id, name, members, mergedInto, visited, new AgencyProgress());
    }

    public Agency(UUID id, @Nullable String name, List<Member> members, @Nullable UUID mergedInto, List<Identifier> visited,
                  AgencyProgress progress) {
        this.id = id;
        this.name = name;
        this.members = new ArrayList<>(members);
        this.mergedInto = mergedInto;
        this.visitedDimensions = new LinkedHashSet<>(visited);
        this.progress = progress;
    }

    /** FP, Forschung, offene Stufen, Sondenziele, ausstehende Erfolge (ab M4, 23.1). */
    public AgencyProgress progress() {
        return progress;
    }

    /** Rolle eines Mitglieds, sonst leer. */
    public Optional<Role> roleOf(UUID player) {
        return members.stream().filter(m -> m.player().equals(player)).map(Member::role).findFirst();
    }

    public UUID id() {
        return id;
    }

    /** Eigener Name oder null (Anzeige dann „Agentur von &lt;Gründer&gt;“, 23.1). */
    public @Nullable String name() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public List<Member> members() {
        return List.copyOf(members);
    }

    public boolean isMember(UUID player) {
        return members.stream().anyMatch(m -> m.player().equals(player));
    }

    void addMember(Member m) {
        members.removeIf(x -> x.player().equals(m.player()));
        members.add(m);
    }

    void clearMembers() {
        members.clear();
    }

    /**
     * Nimmt ein Mitglied heraus (Austreten, Beitritt woanders). War es der Leiter, wird {@link #successor} Leiter
     * (23.1). True, wenn es Mitglied war.
     */
    boolean removeMember(UUID player) {
        Optional<Member> gone = members.stream().filter(m -> m.player().equals(player)).findFirst();
        if (gone.isEmpty()) {
            return false;
        }
        members.remove(gone.get());
        if (gone.get().role() == Role.LEADER && members.stream().noneMatch(m -> m.role() == Role.LEADER)) {
            successor(members).ifPresent(s -> {
                int i = members.indexOf(s);
                members.set(i, new Member(s.player(), Role.LEADER, s.joinedTick()));
            });
        }
        return true;
    }

    /**
     * Nachfolge des Leiters (23.1) als reine Funktion: das am längsten beigetretene {@code engineer}-Mitglied, sonst
     * das am längsten beigetretene Mitglied.
     */
    public static Optional<Member> successor(List<Member> remaining) {
        java.util.Comparator<Member> byJoin = java.util.Comparator.comparingLong(Member::joinedTick);
        return remaining.stream().filter(m -> m.role() == Role.ENGINEER).min(byJoin)
                .or(() -> remaining.stream().min(byJoin));
    }

    /** Gründer: das am längsten beigetretene Mitglied mit Rolle {@code leader}, sonst das älteste. */
    public Optional<Member> founder() {
        return members.stream().filter(m -> m.role() == Role.LEADER).findFirst().or(() -> members.stream().findFirst());
    }

    /** Leiter (Rolle {@code leader}). */
    public Optional<Member> leader() {
        return members.stream().filter(m -> m.role() == Role.LEADER).findFirst();
    }

    public @Nullable UUID mergedInto() {
        return mergedInto;
    }

    void setMergedInto(@Nullable UUID target) {
        this.mergedInto = target;
    }

    public Set<Identifier> visitedDimensions() {
        return Set.copyOf(visitedDimensions);
    }

    /** Merkt eine besuchte Dimension; true, wenn sie neu war. */
    boolean visit(Identifier dimension) {
        return visitedDimensions.add(dimension);
    }
}
