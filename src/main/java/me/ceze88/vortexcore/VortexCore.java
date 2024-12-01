package me.ceze88.vortexcore;

import me.ceze88.vortexcore.hooks.HookManager;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public final class VortexCore {

    private static Plugin currentPlugin;

    public static void setPlugin(Plugin plugin) {
        currentPlugin = plugin;
    }

    public static Plugin getPlugin() {
        return currentPlugin;
    }

}
