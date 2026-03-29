package net.vortexdevelopment.vortexcore.platform.spigot;

import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.spi.CommandMapBridge;
import net.vortexdevelopment.vortexcore.spi.CommandMaps;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

@net.vortexdevelopment.vinject.annotation.component.Component
public class SpigotCommandMapBridge implements CommandMapBridge {

    @PostConstruct
    public void registerBridge() {
        CommandMaps.install(this);
    }

    @Override
    public void register(Plugin plugin, PluginCommand command) {
        try {
            Object craftServer = Bukkit.getServer();
            Object commandMap = craftServer.getClass().getDeclaredMethod("getCommandMap").invoke(craftServer);
            commandMap.getClass()
                    .getMethod("register", String.class, Command.class)
                    .invoke(commandMap, plugin.getName(), command);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register command on Spigot", e);
        }
    }
}
