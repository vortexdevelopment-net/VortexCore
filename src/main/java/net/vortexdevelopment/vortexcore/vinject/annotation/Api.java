package net.vortexdevelopment.vortexcore.vinject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as an API component. When the class is created, it will emit "vortexcore.api.load" event.
 * This annotation is used to register the class as a component in the VortexCore dependency injection framework.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Api {
}
