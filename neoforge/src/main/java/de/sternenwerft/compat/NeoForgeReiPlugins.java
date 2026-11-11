package de.sternenwerft.compat;

import de.sternenwerft.compat.rei.SwReiClientPlugin;
import de.sternenwerft.compat.rei.SwReiCommonPlugin;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.forge.REIPluginCommon;

/** REI findet Plugins auf NeoForge über Annotationen; die Logik steht im gemeinsamen Code. */
public final class NeoForgeReiPlugins {

    private NeoForgeReiPlugins() {
    }

    @REIPluginClient
    public static class Client extends SwReiClientPlugin {
    }

    @REIPluginCommon
    public static class Common extends SwReiCommonPlugin {
    }
}
