package me.ceze88.vortexcore.hooks;

public interface PluginHook {

    String getIdentifier();

    boolean isEnabled();

    void onEnable();

    void onDisable();
}
