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
                    de.infinitehorizon.data.RocketPartRegistry.setClient(de.infinitehorizon.data.RocketPartRegistry.fromJson(payload.rocketParts()));
                });
        Services.NETWORK.registerClientbound(AgencyStatePayload.TYPE, AgencyStatePayload.CODEC,
                payload -> de.infinitehorizon.client.AgencyClientState.receive(payload));
        Services.NETWORK.registerClientbound(HazardStatePayload.TYPE, HazardStatePayload.CODEC,
                payload -> de.infinitehorizon.client.HazardHud.receive(payload));
        Services.NETWORK.registerClientbound(DomeStatePayload.TYPE, DomeStatePayload.CODEC,
                payload -> de.infinitehorizon.client.DomeVisuals.receive(payload));
        Services.NETWORK.registerClientbound(LogisticsStatePayload.TYPE, LogisticsStatePayload.CODEC,
                payload -> de.infinitehorizon.client.LogisticsScreen.receive(payload));
        Services.NETWORK.registerServerbound(LogisticsEditPayload.TYPE, LogisticsEditPayload.CODEC, LogisticsEditPayload::handle);
        Services.NETWORK.registerClientbound(ShipyardStatePayload.TYPE, ShipyardStatePayload.CODEC,
                payload -> de.infinitehorizon.client.ShipyardScreen.receive(payload));
        Services.NETWORK.registerServerbound(ShipyardStatePayload.Request.TYPE, ShipyardStatePayload.Request.CODEC,
                ShipyardStatePayload.Request::handle);
        Services.NETWORK.registerServerbound(ShipyardStatePayload.Start.TYPE, ShipyardStatePayload.Start.CODEC,
                ShipyardStatePayload.Start::handle);
        Services.NETWORK.registerClientbound(FinalePayload.TYPE, FinalePayload.CODEC,
                payload -> de.infinitehorizon.client.FinaleScreen.receive(payload));
        Services.NETWORK.registerClientbound(StarshipPayload.Targets.TYPE, StarshipPayload.Targets.CODEC,
                payload -> de.infinitehorizon.client.StarshipJumpScreen.receive(payload));
        Services.NETWORK.registerServerbound(StarshipPayload.Jump.TYPE, StarshipPayload.Jump.CODEC, StarshipPayload.Jump::handle);
        Services.NETWORK.registerServerbound(MachineConfigPayload.TYPE, MachineConfigPayload.CODEC, MachineConfigPayload::handle);
        Services.NETWORK.registerServerbound(ResearchUnlockPayload.TYPE, ResearchUnlockPayload.CODEC, ResearchUnlockPayload::handle);
        Services.NETWORK.registerServerbound(AgencyActionPayload.TYPE, AgencyActionPayload.CODEC, AgencyActionPayload::handle);
        Services.NETWORK.registerServerbound(AgencyRenamePayload.TYPE, AgencyRenamePayload.CODEC, AgencyRenamePayload::handle);
        Services.NETWORK.registerServerbound(TradingPostEditPayload.TYPE, TradingPostEditPayload.CODEC, TradingPostEditPayload::handle);
        Services.NETWORK.registerServerbound(ShipyardStatePayload.Share.TYPE, ShipyardStatePayload.Share.CODEC,
                ShipyardStatePayload.Share::handle);
        Services.NETWORK.registerServerbound(LaunchRequestPayload.TYPE, LaunchRequestPayload.CODEC, LaunchRequestPayload::handle);
        Services.NETWORK.registerClientbound(LaunchStatePayload.TYPE, LaunchStatePayload.CODEC,
                payload -> de.infinitehorizon.client.LaunchControlScreen.receive(payload));
        // Ereignisse (19)
        Services.NETWORK.registerClientbound(EventStatePayload.TYPE, EventStatePayload.CODEC,
                payload -> de.infinitehorizon.client.HazardHud.receiveEvent(payload));
        // Flug (M4e, 12.5, 12.6)
        Services.NETWORK.registerClientbound(FlightSequencePayload.TYPE, FlightSequencePayload.CODEC,
                payload -> de.infinitehorizon.client.FlightSequenceScreen.receive(payload));
        Services.NETWORK.registerServerbound(RocketActionPayload.TYPE, RocketActionPayload.CODEC, RocketActionPayload::handle);
        Services.NETWORK.registerServerbound(NamePayload.TYPE, NamePayload.CODEC, NamePayload::handle);
    }
}
