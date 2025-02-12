package me.ceze88.vortexcore.vinject.handler;

import eu.decentsoftware.holograms.api.utils.scheduler.S;
import me.ceze88.vortexcore.VortexPlugin;
import me.ceze88.vortexcore.utils.CommandUtils;
import me.ceze88.vortexcore.vinject.annotation.RegisterCommand;
import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

@Registry(annotation = RegisterCommand.class)
public class RegisterCommandHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, Object component, DependencyContainer dependencyContainer) {
        //Check if it implements CommandExecutor and/or TabCompleter and register them
        boolean isCommandExecutor = CommandExecutor.class.isAssignableFrom(aClass);
        boolean isTabCompleter = TabCompleter.class.isAssignableFrom(aClass);

        if (!isCommandExecutor) {
            throw new RuntimeException("Class " + aClass.getName() + " annotated with @RegisterCommand must implement CommandExecutor");
        }

        // Get the annotation
        RegisterCommand registerCommand = aClass.getAnnotation(RegisterCommand.class);
        String command = registerCommand.commandName();

        // Create a new instance of the class
        Object instance = component == null ? dependencyContainer.newInstance(aClass) : component;

        // Register the command
        if (isTabCompleter) {
            CommandUtils.registerCommand(command, (CommandExecutor) instance, (TabCompleter) instance);
        } else {
            CommandUtils.registerCommand(command, (CommandExecutor) instance, null);
        }
    }
}
