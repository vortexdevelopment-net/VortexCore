package net.vortexdevelopment.vortexcore.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public interface ConfigBridge {

    /**
     * Converts a Bukkit ConfigurationSection to a shaded read only SimpleYaml ConfigurationSection.
     *
     * @param section the Bukkit ConfigurationSection to convert
     * @return the converted SimpleYaml ConfigurationSection
     */
    default ConfigurationSection toBukkit(@NotNull org.simpleyaml.configuration.ConfigurationSection section) {
        Objects.requireNonNull(section, "section cannot be null");
        Map<String, Object> values = section.getValues(true);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        for (String key : values.keySet()) {
            Object value = values.get(key);
            if (value instanceof org.simpleyaml.configuration.ConfigurationSection subSection) {
                yamlConfiguration.createSection(key, toMap(subSection));
            } else {
                yamlConfiguration.set(key, value);
            }
        }

        return yamlConfiguration;
    }

    default Map<String, Object> toMap(org.simpleyaml.configuration.ConfigurationSection section) {
        Map<String, Object> map = section.getValues(true);
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof org.simpleyaml.configuration.ConfigurationSection subSection) {
                map.put(key, toMap(subSection));
            }
        }
        return map;
    }
}
