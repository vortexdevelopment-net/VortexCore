package net.vortexdevelopment.vortexcore.vinject.handler;

import net.vortexdevelopment.vinject.annotation.component.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.vinject.annotation.PlaceholderApiExpansion;
import org.bukkit.Bukkit;

@Registry(annotation = PlaceholderApiExpansion.class)
public class PlaceholderApiExpansionHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        try {
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            if (expansionClass.isAssignableFrom(aClass)) {
                Object instance = component != null ? component : dependencyContainer.newInstance(aClass);
                
                // Invoke register() method on PlaceholderExpansion
                java.lang.reflect.Method registerMethod = expansionClass.getMethod("register");
                registerMethod.invoke(instance);

                // Add to VortexPlugin's registered expansions to unregister on disable
                VortexPlugin.getInstance().registerPlaceholderExpansion(instance);
            }
        } catch (Exception e) {
            VortexPlugin.getInstance().getLogger().warning("Failed to register PlaceholderAPI expansion " + aClass.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
