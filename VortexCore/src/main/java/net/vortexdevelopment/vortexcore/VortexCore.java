package net.vortexdevelopment.vortexcore;

import org.bukkit.plugin.Plugin;

public final class VortexCore {

    private static Plugin currentPlugin;

    public static void setPlugin(Plugin plugin) {
        currentPlugin = plugin;
    }

    public static Plugin getPlugin() {
        return currentPlugin;
    }

}
