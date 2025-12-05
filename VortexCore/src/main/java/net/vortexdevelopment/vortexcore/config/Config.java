package net.vortexdevelopment.vortexcore.config;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.config.serializer.ItemSerializer;
import org.bukkit.inventory.ItemStack;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;

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

    /**
     * Read an ItemStack from a configuration section using the ItemSerializer.
     *
     * @param section The configuration section to read from
     * @return The deserialized ItemStack, or null if the section is null
     */
    public ItemStack readItem(ConfigurationSection section) {
        return ItemSerializer.deserialize(section);
    }

    /**
     * Read an ItemStack from a configuration section at the specified path.
     *
     * @param path The path to the configuration section
     * @return The deserialized ItemStack, or null if the section doesn't exist
     */
    public ItemStack readItem(String path) {
        ConfigurationSection section = getConfigurationSection(path);
        return readItem(section);
    }

    /**
     * Write an ItemStack to a configuration section using the ItemSerializer.
     *
     * @param itemStack The ItemStack to write
     * @param section The configuration section to write to
     */
    public void writeItem(ItemStack itemStack, ConfigurationSection section) {
        ItemSerializer.serialize(itemStack, section);
    }

    /**
     * Write an ItemStack to a configuration section at the specified path.
     *
     * @param itemStack The ItemStack to write
     * @param path The path to the configuration section
     */
    public void writeItem(ItemStack itemStack, String path) {
        ConfigurationSection section = createSection(path);
        writeItem(itemStack, section);
    }
}
