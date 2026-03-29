package net.vortexdevelopment.vortexcore.spi;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CommandMaps {

    private static volatile CommandMapBridge bridge;

    private CommandMaps() {
    }

    public static void install(CommandMapBridge commandMapBridge) {
        CommandMaps.bridge = commandMapBridge;
    }

    /**
     * Installs the Paper/Spigot {@link CommandMapBridge} before Vinject runs {@code @PostConstruct}, so dynamic
     * command registration works while the {@code DependencyContainer} is still being built (ordering-safe for the
     * core plugin). Child plugins that shade this API into their own JAR should still use {@link #register}'s fallback
     * or depend on VortexCore with {@code provided} scope so only one {@code CommandMaps} class exists.
     */
    /**
     * @return {@code true} if a bridge is installed after this call (including if it was already installed)
     */
    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass) {
        if (bridge != null) {
            return true;
        }
        String base = pluginMainClass.getPackageName();
        ClassLoader loader = pluginMainClass.getClassLoader();
        String[] candidates = {
                base + ".core.platform.spigot.SpigotCommandMapBridge",
                base + ".core.platform.paper.PaperCommandMapBridge",
                "net.vortexdevelopment.vortexcore.platform.spigot.SpigotCommandMapBridge",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperCommandMapBridge"
        };
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!CommandMapBridge.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method registerBridge = c.getMethod("registerBridge");
                registerBridge.invoke(instance);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return bridge != null;
    }

    public static void register(Plugin plugin, PluginCommand command) {
        CommandMapBridge b = bridge;
        if (b == null) {
            // Retry: fat jars that relocate vortexcore rely on this; onEnable may have run with a different state.
            installEarlyIfAbsent(plugin.getClass());
            b = bridge;
        }
        if (b != null) {
            b.register(plugin, command);
            return;
        }
        if (registerViaHostPluginCommandMaps(plugin, command)) {
            return;
        }
        throw new IllegalStateException(
                "CommandMapBridge not installed (use VortexCore-Paper or VortexCore-Spigot, and avoid shading "
                        + "net.vortexdevelopment.vortexcore into your plugin JAR — use compileOnly/provided for "
                        + "VortexCore-API)");
    }

    /**
     * When this API is duplicated across plugin class loaders, {@link #install} ran on VortexCore's {@code CommandMaps}
     * but the caller uses another copy with a null bridge. Delegate to any enabled plugin whose {@code CommandMaps}
     * has a non-null bridge (the VortexCore distribution).
     */
    private static boolean registerViaHostPluginCommandMaps(Plugin plugin, PluginCommand command) {
        for (Plugin host : Bukkit.getPluginManager().getPlugins()) {
            if (host == null || !host.isEnabled()) {
                continue;
            }
            ClassLoader cl = host.getClass().getClassLoader();
            try {
                Class<?> mapsClass = loadCommandMapsClass(host, cl);
                if (mapsClass == null) {
                    continue;
                }
                Field bridgeField = mapsClass.getDeclaredField("bridge");
                bridgeField.setAccessible(true);
                Object foreignBridge = bridgeField.get(null);
                if (foreignBridge == null) {
                    continue;
                }
                Method register = mapsClass.getMethod("register", Plugin.class, PluginCommand.class);
                register.invoke(null, plugin, command);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * Resolves {@code CommandMaps} for a plugin class loader: canonical VortexCore API, or a shaded copy at
     * {@code {pluginMainPackage}.core.spi.CommandMaps} (e.g. VortexVouchers relocates {@code vortexcore}).
     */
    private static @Nullable Class<?> loadCommandMapsClass(Plugin host, ClassLoader cl) {
        try {
            return Class.forName("net.vortexdevelopment.vortexcore.spi.CommandMaps", false, cl);
        } catch (ClassNotFoundException ignored) {
        }
        String relocated = host.getClass().getPackageName() + ".core.spi.CommandMaps";
        try {
            return Class.forName(relocated, false, cl);
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }
}
