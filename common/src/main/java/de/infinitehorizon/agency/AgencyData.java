package de.infinitehorizon.agency;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Alle Agenturen als {@link SavedData} der Overworld (3A.4, 23.1). Felder in M3: je Agentur {@code id}, {@code name},
 * {@code members}, {@code merged_into}, {@code visited_dimensions}; dazu {@code data_version}. Die Logik ist ohne Server
 * nutzbar (JUnit); {@link #get(MinecraftServer)} holt die Instanz der laufenden Welt.
 */
public final class AgencyData extends SavedData {

    public static final int DATA_VERSION = 1;

    public static final Codec<AgencyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            // immer schreiben (optionalFieldOf mit Standardwert ließe den Wert weg); fehlt er beim Laden, gilt 1
            Codec.INT.optionalFieldOf("data_version").forGetter(d -> Optional.of(DATA_VERSION)),
            Agency.CODEC.listOf().optionalFieldOf("agencies", List.of()).forGetter(d -> List.copyOf(d.agencies.values()))
    ).apply(i, (version, list) -> new AgencyData(list)));

    // Vanilla verlangt einen DataFixTypes-Wert (null stürzt beim Laden ab). Die Fixes für Command-Storage berühren
    // unsere Felder nicht. SPEC-GAP: eigener Datenfix erst bei data_version 2.
    public static final SavedDataType<AgencyData> TYPE = new SavedDataType<>(InfiniteHorizon.id("agencies"),
            AgencyData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    /** Ergebnis von {@link #transfer}. */
    public enum TransferResult { OK, SAME, UNKNOWN_FROM, UNKNOWN_TO, CYCLE }

    private final Map<UUID, Agency> agencies = new LinkedHashMap<>();

    public AgencyData() {
    }

    private AgencyData(List<Agency> list) {
        list.forEach(a -> agencies.put(a.id(), a));
    }

    /** Agenturen der laufenden Welt (Overworld-Datenspeicher). */
    public static AgencyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<Agency> all() {
        return List.copyOf(agencies.values());
    }

    public Optional<Agency> byId(UUID id) {
        return Optional.ofNullable(agencies.get(id));
    }

    /** Aktuelle Agentur eines Spielers: die nicht aufgegangene Agentur, in der er Mitglied ist. */
    public Optional<Agency> agencyOf(UUID player) {
        return agencies.values().stream().filter(a -> a.mergedInto() == null && a.isMember(player)).findFirst();
    }

    /** Agentur des Spielers; gibt es keine, entsteht eine neue mit ihm als {@code leader} (23.1, bei jedem Login). */
    public Agency ensureFor(UUID player, long tick) {
        Optional<Agency> existing = agencyOf(player);
        if (existing.isPresent()) {
            return existing.get();
        }
        Agency created = new Agency(UUID.randomUUID(), null, List.of(new Agency.Member(player, Role.LEADER, tick)),
                null, List.of());
        agencies.put(created.id(), created);
        setDirty();
        return created;
    }

    /** Aktueller Besitzer zu einer gespeicherten {@code owner_agency} (folgt {@code merged_into}). */
    public UUID resolve(UUID owner) {
        return OwnershipResolver.current(owner, id -> byId(id).flatMap(a -> Optional.ofNullable(a.mergedInto())));
    }

    /** Merkt eine besuchte Dimension für die Agentur des Spielers. */
    public void visit(UUID player, Identifier dimension) {
        agencyOf(player).ifPresent(a -> {
            if (a.visit(dimension)) {
                setDirty();
            }
        });
    }

    /** Ergebnis von {@link #invite}, {@link #accept}, {@link #decline} und {@link #leave}. */
    public enum ActionResult { OK, NOT_LEADER, ALREADY_MEMBER, NOT_INVITED, UNKNOWN_AGENCY, ALONE }

    /** Der Leiter {@code leader} lädt {@code target} in seine Agentur ein (23.1). */
    public ActionResult invite(UUID leader, UUID target) {
        Agency a = agencyOf(leader).orElse(null);
        if (a == null || a.roleOf(leader).orElse(Role.GUEST) != Role.LEADER) {
            return ActionResult.NOT_LEADER;
        }
        if (a.isMember(target)) {
            return ActionResult.ALREADY_MEMBER;
        }
        if (a.progress().addInvite(target)) {
            setDirty();
        }
        return ActionResult.OK;
    }

    /**
     * {@code player} nimmt die Einladung von {@code agency} an (23.1 „Beitritt“): Er verlässt seine alte Agentur (der
     * Leiter wird dort ggf. ersetzt) und wird {@code engineer}. Ist die alte Agentur danach leer, geht ihr Besitz über
     * {@code merged_into} an die neue; FP, Forschung und Atlas werden nicht übertragen.
     */
    public ActionResult accept(UUID player, UUID agency, long tick) {
        Agency target = agencies.get(agency);
        if (target == null || target.mergedInto() != null) {
            return ActionResult.UNKNOWN_AGENCY;
        }
        if (target.isMember(player)) {
            return ActionResult.ALREADY_MEMBER;
        }
        if (!target.progress().removeInvite(player)) {
            return ActionResult.NOT_INVITED;
        }
        agencyOf(player).ifPresent(old -> {
            old.removeMember(player);
            if (old.members().isEmpty()) {
                old.setMergedInto(target.id());
            }
        });
        target.addMember(new Agency.Member(player, Role.ENGINEER, tick));
        setDirty();
        return ActionResult.OK;
    }

    /** {@code player} lehnt die Einladung von {@code agency} ab. */
    public ActionResult decline(UUID player, UUID agency) {
        Agency target = agencies.get(agency);
        if (target == null) {
            return ActionResult.UNKNOWN_AGENCY;
        }
        if (!target.progress().removeInvite(player)) {
            return ActionResult.NOT_INVITED;
        }
        setDirty();
        return ActionResult.OK;
    }

    /**
     * {@code player} tritt aus (23.1): Er bekommt sofort eine neue eigene Agentur als {@code leader}; in der alten
     * folgt ggf. ein neuer Leiter. Wer allein ist, kann nicht austreten (SPEC-GAP #70).
     */
    public ActionResult leave(UUID player, long tick) {
        Agency old = agencyOf(player).orElse(null);
        if (old == null) {
            return ActionResult.UNKNOWN_AGENCY;
        }
        if (old.members().size() <= 1) {
            return ActionResult.ALONE;
        }
        old.removeMember(player);
        Agency created = new Agency(UUID.randomUUID(), null, List.of(new Agency.Member(player, Role.LEADER, tick)),
                null, List.of());
        agencies.put(created.id(), created);
        setDirty();
        return ActionResult.OK;
    }

    /** Agenturen, die {@code player} eingeladen haben. */
    public List<Agency> invitesFor(UUID player) {
        return agencies.values().stream().filter(a -> a.mergedInto() == null && a.progress().isInvited(player)).toList();
    }

    /**
     * Überträgt den Besitz von {@code from} an {@code to} (Befehl {@code agency transfer}, 23.1): {@code from} bekommt
     * {@code merged_into}, {@code owner_agency} wird beim nächsten Zugriff umgeschrieben.
     */
    public TransferResult transfer(UUID from, UUID to) {
        Agency source = agencies.get(from);
        if (source == null) {
            return TransferResult.UNKNOWN_FROM;
        }
        if (!agencies.containsKey(to)) {
            return TransferResult.UNKNOWN_TO;
        }
        if (from.equals(to)) {
            return TransferResult.SAME;
        }
        if (resolve(to).equals(from)) {
            return TransferResult.CYCLE;
        }
        source.setMergedInto(to);
        // SPEC-GAP #38: Die übertragene Agentur verliert ihre Mitglieder; kommt eines zurück, bekommt es beim Login
        // eine neue Agentur und kann wieder eingeladen werden (23.1 „Ehemalige Mitglieder“).
        source.clearMembers();
        setDirty();
        return TransferResult.OK;
    }
}
