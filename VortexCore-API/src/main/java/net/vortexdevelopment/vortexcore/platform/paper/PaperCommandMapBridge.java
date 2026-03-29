package net.vortexdevelopment.vortexcore.platform.paper;

import net.vortexdevelopment.vinject.annotation.lifecycle.PostConstruct;
import net.vortexdevelopment.vortexcore.spi.CommandMapBridge;
import net.vortexdevelopment.vortexcore.spi.CommandMaps;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

@net.vortexdevelopment.vinject.annotation.component.Component
public class PaperCommandMapBridge implements CommandMapBridge {

    @PostConstruct
    public void registerBridge() {
        CommandMaps.install(this);
    }

    @Override
    public void register(Plugin plugin, PluginCommand command) {
        Bukkit.getServer().getCommandMap().register(plugin.getName(), command);
    }
}
