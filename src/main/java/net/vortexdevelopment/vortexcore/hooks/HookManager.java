package net.vortexdevelopment.vortexcore.hooks;

import net.vortexdevelopment.vortexcore.hooks.holo.HologramHook;

public class HookManager<T> {

    private Object hook;

    public HookManager(Object hook) {
        this.hook = hook;
    }

    public static HookManager<HologramHook> HOLOGRAM;

    static {
        HOLOGRAM = new HookManager<>(new HologramHook());
    }

    public T getHook() {
        return (T) hook;
    }

}
