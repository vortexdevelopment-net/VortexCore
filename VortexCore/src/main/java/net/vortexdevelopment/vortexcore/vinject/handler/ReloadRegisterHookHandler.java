package net.vortexdevelopment.vortexcore.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;

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
