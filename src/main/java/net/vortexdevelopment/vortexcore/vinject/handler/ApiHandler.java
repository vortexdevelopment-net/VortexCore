package net.vortexdevelopment.vortexcore.vinject.handler;

import net.vortexdevelopment.vinject.annotation.Registry;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vinject.di.DependencyRepository;
import net.vortexdevelopment.vinject.di.registry.AnnotationHandler;
import net.vortexdevelopment.vortexcore.vinject.annotation.Api;
import org.jetbrains.annotations.Nullable;

import java.text.Annotation;

/**
 * This class will handle Api load event emissions.
 */
@Registry(annotation = Api.class)
public class ApiHandler extends AnnotationHandler {

    @Override
    public void handle(Class<?> aClass, @Nullable Object o, DependencyContainer dependencyContainer) {
        if (o == null) {
            dependencyContainer.newInstance(aClass);
        }
        DependencyRepository.getInstance().emitEvent("vortexcore.api.load");
    }
}
