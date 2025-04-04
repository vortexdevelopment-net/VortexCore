package net.vortexdevelopment.vortexcore.command;

import net.vortexdevelopment.vinject.annotation.Component;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.command.annotation.BaseCommand;
import net.vortexdevelopment.vortexcore.command.annotation.Command;
import net.vortexdevelopment.vortexcore.command.annotation.Param;
import net.vortexdevelopment.vortexcore.command.annotation.Permission;
import net.vortexdevelopment.vortexcore.command.annotation.Sender;
import net.vortexdevelopment.vortexcore.command.annotation.SubCommand;
import net.vortexdevelopment.vortexcore.command.annotation.TabArgs;
import net.vortexdevelopment.vortexcore.command.annotation.TabComplete;
import net.vortexdevelopment.vortexcore.command.annotation.TabIndex;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {
    private final VortexPlugin plugin;
    private final Map<Class<?>, ParameterResolver<?>> resolvers;
    private final Map<String, Object> commandInstances; // Store command instances instead of executors
    private final CommandExecutor sharedExecutor; // Singleton executor
    private final TabCompleter sharedTabCompleter; // Add singleton tab completer

    public CommandManager() {
        this.plugin = VortexPlugin.getInstance();
        this.resolvers = new HashMap<>();
        this.commandInstances = new HashMap<>();
        this.sharedExecutor = createSharedExecutor();
        this.sharedTabCompleter = createSharedTabCompleter(); // Initialize the singleton tab completer
    }

    /**
     * Registers a parameter resolver for converting string arguments to specific types
     */
    public void registerResolver(ParameterResolver<?> resolver) {
        if (resolver == null) {
            plugin.getLogger().warning("Attempted to register null resolver - ignoring");
            return;
        }
        
        Set<Class<?>> supportedTypes = resolver.getSupportedTypes();
        
        if (supportedTypes.isEmpty()) {
            plugin.getLogger().warning("Resolver " + resolver.getClass().getSimpleName() + 
                                      " did not register any types - it may not work correctly");
        }
        
        // Register all explicitly supported types
        for (Class<?> type : supportedTypes) {
            if (type != null) {
                resolvers.put(type, resolver);
                plugin.getLogger().info("Registered " + resolver.getClass().getSimpleName() + 
                                       " for type " + type.getSimpleName());
            } else {
                plugin.getLogger().warning("Resolver " + resolver.getClass().getSimpleName() + 
                                          " tried to register null type - ignoring");
            }
        }
        
        // Also register the resolver for its own class (for backward compatibility)
        resolvers.put(resolver.getClass(), resolver);
    }

    /**
     * Registers a command class
     */
    public void registerCommand(Class<?> commandClass, Object instance) {
        Command commandAnnotation = commandClass.getAnnotation(Command.class);
        if (commandAnnotation == null) {
            plugin.getLogger().warning("Command annotation not found on class: " + commandClass.getName());
            return;
        }

        String commandName = commandAnnotation.value();
        PluginCommand command = plugin.getServer().getPluginCommand(commandName);
        if (command == null) {
            // Create a new command dynamically
            try {
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                command = constructor.newInstance(commandName, plugin);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create plugin command: " + commandName);
                return;
            }
            plugin.getServer().getCommandMap().register(plugin.getName(), command);
        }

        // Set command aliases
        String[] aliases = commandAnnotation.aliases();
        if (aliases.length > 0) {
            command.setAliases(Arrays.asList(aliases));
            plugin.getLogger().info("Registered aliases for command " + commandName + ": " + 
                                   String.join(", ", aliases));
        }

        // Store the command instance instead of creating a new executor
        commandInstances.put(commandName, instance);

        // Use the shared executor for all commands
        command.setExecutor(sharedExecutor);

        // Register tab completion
        registerTabCompletion(commandName, instance);

        plugin.getLogger().info("Registered command: " + commandName);
    }

    /**
     * Unregisters all commands
     */
    public void unregisterAll() {
        for (String commandName : commandInstances.keySet()) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                command.setExecutor(null);
                command.setTabCompleter(null);
            }
        }
        commandInstances.clear();
    }

    /**
     * Creates a shared command executor that works for all command instances
     */
    private CommandExecutor createSharedExecutor() {
        return (sender, command, label, args) -> {
            // Get the command instance for this command name
            Object instance = commandInstances.get(command.getName());
            if (instance == null) {
                return false;
            }
            
            Method baseCommand = null;
            Map<Method, List<String>> subCommandPatterns = new HashMap<>();

            // Find base command and subcommands
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(BaseCommand.class)) {
                    baseCommand = method;
                } else if (method.isAnnotationPresent(SubCommand.class)) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    List<String> patterns = new ArrayList<>();
                    patterns.add(subCommand.command());
                    
                    // Add aliases if they exist
                    if (subCommand.aliases() != null && subCommand.aliases().length > 0) {
                        patterns.addAll(Arrays.asList(subCommand.aliases()));
                    }
                    
                    subCommandPatterns.put(method, patterns);
                }
            }

            // Execute base command if no args
            if (args.length == 0 && baseCommand != null) {
                return executeCommand(sender, instance, baseCommand, args);
            }

            // Try to match a subcommand
            for (Map.Entry<Method, List<String>> entry : subCommandPatterns.entrySet()) {
                for (String pattern : entry.getValue()) {
                    if (matchesSubCommand(pattern, args)) {
                        return executeCommand(sender, instance, entry.getKey(), args);
                    }
                }
            }

            // Fall back to base command
            if (baseCommand != null) {
                return executeCommand(sender, instance, baseCommand, args);
            }

            return false;
        };
    }

    /**
     * Registers tab completion for a command
     */
    private void registerTabCompletion(String commandName, Object instance) {
        PluginCommand command = plugin.getServer().getPluginCommand(commandName);
        if (command == null) return;
        
        // Use the shared tab completer for all commands
        command.setTabCompleter(sharedTabCompleter);
    }

    /**
     * Creates a shared tab completer that works for all command instances
     */
    private TabCompleter createSharedTabCompleter() {
        return (sender, cmd, alias, args) -> {
            // Get the command instance for this command name
            Object instance = commandInstances.get(cmd.getName());
            if (instance == null) {
                return Collections.emptyList();
            }
            
            List<String> completions = new ArrayList<>();
            
            Map<String, Method> tabCompleteMethods = new HashMap<>();
            Map<String, Method> paramTabCompleteMethods = new HashMap<>();
            
            // Find all tab complete methods for this command instance
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(TabComplete.class)) {
                    TabComplete tabComplete = method.getAnnotation(TabComplete.class);
                    
                    if (!tabComplete.param().isEmpty()) {
                        // Parameter-based tab completion
                        paramTabCompleteMethods.put(tabComplete.param(), method);
                    } else {
                        // Command pattern-based tab completion
                        tabCompleteMethods.put(tabComplete.command(), method);
                    }
                }
            }
            
            if (tabCompleteMethods.isEmpty() && paramTabCompleteMethods.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Get all subcommands for this command
            Map<String, Method> subCommands = new HashMap<>();
            for (Method method : instance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubCommand.class)) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    subCommands.put(subCommand.command(), method);
                    
                    // Also register aliases if they exist
                    if (subCommand.aliases() != null && subCommand.aliases().length > 0) {
                        for (String al : subCommand.aliases()) {
                            subCommands.put(al, method);
                        }
                    }
                }
            }
            
            // First try parameter-based completions
            if (!paramTabCompleteMethods.isEmpty()) {
                handleParameterBasedTabCompletions(sender, args, paramTabCompleteMethods, subCommands, instance, completions);
            }
            
            // Then try command pattern-based completions
            if (!tabCompleteMethods.isEmpty()) {
                handlePatternBasedTabCompletions(sender, args, tabCompleteMethods, instance, completions);
            }
            
            // Filter completions based on current input
            String currentArg = args.length > 0 ? args[args.length - 1] : "";
            return filterCompletions(completions, currentArg);
        };
    }

    /**
     * Handles parameter-based tab completions
     */
    private void handleParameterBasedTabCompletions(CommandSender sender, String[] args, 
                                                   Map<String, Method> paramTabCompleteMethods,
                                                   Map<String, Method> subCommands, Object instance,
                                                   List<String> completions) {
        for (Map.Entry<String, Method> entry : subCommands.entrySet()) {
            String pattern = entry.getKey();
            Method method = entry.getValue();
            String[] patternParts = pattern.split(" ");
            
            // Skip if no permission
            if (!hasPermission(sender, instance.getClass(), method)) {
                continue;
            }
            
            // Skip patterns that don't match the current args
            if (!matchesPartialPattern(patternParts, args)) {
                continue;
            }
            
            // Find the parameter at the current arg index
            int argIndex = args.length - 1;
            
            // Make sure we don't go out of bounds
            if (argIndex < patternParts.length) {
                String patternPart = patternParts[argIndex];
                
                if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                    String paramName = patternPart.substring(1, patternPart.length() - 1);
                    
                    // Check if we have a tab completer for this parameter
                    Method tabMethod = paramTabCompleteMethods.get(paramName);
                    if (tabMethod != null) {
                        try {
                            // Create an array with only the parameters the method expects
                            Object[] methodParams = createMatchingParameters(sender, tabMethod, args, argIndex);
                            Object result = tabMethod.invoke(instance, methodParams);
                            if (result instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> paramCompletions = (List<String>) result;
                                completions.addAll(paramCompletions);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error executing tab complete: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    // It's a static part, suggest it if it matches the current input
                    String currentArg = args[argIndex];
                    if (patternPart.toLowerCase().startsWith(currentArg.toLowerCase())) {
                        completions.add(patternPart);
                    }
                }
            }
        }
    }

    /**
     * Handles pattern-based tab completions
     */
    private void handlePatternBasedTabCompletions(CommandSender sender, String[] args,
                                                 Map<String, Method> tabCompleteMethods,
                                                 Object instance, List<String> completions) {
        for (Map.Entry<String, Method> entry : tabCompleteMethods.entrySet()) {
            String pattern = entry.getKey();
            Method method = entry.getValue();
            
            // Skip if no permission
            if (!hasPermission(sender, instance.getClass(), method)) {
                continue;
            }
            
            if (pattern.isEmpty() || matchesTabCompletePattern(pattern, args)) {
                TabComplete tabComplete = method.getAnnotation(TabComplete.class);
                int argIndex = tabComplete.argIndex();
                
                // If argIndex is -1, use the last argument
                if (argIndex == -1) {
                    argIndex = args.length - 1;
                }
                
                // Only complete if we're at the right argument index
                if (argIndex >= 0 && argIndex < args.length) {
                    try {
                        // Create an array with only the parameters the method expects
                        Object[] methodParams = createMatchingParameters(sender, method, args, argIndex);
                        Object result = method.invoke(instance, methodParams);
                        if (result instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> paramCompletions = (List<String>) result;
                            completions.addAll(paramCompletions);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error executing tab complete: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Creates an array of parameters that matches exactly what the method expects
     */
    private Object[] createMatchingParameters(CommandSender sender, Method method, String[] args, int argIndex) {
        Parameter[] parameters = method.getParameters();
        Object[] methodParams = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            if (param.isAnnotationPresent(Sender.class)) {
                // Handle sender parameter
                if (param.getType().isAssignableFrom(sender.getClass())) {
                    methodParams[i] = sender;
                } else if (param.getType() == Player.class && sender instanceof Player) {
                    methodParams[i] = sender;
                } else {
                    methodParams[i] = null;
                }
            } else if (param.isAnnotationPresent(TabArgs.class)) {
                // Handle args parameter
                if (param.getType() == String[].class) {
                    methodParams[i] = args;
                } else {
                    methodParams[i] = null;
                }
            } else if (param.isAnnotationPresent(TabIndex.class)) {
                // Handle index parameter
                if (param.getType() == int.class || param.getType() == Integer.class) {
                    methodParams[i] = argIndex;
                } else {
                    methodParams[i] = param.getType() == int.class ? 0 : null;
                }
            } else {
                // For backward compatibility, try to infer the parameter type
                if (i == 0 && (param.getType().isAssignableFrom(sender.getClass()) || 
                             (param.getType() == Player.class && sender instanceof Player))) {
                    methodParams[i] = sender;
                } else if (i == 1 && param.getType() == String[].class) {
                    methodParams[i] = args;
                } else if (i == 2 && (param.getType() == int.class || param.getType() == Integer.class)) {
                    methodParams[i] = argIndex;
                } else {
                    methodParams[i] = null;
                }
            }
        }
        
        return methodParams;
    }

    /**
     * Checks if a command pattern matches the current args for tab completion
     */
    private boolean matchesPartialPattern(String[] patternParts, String[] args) {
        // For partial matching, we need to check all args except the last one
        int argsToCheck = args.length - 1;
        
        if (argsToCheck > patternParts.length) {
            return false;
        }
        
        for (int i = 0; i < argsToCheck; i++) {
            String patternPart = patternParts[i];
            
            // If it's a parameter placeholder, it matches anything
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }
            
            // For static parts, they must match exactly
            if (!patternPart.equalsIgnoreCase(args[i])) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if a tab complete pattern matches the current args
     */
    private boolean matchesTabCompletePattern(String pattern, String[] args) {
        String[] patternParts = pattern.split(" ");
        
        // If we have more args than pattern parts, only match if the pattern ends with a parameter
        if (args.length > patternParts.length && 
            (patternParts.length == 0 || !patternParts[patternParts.length - 1].startsWith("{"))) {
            return false;
        }
        
        // Check each part of the pattern
        for (int i = 0; i < Math.min(patternParts.length, args.length); i++) {
            String patternPart = patternParts[i];
            
            // If it's a parameter, it matches anything
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }
            
            // Otherwise, it must match exactly
            if (!patternPart.equalsIgnoreCase(args[i])) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Filters completions based on the current input prefix
     */
    private List<String> filterCompletions(List<String> completions, String prefix) {
        if (prefix.isEmpty()) return completions;
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a subcommand pattern matches the given args
     */
    private boolean matchesSubCommand(String pattern, String[] args) {
        String[] patternParts = pattern.split(" ");
        
        // Handle wildcard pattern at the end (e.g., {**})
        boolean hasWildcard = patternParts.length > 0 && patternParts[patternParts.length - 1].equals("{**}");
        
        // If we have a wildcard, we need at least the number of non-wildcard parts
        if (hasWildcard) {
            if (args.length < patternParts.length - 1) return false;
        } 
        // Without wildcard, we need exact match in length
        else if (patternParts.length != args.length) {
            return false;
        }
    
        // Check each part of the pattern against the args
        int argIndex = 0;
        for (int i = 0; i < patternParts.length; i++) {
            // Skip wildcard at the end, we've already handled the length check
            if (i == patternParts.length - 1 && hasWildcard) {
                break;
            }
            
            String patternPart = patternParts[i];
            
            // If it's a parameter placeholder, just increment the arg index
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                argIndex++;
                continue;
            }
            
            // For static text, it must match exactly
            if (!patternPart.equalsIgnoreCase(args[argIndex])) {
                return false;
            }
            
            argIndex++;
        }
        
        return true;
    }

    /**
     * Executes a command method
     */
    private boolean executeCommand(CommandSender sender, Object instance, Method method, String[] args) {
        try {
            // Check permissions first
            if (!hasPermission(sender, instance.getClass(), method)) {
                return true; // Return true to indicate we handled the command
            }
            
            // Check if the sender type is compatible with the first parameter
            Parameter[] parameters = method.getParameters();
            if (parameters.length > 0) {
                Parameter firstParam = parameters[0];
                if (firstParam.getAnnotation(Param.class) == null) {
                    Class<?> requiredType = firstParam.getType();
                    
                    // Check sender type compatibility
                    if (!requiredType.isAssignableFrom(sender.getClass())) {
                        if (Player.class.isAssignableFrom(requiredType)) {
                            sender.sendMessage("This command can only be executed by a player.");
                        } else if (ConsoleCommandSender.class.isAssignableFrom(requiredType)) {
                            sender.sendMessage("This command can only be executed from the console.");
                        } else {
                            sender.sendMessage("You cannot execute this command.");
                        }
                        return true; // Return true to indicate we handled the command
                    }
                }
            }
            
            Object[] resolvedParameters = resolveParameters(sender, method, args);
            if (resolvedParameters == null) return false;
            
            method.invoke(instance, resolvedParameters);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing command: " + e.getMessage());
            if (e instanceof InvocationTargetException) {
                ((InvocationTargetException) e).getTargetException().printStackTrace();
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Checks if a sender has permission to execute a command
     */
    private boolean hasPermission(CommandSender sender, Class<?> commandClass, Method method) {
        // Check method-level permission first (more specific)
        if (method.isAnnotationPresent(Permission.class)) {
            Permission permission = method.getAnnotation(Permission.class);
            if (!sender.hasPermission(permission.value())) {
                String message = permission.message();
                if (message.isEmpty()) {
                    message = "§cYou don't have permission to use this command.";
                }
                sender.sendMessage(message);
                return false;
            }
        }
        
        // Then check class-level permission
        if (commandClass.isAnnotationPresent(Permission.class)) {
            Permission permission = commandClass.getAnnotation(Permission.class);
            if (!sender.hasPermission(permission.value())) {
                String message = permission.message();
                if (message.isEmpty()) {
                    message = "§cYou don't have permission to use this command.";
                }
                sender.sendMessage(message);
                return false;
            }
        }
        
        return true;
    }

    /**
     * Resolves parameters for command execution
     */
    private Object[] resolveParameters(CommandSender sender, Method method, String[] args) {
        Parameter[] parameters = method.getParameters();
        Object[] resolvedParams = new Object[parameters.length];
        
        // Track which args have been used
        boolean[] usedArgs = new boolean[args.length];
        
        // First pass: handle @Sender and other special annotations
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            if (parameter.isAnnotationPresent(Sender.class)) {
                // Handle @Sender annotation
                if (parameter.getType().isAssignableFrom(sender.getClass())) {
                    resolvedParams[i] = sender;
                } else if (parameter.getType() == Player.class && sender instanceof Player) {
                    resolvedParams[i] = sender;
                } else {
                    // Sender type mismatch
                    return null;
                }
            }
        }
        
        // Second pass: handle @Param annotations
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            if (parameter.isAnnotationPresent(Param.class)) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                String paramName = paramAnnotation.value();
                
                // Handle wildcard parameter (captures all remaining args)
                if (paramName.equals("**")) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < args.length; j++) {
                        if (!usedArgs[j]) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(args[j]);
                            usedArgs[j] = true;
                        }
                    }
                    
                    resolvedParams[i] = resolveParameter(parameter.getType(), sb.toString());
                    continue;
                }
                
                // Find the parameter in the command pattern
                int paramIndex = findParamIndex(method, paramName, args);
                if (paramIndex >= 0 && paramIndex < args.length && !usedArgs[paramIndex]) {
                    resolvedParams[i] = resolveParameter(parameter.getType(), args[paramIndex]);
                    usedArgs[paramIndex] = true;
                } else {
                    // Parameter not found or already used
                    return null;
                }
            } else if (resolvedParams[i] == null) {
                // For parameters without annotations, try to resolve by position
                for (int j = 0; j < args.length; j++) {
                    if (!usedArgs[j]) {
                        resolvedParams[i] = resolveParameter(parameter.getType(), args[j]);
                        usedArgs[j] = true;
                        break;
                    }
                }
                
                // If still null, try to use default values or null
                if (resolvedParams[i] == null) {
                    if (parameter.getType().isPrimitive()) {
                        // Use default values for primitives
                        if (parameter.getType() == int.class) resolvedParams[i] = 0;
                        else if (parameter.getType() == long.class) resolvedParams[i] = 0L;
                        else if (parameter.getType() == double.class) resolvedParams[i] = 0.0;
                        else if (parameter.getType() == float.class) resolvedParams[i] = 0.0f;
                        else if (parameter.getType() == boolean.class) resolvedParams[i] = false;
                        else if (parameter.getType() == char.class) resolvedParams[i] = '\0';
                        else if (parameter.getType() == byte.class) resolvedParams[i] = (byte)0;
                        else if (parameter.getType() == short.class) resolvedParams[i] = (short)0;
                    }
                }
            }
        }
        
        return resolvedParams;
    }
    
    /**
     * Finds the index of a parameter in the command args
     */
    private int findParamIndex(Method method, String paramName, String[] args) {
        // If the method has a SubCommand annotation, use its pattern to find the parameter
        if (method.isAnnotationPresent(SubCommand.class)) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            String pattern = subCommand.command();
            String[] patternParts = pattern.split(" ");
            
            for (int i = 0; i < patternParts.length; i++) {
                String part = patternParts[i];
                if (part.startsWith("{") && part.endsWith("}")) {
                    String name = part.substring(1, part.length() - 1);
                    if (name.equals(paramName) && i < args.length) {
                        return i;
                    }
                }
            }
        }
        
        // If not found or no SubCommand annotation, return the first available arg index
        for (int i = 0; i < args.length; i++) {
            return i;
        }
        
        return -1;
    }
    
    /**
     * Resolves a parameter value from a string
     */
    private Object resolveParameter(Class<?> type, String value) {
        // Try to find a resolver for this type
        ParameterResolver<?> resolver = resolvers.get(type);
        if (resolver != null && resolver.supports(type)) {
            return resolver.resolve(value);
        }
        
        // Built-in conversions for common types
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0 : null;
            }
        } else if (type == double.class || type == Double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0.0 : null;
            }
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0L : null;
            }
        } else if (type == float.class || type == Float.class) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return type.isPrimitive() ? 0.0f : null;
            }
        } else if (type == Player.class) {
            return plugin.getServer().getPlayer(value);
        }
        
        // For other types, return null
        return null;
    }
}