package net.vortexdevelopment.vortexcore.vinject.handler;

import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.command.CommandManager;
import net.vortexdevelopment.vortexcore.command.annotation.Command;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterCommand;
import org.jetbrains.annotations.Nullable;

@Registry(annotation = Command.class)
public class CommandHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, @Nullable Object instance, DependencyContainer dependencyContainer) {
        VortexPlugin.getInstance().getCommandManager().registerCommand(aClass, instance == null ? dependencyContainer.newInstance(aClass) : instance);
    }
}
