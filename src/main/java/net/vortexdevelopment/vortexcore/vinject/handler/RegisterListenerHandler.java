package net.vortexdevelopment.vortexcore.vinject.handler;

import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterListener;
import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import org.bukkit.Bukkit;

@Registry(annotation = RegisterListener.class)
public class RegisterListenerHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if the class is a org.bukkit.event.Listener
        if (org.bukkit.event.Listener.class.isAssignableFrom(aClass)) {
            Object instance = component != null ? component : dependencyContainer.newInstance(aClass);

            String registerWhenClassPresent = aClass.getAnnotation(RegisterListener.class).registerWhenClassPresent();

            if (!registerWhenClassPresent.isEmpty()) {
                try {
                    Class.forName(registerWhenClassPresent);
                } catch (ClassNotFoundException e) {
                    return;
                }
            }

            //Register the listener
            Bukkit.getPluginManager().registerEvents((org.bukkit.event.Listener) instance, VortexPlugin.getInstance());
        }
    }
}
