package net.vortexdevelopment.vortexcore.command;

import java.util.Set;

public interface ParameterResolver<T> {
    /**
     * Resolve the input string to the target type
     * @param input The input string to resolve
     * @return The resolved object, or null if resolution failed
     */
    T resolve(String input);
    
    /**
     * Check if this resolver supports the given type
     * @param type The type to check
     * @return True if this resolver supports the type
     */
    boolean supports(Class<?> type);
    
    /**
     * Get all types supported by this resolver
     * @return A set of supported types
     */
     Set<Class<?>> getSupportedTypes();
}