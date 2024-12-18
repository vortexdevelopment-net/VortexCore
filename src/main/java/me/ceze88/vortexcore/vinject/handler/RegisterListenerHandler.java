package me.ceze88.vortexcore.vinject.handler;

import me.ceze88.vortexcore.VortexPlugin;
import me.ceze88.vortexcore.vinject.annotation.RegisterListener;
import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import org.bukkit.Bukkit;

import java.lang.annotation.Annotation;

@Registry
public class RegisterListenerHandler extends AnnotationHandler {

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return RegisterListener.class;
    }

    @Override
    public void handle(Class<?> aClass) {
        //Check if the class is a org.bukkit.event.Listener
        if (org.bukkit.event.Listener.class.isAssignableFrom(aClass)) {
            Object instance;
            try {
                instance = aClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create org.bukkit.event.Listener instance for " + aClass.getName(), e);
            }
            //Register the listener
            Bukkit.getPluginManager().registerEvents((org.bukkit.event.Listener) instance, VortexPlugin.getInstance());
        }
    }
}
