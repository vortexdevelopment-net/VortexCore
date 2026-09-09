package net.vortexdevelopment.vortexcore.spi;

import net.vortexdevelopment.vortexcore.compatibility.ServerProject;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class BukkitAdventureBridges {

    private static volatile BukkitAdventureBridge bridge;

    private BukkitAdventureBridges() {
    }

    public static void install(BukkitAdventureBridge bukkitAdventureBridge) {
        if (!isCompatiblePlatform(bukkitAdventureBridge)) {
            throw new IllegalStateException("Incorrect BukkitAdventureBridge for runtime: expected paper-compatible "
                    + "but got " + bukkitAdventureBridge.getClass().getName());
        }
        if (bridge != null && !isCompatiblePlatform(bridge)) {
            throw new IllegalStateException("An incompatible BukkitAdventureBridge was already installed: "
                    + bridge.getClass().getName());
        }
        BukkitAdventureBridges.bridge = bukkitAdventureBridge;
    }

    private static boolean isPaperBridge(BukkitAdventureBridge bukkitAdventureBridge) {
        return bukkitAdventureBridge != null
                && bukkitAdventureBridge.getClass().getName().contains(".platform.paper.");
    }

    /**
     * Installs the Paper bridge before Vinject runs {@code @PostConstruct}.
     * The package is supplied by {@code VortexPlugin} so relocation remains
     * transparent to the final plugin consumer.
     */
    public static void installEarlyIfAbsent(Class<?> pluginMainClass) {
        installEarlyIfAbsent(pluginMainClass, "net.vortexdevelopment.vortexcore");
    }

    public static void installEarlyIfAbsent(Class<?> pluginMainClass, String vortexCorePackage) {
        if (bridge != null) {
            return;
        }
        ClassLoader loader = pluginMainClass.getClassLoader();
        for (String name : bridgeCandidates(pluginMainClass.getPackageName(), vortexCorePackage)) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (!BukkitAdventureBridge.class.isAssignableFrom(type)) {
                    continue;
                }
                Object instance = type.getDeclaredConstructor().newInstance();
                Method register = type.getMethod("registerBridge");
                register.invoke(instance);
                if (bridge != null) {
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new IllegalStateException("Could not install a compatible BukkitAdventureBridge for runtime "
                + ServerProject.getServerProject());
    }

    private static String[] bridgeCandidates(String pluginPackage, String vortexCorePackage) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, vortexCorePackage + ".platform.paper.PaperBukkitAdventureBridge");
        addCandidate(candidates, pluginPackage + ".core.platform.paper.PaperBukkitAdventureBridge");
        addCandidate(candidates, "net.vortexdevelopment.vortexcore.platform.paper.PaperBukkitAdventureBridge");
        return candidates.toArray(new String[0]);
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    /**
     * @return the installed bridge, or {@code null} before DI has run
     */
    public static @Nullable BukkitAdventureBridge getOrNull() {
        return bridge;
    }

    public static BukkitAdventureBridge get() {
        BukkitAdventureBridge installed = bridge;
        if (installed == null) {
            throw new IllegalStateException(
                    "BukkitAdventureBridge not installed (use the unified VortexCore runtime artifact)");
        }
        if (!isCompatiblePlatform(installed)) {
            throw new IllegalStateException("Incorrect BukkitAdventureBridge installed for runtime: expected "
                    + "paper-compatible but got " + installed.getClass().getName());
        }
        return installed;
    }

    private static boolean isCompatiblePlatform(BukkitAdventureBridge bukkitAdventureBridge) {
        return ServerProject.isPaperCompatible() && isPaperBridge(bukkitAdventureBridge);
    }
}
