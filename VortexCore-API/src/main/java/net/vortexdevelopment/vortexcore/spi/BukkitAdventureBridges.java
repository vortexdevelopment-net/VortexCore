package net.vortexdevelopment.vortexcore.spi;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class BukkitAdventureBridges {

    private static volatile BukkitAdventureBridge bridge;

    private BukkitAdventureBridges() {
    }

    public static void install(BukkitAdventureBridge bukkitAdventureBridge) {
        BukkitAdventureBridges.bridge = bukkitAdventureBridge;
    }

    /**
     * Installs the Paper/Spigot bridge before Vinject runs {@code @PostConstruct}, so YAML config mapping
     * (e.g. {@code ItemStack} deserialization) can use {@link #get()} while the Vinject
     * {@code DependencyContainer} is still being constructed.
     * <p>
     * Tries, in order: {@code {pluginMainPackage}.core.platform.spigot.SpigotBukkitAdventureBridge},
     * {@code {pluginMainPackage}.core.platform.paper.PaperBukkitAdventureBridge} (shaded core under the usual
     * {@code vortexcore} → {@code … .core} relocation), then the unshaded {@code net.vortexdevelopment.vortexcore}
     * platform classes.
     */
    public static void installEarlyIfAbsent(Class<?> pluginMainClass) {
        if (bridge != null) {
            return;
        }
        String base = pluginMainClass.getPackageName();
        ClassLoader loader = pluginMainClass.getClassLoader();
        String[] candidates = {
                base + ".core.platform.spigot.SpigotBukkitAdventureBridge",
                base + ".core.platform.paper.PaperBukkitAdventureBridge",
                "net.vortexdevelopment.vortexcore.platform.spigot.SpigotBukkitAdventureBridge",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperBukkitAdventureBridge"
        };
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!BukkitAdventureBridge.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method register = c.getMethod("registerBridge");
                register.invoke(instance);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * @return the installed bridge, or {@code null} before DI has run (e.g. early {@code onEnable} / {@code onLoad})
     */
    public static @Nullable BukkitAdventureBridge getOrNull() {
        return bridge;
    }

    public static BukkitAdventureBridge get() {
        BukkitAdventureBridge b = bridge;
        if (b == null) {
            throw new IllegalStateException(
                    "BukkitAdventureBridge not installed (use VortexCore-Paper or VortexCore-Spigot)");
        }
        return b;
    }
}
