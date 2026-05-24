package net.vortexdevelopment.vortexcore;

import org.bukkit.plugin.Plugin;

public final class VortexCore {

    /**
     * Keeps {@link VortexCorePluginLoader} reachable for bytecode (Paper loads it by name from {@code paper-plugin.yml};
     * without this, maven-shade {@code minimizeJar} in downstream plugins can strip that class).
     */
    @SuppressWarnings("unused")
    //private static final Class<?> PAPER_PLUGIN_LOADER = VortexCorePluginLoader.class;

    private static Plugin currentPlugin;

    public static void setPlugin(Plugin plugin) {
        currentPlugin = plugin;
    }

    public static Plugin getPlugin() {
        return currentPlugin;
    }

}
