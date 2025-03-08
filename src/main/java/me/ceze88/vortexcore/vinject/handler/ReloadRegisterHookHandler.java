package me.ceze88.vortexcore.vinject.handler;

import me.ceze88.vortexcore.VortexPlugin;
import me.ceze88.vortexcore.hooks.internal.ReloadHook;
import me.ceze88.vortexcore.vinject.annotation.RegisterListener;
import me.ceze88.vortexcore.vinject.annotation.RegisterReloadHook;
import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import org.bukkit.Bukkit;

@Registry(annotation = RegisterReloadHook.class)
public class ReloadRegisterHookHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if the class is a org.bukkit.event.Listener
        if (ReloadHook.class.isAssignableFrom(aClass)) {
            Object instance = component != null ? component : dependencyContainer.newInstance(aClass);

            //Register the listener
            VortexPlugin.getInstance().registerReloadHook((ReloadHook) instance);
        }
    }
}

