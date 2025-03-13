package net.vortexdevelopment.vortexcore.hooks;

public interface PluginHook {

    String getIdentifier();

    boolean isEnabled();

    void onEnable();

    void onDisable();
}
