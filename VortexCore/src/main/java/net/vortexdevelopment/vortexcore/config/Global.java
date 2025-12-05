package net.vortexdevelopment.vortexcore.config;

import net.vortexdevelopment.vinject.annotation.Component;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@Component
@RegisterReloadHook
public class Global implements ReloadHook {

    private static YamlConfiguration config;

    public Global() {
        onReload();
    }

    @Override
    public void onReload() {
        File file = new File(VortexPlugin.getInstance().getDataFolder(), "global.yml");
        if (!file.exists()) {
            VortexPlugin.getInstance().saveResource("global.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static <T> T isFeatureEnabled(String feature, boolean defaultValue, Class<T> type) {
        return (T) config.get(feature, false);
    }
}
