package de.infinitehorizon.network;

import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.platform.Services;

/** Registriert alle Payloads (3A.5). Client-Handler legen nur Daten ab. */
public final class ModPayloads {

    private ModPayloads() {
    }

    public static void init() {
        Services.NETWORK.registerClientbound(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC,
                payload -> ServerConfig.setClient(payload.json()));
        Services.NETWORK.registerClientbound(SyncDataPayload.TYPE, SyncDataPayload.CODEC,
                payload -> {
                    de.infinitehorizon.data.PlanetRegistry.setClient(de.infinitehorizon.data.PlanetRegistry.fromJson(payload.planets()));
                    de.infinitehorizon.data.ResearchRegistry.setClient(de.infinitehorizon.data.ResearchRegistry.fromJson(payload.research()));
                });
        Services.NETWORK.registerClientbound(AgencyStatePayload.TYPE, AgencyStatePayload.CODEC,
                payload -> de.infinitehorizon.client.AgencyClientState.receive(payload));
        Services.NETWORK.registerClientbound(HazardStatePayload.TYPE, HazardStatePayload.CODEC,
                payload -> de.infinitehorizon.client.HazardHud.receive(payload));
        Services.NETWORK.registerClientbound(DomeStatePayload.TYPE, DomeStatePayload.CODEC,
                payload -> de.infinitehorizon.client.DomeVisuals.receive(payload));
        Services.NETWORK.registerServerbound(MachineConfigPayload.TYPE, MachineConfigPayload.CODEC, MachineConfigPayload::handle);
        Services.NETWORK.registerServerbound(ResearchUnlockPayload.TYPE, ResearchUnlockPayload.CODEC, ResearchUnlockPayload::handle);
        Services.NETWORK.registerServerbound(AgencyActionPayload.TYPE, AgencyActionPayload.CODEC, AgencyActionPayload::handle);
    }
}
