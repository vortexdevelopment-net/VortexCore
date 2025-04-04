package net.vortexdevelopment.vortexcore.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabComplete {
    /**
     * The command pattern to match for tab completion
     * Use {param} for parameters and static text for exact matches
     */
    String command() default "";
    
    /**
     * The parameter name to complete
     * If specified, will complete the parameter with this name
     */
    String param() default "";
    
    /**
     * The argument index to complete (0-based)
     * If not specified, will complete the last argument
     */
    int argIndex() default -1;
}