package net.vortexdevelopment.vortexcore.vinject.interceptor;


import net.vortexdevelopment.vinject.di.ComponentInterceptor;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;

/**
 * Registers ReloadHooks without the need of annotation for Components
 */
public class ReloadHookInterceptor implements ComponentInterceptor {

    @Override
    public void onComponentRegistered(Class<?> clazz, Object instance, DependencyContainer container) {
        if (ReloadHook.class.isAssignableFrom(clazz)) {
            VortexPlugin.getInstance().registerReloadHook((ReloadHook) instance);
        }
    }
}
