package me.ceze88.vortexcore.config;

import me.ceze88.vortexcore.VortexCore;
import me.ceze88.vortexcore.VortexPlugin;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.snakeyaml.lib.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config extends YamlFile {

    /**
     * Constructor for Config
     * @param path The path to the config file and internal resource
     */
    public Config(String path) {
        super(new File(VortexPlugin.getInstance().getDataFolder(), path));
        if (!getConfigurationFile().exists()) {
            // Save the default config file
            VortexPlugin.getInstance().saveResource(path, false);
        }

        // Load the config file
        try {
            load();
        } catch (Exception e) {
            VortexPlugin.getInstance().getLogger().severe("Failed to load config file (" + path + ")");
            e.printStackTrace();
        }
    }
}
