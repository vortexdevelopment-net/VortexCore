package net.vortexdevelopment.vortexcore.vinject.interceptor;

import net.vortexdevelopment.vinject.annotation.yaml.YamlConfiguration;
import net.vortexdevelopment.vinject.annotation.yaml.YamlDirectory;
import net.vortexdevelopment.vinject.di.ComponentInterceptor;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.internal.ConfigReloadHook;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;

/**
 * Registers reload hooks for beans annotated with {@link RegisterReloadHook}.
 */
public class ReloadHookInterceptor implements ComponentInterceptor {

    @Override
    public void onComponentRegistered(Class<?> clazz, Object instance, DependencyContainer container) {
        if (!clazz.isAnnotationPresent(RegisterReloadHook.class)) {
            return;
        }

        if (ReloadHook.class.isAssignableFrom(clazz)) {
            VortexPlugin.getInstance().registerReloadHook((ReloadHook) instance);
            return;
        }

        if (clazz.isAnnotationPresent(YamlConfiguration.class) || clazz.isAnnotationPresent(YamlDirectory.class)) {
            VortexPlugin.getInstance().registerReloadHook(new ConfigReloadHook(clazz));
        }
    }
}
