package de.infinitehorizon.compat;

import de.infinitehorizon.compat.rei.IhReiClientPlugin;
import de.infinitehorizon.compat.rei.IhReiCommonPlugin;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.forge.REIPluginCommon;

/** REI findet Plugins auf NeoForge über Annotationen; die Logik steht im gemeinsamen Code. */
public final class NeoForgeReiPlugins {

    private NeoForgeReiPlugins() {
    }

    @REIPluginClient
    public static class Client extends IhReiClientPlugin {
    }

    @REIPluginCommon
    public static class Common extends IhReiCommonPlugin {
    }
}
