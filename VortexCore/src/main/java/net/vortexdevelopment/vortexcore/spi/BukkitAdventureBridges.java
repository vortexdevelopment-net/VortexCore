package net.vortexdevelopment.vortexcore.spi;

import net.vortexdevelopment.vortexcore.compatibility.ServerProject;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class BukkitAdventureBridges {

    private static volatile BukkitAdventureBridge bridge;

    private BukkitAdventureBridges() {
    }

    public static void install(BukkitAdventureBridge bukkitAdventureBridge) {
        boolean expectPaper = ServerProject.isServer(ServerProject.PAPER);
        if (!isCompatiblePlatform(bukkitAdventureBridge, expectPaper)) {
            throw new IllegalStateException("Incorrect BukkitAdventureBridge for runtime: expected "
                    + expectedPlatform(expectPaper) + " but got " + bukkitAdventureBridge.getClass().getName());
        }
        if (bridge != null && !isCompatiblePlatform(bridge, expectPaper)) {
            throw new IllegalStateException("An incompatible BukkitAdventureBridge was already installed: "
                    + bridge.getClass().getName());
        }
        BukkitAdventureBridges.bridge = bukkitAdventureBridge;
    }

    private static boolean isPaperBridge(BukkitAdventureBridge bukkitAdventureBridge) {
        return bukkitAdventureBridge != null
                && bukkitAdventureBridge.getClass().getName().contains(".platform.paper.");
    }

    private static boolean isSpigotBridge(BukkitAdventureBridge bukkitAdventureBridge) {
        return bukkitAdventureBridge != null
                && bukkitAdventureBridge.getClass().getName().contains(".platform.spigot.");
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
        boolean preferPaper = ServerProject.isServer(ServerProject.PAPER);
        String[] candidates = bridgeCandidates(base, preferPaper);
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!BukkitAdventureBridge.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method register = c.getMethod("registerBridge");
                register.invoke(instance);
                if (bridge != null) {
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new IllegalStateException("Could not install a compatible BukkitAdventureBridge for runtime "
                + expectedPlatform(preferPaper));
    }

    private static String[] bridgeCandidates(String base, boolean preferPaper) {
        if (preferPaper) {
            return new String[] {
                    base + ".core.platform.paper.PaperBukkitAdventureBridge",
                    base + ".core.platform.spigot.SpigotBukkitAdventureBridge",
                    "net.vortexdevelopment.vortexcore.platform.paper.PaperBukkitAdventureBridge",
                    "net.vortexdevelopment.vortexcore.platform.spigot.SpigotBukkitAdventureBridge"
            };
        }
        return new String[] {
                base + ".core.platform.spigot.SpigotBukkitAdventureBridge",
                base + ".core.platform.paper.PaperBukkitAdventureBridge",
                "net.vortexdevelopment.vortexcore.platform.spigot.SpigotBukkitAdventureBridge",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperBukkitAdventureBridge"
        };
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
                    "BukkitAdventureBridge not installed (use the unified VortexCore runtime artifact)");
        }
        boolean expectPaper = ServerProject.isServer(ServerProject.PAPER);
        if (!isCompatiblePlatform(b, expectPaper)) {
            throw new IllegalStateException("Incorrect BukkitAdventureBridge installed for runtime: expected "
                    + expectedPlatform(expectPaper) + " but got " + b.getClass().getName());
        }
        return b;
    }

    private static boolean isCompatiblePlatform(BukkitAdventureBridge bukkitAdventureBridge, boolean expectPaper) {
        return expectPaper ? isPaperBridge(bukkitAdventureBridge) : isSpigotBridge(bukkitAdventureBridge);
    }

    private static String expectedPlatform(boolean expectPaper) {
        return expectPaper ? "paper" : "spigot";
    }
}
