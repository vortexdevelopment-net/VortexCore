package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

/**
 * Registers dynamically created {@link PluginCommand} instances; Paper exposes {@code Server#getCommandMap()}
 * while Spigot requires CraftServer reflection.
 */
public interface CommandMapBridge {

    void register(Plugin plugin, PluginCommand command);
}
