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
        Services.NETWORK.registerServerbound(MachineConfigPayload.TYPE, MachineConfigPayload.CODEC, MachineConfigPayload::handle);
    }
}
