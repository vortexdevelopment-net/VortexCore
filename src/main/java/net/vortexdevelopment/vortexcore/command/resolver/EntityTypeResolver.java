package net.vortexdevelopment.vortexcore.command.resolver;

import net.vortexdevelopment.vortexcore.command.ParameterResolver;
import org.bukkit.entity.EntityType;

import java.util.Set;

public class EntityTypeResolver implements ParameterResolver<EntityType> {
    @Override
    public EntityType resolve(String input) {
        try {
            EntityType type = EntityType.valueOf(input.toUpperCase());
            return type;
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to resolve entity type: " + input);
            // Try a more forgiving approach
            for (EntityType type : EntityType.values()) {
                if (type.name().equalsIgnoreCase(input)) {
                    System.err.println("Found match using case-insensitive comparison: " + type);
                    return type;
                }
            }
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return EntityType.class.isAssignableFrom(type);
    }
    
    @Override
    public Set<Class<?>> getSupportedTypes() {
        return Set.of(EntityType.class);
    }
}