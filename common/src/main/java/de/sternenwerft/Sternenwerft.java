package de.sternenwerft;

import de.sternenwerft.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstieg der Mod im gemeinsamen Code (Kapitel 3A.2). Die Loader-Projekte rufen {@link #init()} auf.
 */
public final class Sternenwerft {

    public static final String MOD_ID = "sternenwerft";
    public static final String MOD_NAME = "Sternenwerft";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Sternenwerft() {
    }

    public static void init() {
        LOG.info("[Sternenwerft] Initialisierung auf {} ({})", Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName());
    }
}
