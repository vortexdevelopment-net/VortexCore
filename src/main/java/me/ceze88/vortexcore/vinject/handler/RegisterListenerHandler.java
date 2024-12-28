package me.ceze88.vortexcore.vinject.handler;

import me.ceze88.vortexcore.VortexPlugin;
import me.ceze88.vortexcore.vinject.annotation.RegisterListener;
import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import org.bukkit.Bukkit;

import java.lang.annotation.Annotation;

@Registry(annotation = RegisterListener.class)
public class RegisterListenerHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if the class is a org.bukkit.event.Listener
        if (org.bukkit.event.Listener.class.isAssignableFrom(aClass)) {
            Object instance = dependencyContainer.newInstance(aClass);
            //Register the listener
            Bukkit.getPluginManager().registerEvents((org.bukkit.event.Listener) instance, VortexPlugin.getInstance());
        }
    }
}
