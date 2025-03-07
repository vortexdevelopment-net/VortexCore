package me.ceze88.vortexcore.vinject.templates;

import net.vortexdevelopment.vinject.annotation.RegisterTemplate;

@RegisterTemplate(
        annotationFqcn = "me.ceze88.vortexcore.vinject.annotation.RegisterCommand",
        resource = "RegisterCommandTemplate.java.ft",
        name = "Minecraft Listener"
)
public class RegisterCommandTemplate {
}
