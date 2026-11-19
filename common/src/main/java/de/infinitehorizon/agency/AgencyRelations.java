package de.infinitehorizon.agency;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Beziehungen einer Agentur zu anderen (23.3, ab M9): Partner und eigene, noch nicht bestätigte Anfragen. Eine
 * Partnerschaft gilt erst, wenn die Leiter beider Agenturen sie am Terminal bestätigt haben; dann steht jede Agentur in
 * {@code partners} der anderen. Felder flach in der gespeicherten Agentur, optional mit leerem Standard.
 */
public final class AgencyRelations {

    public static final MapCodec<AgencyRelations> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            UUIDUtil.CODEC.listOf().optionalFieldOf("partners", List.of()).forGetter(r -> List.copyOf(r.partners)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("partner_requests", List.of()).forGetter(r -> List.copyOf(r.requests))
    ).apply(i, AgencyRelations::new));

    private final Set<UUID> partners;
    /** Agenturen, denen diese Agentur eine Partnerschaft angeboten hat. */
    private final Set<UUID> requests;

    public AgencyRelations() {
        this(List.of(), List.of());
    }

    private AgencyRelations(List<UUID> partners, List<UUID> requests) {
        this.partners = new LinkedHashSet<>(partners);
        this.requests = new LinkedHashSet<>(requests);
    }

    public Set<UUID> partners() {
        return Set.copyOf(partners);
    }

    public Set<UUID> requests() {
        return Set.copyOf(requests);
    }

    public boolean isPartner(UUID agency) {
        return partners.contains(agency);
    }

    public boolean hasRequested(UUID agency) {
        return requests.contains(agency);
    }

    boolean addPartner(UUID agency) {
        requests.remove(agency);
        return partners.add(agency);
    }

    boolean removePartner(UUID agency) {
        return partners.remove(agency);
    }

    boolean addRequest(UUID agency) {
        return requests.add(agency);
    }

    boolean removeRequest(UUID agency) {
        return requests.remove(agency);
    }
}
