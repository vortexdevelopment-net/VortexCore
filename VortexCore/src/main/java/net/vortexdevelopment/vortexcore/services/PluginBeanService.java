package net.vortexdevelopment.vortexcore.services;

import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.Service;
import net.vortexdevelopment.vortexcore.VortexCore;
import org.bukkit.plugin.Plugin;

@Service
public class PluginBeanService {

    @Bean
    public Plugin registerPluginBean() {
        return VortexCore.getPlugin();
    }
}
