package de.infinitehorizon.agency;

import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** M3a (27.5): {@link AgencyData} übersteht Speichern und Laden; {@link OwnershipResolver} folgt {@code merged_into}. */
class AgencyDataTest {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");

    private static AgencyData sample(UUID p1, UUID p2, UUID p3) {
        AgencyData d = new AgencyData();
        Agency a = d.ensureFor(p1, 10L);
        a.addMember(new Agency.Member(p2, Role.ENGINEER, 25L));
        a.setName("Mondbasis");
        d.visit(p1, OVERWORLD);
        d.visit(p2, MOON);
        Agency b = d.ensureFor(p3, 40L);
        d.transfer(b.id(), a.id());
        return d;
    }

    private static void assertSameContent(AgencyData expected, AgencyData actual) {
        assertEquals(expected.all().size(), actual.all().size());
        for (Agency e : expected.all()) {
            Agency r = actual.byId(e.id()).orElseThrow();
            assertEquals(e.name(), r.name());
            assertEquals(e.members(), r.members());
            assertEquals(e.mergedInto(), r.mergedInto());
            assertEquals(e.visitedDimensions(), r.visitedDimensions());
        }
    }

    @Test
    void survivesNbtSaveAndLoad() {
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();
        AgencyData d = sample(p1, p2, p3);
        Tag tag = AgencyData.CODEC.encodeStart(NbtOps.INSTANCE, d).getOrThrow();
        assertEquals(Optional.of(AgencyData.DATA_VERSION), ((CompoundTag) tag).getInt("data_version"));
        AgencyData loaded = AgencyData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        assertSameContent(d, loaded);
        // Spieler-Zuordnung und Rollen gelten nach dem Laden weiter
        Agency a = loaded.agencyOf(p2).orElseThrow();
        assertEquals("Mondbasis", a.name());
        assertEquals(Role.LEADER, a.founder().orElseThrow().role());
        assertEquals(p1, a.founder().orElseThrow().player());
        assertTrue(a.visitedDimensions().contains(MOON));
    }

    @Test
    void survivesJsonSaveAndLoad() {
        AgencyData d = sample(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        AgencyData loaded = AgencyData.CODEC.parse(JsonOps.INSTANCE,
                AgencyData.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow()).getOrThrow();
        assertSameContent(d, loaded);
    }

    @Test
    void emptyFileLoadsWithDefaults() {
        AgencyData loaded = AgencyData.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        assertTrue(loaded.all().isEmpty());
    }

    @Test
    void ensureForCreatesOnceWithLeader() {
        AgencyData d = new AgencyData();
        UUID p = UUID.randomUUID();
        Agency first = d.ensureFor(p, 7L);
        assertSame(first, d.ensureFor(p, 99L));
        assertNull(first.name());
        assertEquals(new Agency.Member(p, Role.LEADER, 7L), first.members().getFirst());
    }

    @Test
    void transferEmptiesSourceAndRejectsCycles() {
        AgencyData d = new AgencyData();
        UUID p = UUID.randomUUID();
        UUID a = d.ensureFor(p, 0L).id();
        UUID b = d.ensureFor(UUID.randomUUID(), 0L).id();
        assertEquals(AgencyData.TransferResult.SAME, d.transfer(a, a));
        assertEquals(AgencyData.TransferResult.UNKNOWN_TO, d.transfer(a, UUID.randomUUID()));
        assertEquals(AgencyData.TransferResult.OK, d.transfer(a, b));
        assertEquals(AgencyData.TransferResult.CYCLE, d.transfer(b, a));
        assertTrue(d.agencyOf(p).isEmpty(), "ehemaliges Mitglied hängt noch an der übertragenen Agentur");
        assertEquals(b, d.resolve(a));
    }

    @Test
    void resolverFollowsChainAtoBtoC() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        Map<UUID, UUID> merged = Map.of(a, b, b, c);
        assertEquals(c, OwnershipResolver.current(a, id -> Optional.ofNullable(merged.get(id))));
        assertEquals(c, OwnershipResolver.current(b, id -> Optional.ofNullable(merged.get(id))));
        assertEquals(c, OwnershipResolver.current(c, id -> Optional.ofNullable(merged.get(id))));
    }

