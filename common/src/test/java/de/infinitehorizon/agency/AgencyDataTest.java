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
}
