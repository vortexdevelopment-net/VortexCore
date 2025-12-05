package net.vortexdevelopment.vortexcore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify permission requirements for commands, subcommands, or tab completers.
 * Can be applied at both class and method levels.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Permission {
    /**
     * The permission node required to execute this command or subcommand.
     * @return The permission node string
     */
    String value();
    
    /**
     * Message to display when permission is denied.
     * If empty, a default message will be used.
     * @return The permission denied message
     */
    String message() default "";
}