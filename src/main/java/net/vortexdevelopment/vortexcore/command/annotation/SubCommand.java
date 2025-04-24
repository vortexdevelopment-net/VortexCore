package net.vortexdevelopment.vortexcore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
    /**
     * The command pattern to match
     * Use {param} for parameters and static text for exact matches
     */
    String value();
    
    /**
     * Aliases for the command pattern
     * Each alias follows the same pattern format as the main command
     */
    String[] aliases() default {};
}