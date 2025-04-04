package net.vortexdevelopment.vortexcore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * The name of the command
     */
    String value();
    
    /**
     * Aliases for the command
     */
    String[] aliases() default {};
}