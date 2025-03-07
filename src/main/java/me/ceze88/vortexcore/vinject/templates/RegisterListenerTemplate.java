package me.ceze88.vortexcore.vinject.templates;

import net.vortexdevelopment.vinject.annotation.RegisterTemplate;

@RegisterTemplate(
        annotationFqcn = "me.ceze88.vortexcore.vinject.annotation.RegisterListener",
        resource = "RegisterListenerTemplate.java.ft",
        name = "Minecraft Command"
)
public class RegisterListenerTemplate {
}