    @Test
    void resolverStopsOnCycle() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Map<UUID, UUID> merged = Map.of(a, b, b, a);
        UUID result = OwnershipResolver.current(a, id -> Optional.ofNullable(merged.get(id)));
        assertTrue(result.equals(a) || result.equals(b));
    }

    /** M4a (23.1): Felder ab M4 überstehen Speichern und Laden; eine Agentur aus M3 lädt mit leeren Standardwerten. */
    @Test
    void m4FieldsSurviveAndDefaultEmpty() {
        UUID p1 = UUID.randomUUID();
        AgencyData d = new AgencyData();
        Agency a = d.ensureFor(p1, 1L);
        AgencyProgress pr = a.progress();
        pr.addFp(1050);
        pr.research(Identifier.parse("infinite_horizon:suit_t1"));
        pr.openTier(1);
        pr.openTier(2);
        pr.addProbeTarget(MOON);
        pr.addProbeReached(MOON);
        pr.addPendingAdvancement(p1, Identifier.parse("infinite_horizon:own_air"));
        Tag tag = AgencyData.CODEC.encodeStart(NbtOps.INSTANCE, d).getOrThrow();
        AgencyProgress r = AgencyData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().agencyOf(p1).orElseThrow().progress();
        assertEquals(1050, r.fp());
        assertEquals(java.util.Set.of(Identifier.parse("infinite_horizon:suit_t1")), r.researched());
        assertEquals(java.util.Set.of(1, 2), r.openTiers());
        assertEquals(java.util.Set.of(MOON), r.probeTargets());
        assertEquals(java.util.Set.of(MOON), r.probeReached());
        assertEquals(java.util.List.of(Identifier.parse("infinite_horizon:own_air")), r.takePendingAdvancements(p1));
        assertTrue(r.takePendingAdvancements(p1).isEmpty());

        // Welt aus M3: nur die alten Felder (UUID als Int-Array wie in der Speicherdatei)
        var m3 = Agency.CODEC.encodeStart(JsonOps.INSTANCE, new Agency(UUID.randomUUID(), null, java.util.List.of(), null,
                java.util.List.of(MOON))).getOrThrow().getAsJsonObject();
        assertTrue(!m3.has("fp") && !m3.has("researched") && !m3.has("open_tiers"), "M4-Felder mit Standardwert: " + m3);
        var root = new com.google.gson.JsonObject();
        root.addProperty("data_version", 1);
        var list = new com.google.gson.JsonArray();
        list.add(m3);
        root.add("agencies", list);
        var json = root;
        AgencyProgress old = AgencyData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().all().iterator().next().progress();
        assertEquals(0, old.fp());
        assertTrue(old.researched().isEmpty());
        assertTrue(old.openTiers().isEmpty());
    }

    @Test
    void fpNeverBelowZero() {
        AgencyProgress p = new AgencyProgress();
        p.addFp(500);
        p.addFp(-800);
        assertEquals(0, p.fp());
    }

    /** M4c (23.1): Nachfolge des Leiters: ältestes {@code engineer}-Mitglied, sonst ältestes Mitglied. */
    @Test
    void leaderSuccession() {
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID();
        var members = java.util.List.of(new Agency.Member(p1, Role.GUEST, 5L), new Agency.Member(p2, Role.ENGINEER, 30L),
                new Agency.Member(p3, Role.ENGINEER, 20L));
        assertEquals(p3, Agency.successor(members).orElseThrow().player());
        var guests = java.util.List.of(new Agency.Member(p2, Role.GUEST, 30L), new Agency.Member(p1, Role.GUEST, 5L));
        assertEquals(p1, Agency.successor(guests).orElseThrow().player());
        assertTrue(Agency.successor(java.util.List.of()).isEmpty());

        AgencyData d = new AgencyData();
        Agency a = d.ensureFor(p1, 0L);
        a.addMember(new Agency.Member(p2, Role.ENGINEER, 30L));
        a.addMember(new Agency.Member(p3, Role.ENGINEER, 20L));
        assertEquals(AgencyData.ActionResult.OK, d.leave(p1, 40L));
        assertEquals(Role.LEADER, a.roleOf(p3).orElseThrow());
        assertEquals(Role.ENGINEER, a.roleOf(p2).orElseThrow());
        Agency own = d.agencyOf(p1).orElseThrow();
        assertEquals(Role.LEADER, own.roleOf(p1).orElseThrow());
        assertEquals(AgencyData.ActionResult.ALONE, d.leave(p1, 50L));
    }

    /** M4c (23.1): Einladen nur durch den Leiter; Beitritt leert die alte Agentur, die dann merged_into bekommt. */
    @Test
    void inviteAcceptMerges() {
        UUID leader = UUID.randomUUID(), other = UUID.randomUUID();
        AgencyData d = new AgencyData();
        Agency a = d.ensureFor(leader, 0L);
        Agency b = d.ensureFor(other, 1L);
        assertEquals(AgencyData.ActionResult.NOT_INVITED, d.accept(other, a.id(), 2L));
        UUID engineer = UUID.randomUUID();
        a.addMember(new Agency.Member(engineer, Role.ENGINEER, 1L));
        assertEquals(AgencyData.ActionResult.NOT_LEADER, d.invite(engineer, other));
        assertEquals(AgencyData.ActionResult.ALREADY_MEMBER, d.invite(leader, engineer));
        assertEquals(AgencyData.ActionResult.OK, d.invite(leader, other));
        assertEquals(java.util.List.of(a), d.invitesFor(other));
        assertEquals(AgencyData.ActionResult.OK, d.accept(other, a.id(), 3L));
        assertEquals(Role.ENGINEER, a.roleOf(other).orElseThrow());
        assertEquals(a.id(), b.mergedInto());
        assertTrue(b.members().isEmpty());
        assertTrue(d.invitesFor(other).isEmpty());
        assertEquals(a.id(), d.resolve(b.id()));
    }
}
