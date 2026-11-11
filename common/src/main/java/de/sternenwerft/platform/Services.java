package de.sternenwerft.platform;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.platform.services.IEnergyHelper;
import de.sternenwerft.platform.services.IFluidHelper;
import de.sternenwerft.platform.services.INetworkHelper;
import de.sternenwerft.platform.services.IPlatformHelper;
import de.sternenwerft.platform.services.IRegistryHelper;

import java.util.ServiceLoader;

/** Lädt die Loader-Implementierungen per ServiceLoader (Dateien unter META-INF/services je Loader). */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final IEnergyHelper ENERGY = load(IEnergyHelper.class);
    public static final IFluidHelper FLUID = load(IFluidHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Sternenwerft.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
