package de.sternenwerft.network;

import de.sternenwerft.data.ServerConfig;
import de.sternenwerft.platform.Services;

/** Registriert alle Payloads (3A.5). Client-Handler legen nur Daten ab. */
public final class ModPayloads {

    private ModPayloads() {
    }

    public static void init() {
        Services.NETWORK.registerClientbound(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC,
                payload -> ServerConfig.setClient(payload.json()));
    }
}
