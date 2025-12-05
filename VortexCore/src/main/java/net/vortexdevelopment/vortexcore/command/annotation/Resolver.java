package net.vortexdevelopment.vortexcore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a parameter resolver for automatic registration.
 * <p>
 * Classes annotated with {@code @Resolver} that implement {@link net.vortexdevelopment.vortexcore.command.ParameterResolver}
 * will be automatically discovered and registered with the {@link net.vortexdevelopment.vortexcore.command.CommandManager}
 * during plugin initialization.
 * <p>
 * Example:
 * <pre>{@code
 * @Resolver
 * public class PlayerResolver implements ParameterResolver<Player> {
 *     @Override
 *     public Player resolve(String input) {
 *         return Bukkit.getPlayer(input);
 *     }
 *
 *     @Override
 *     public boolean supports(Class<?> type) {
 *         return Player.class.isAssignableFrom(type);
 *     }
 *
 *     @Override
 *     public Set<Class<?>> getSupportedTypes() {
 *         return Set.of(Player.class);
 *     }
 * }
 * }</pre>
 *
 * @see net.vortexdevelopment.vortexcore.command.ParameterResolver
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolver {
}
