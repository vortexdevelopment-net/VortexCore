package net.vortexdevelopment.vortexcore.spi;

import java.lang.reflect.Method;

/**
 * Active {@link SkullProfileService} for the current artifact (Paper or Spigot).
 */
public final class SkullProfiles {

    private static volatile SkullProfileService service;

    private SkullProfiles() {
    }

    public static void install(SkullProfileService skullProfileService) {
        SkullProfiles.service = skullProfileService;
    }

    /**
     * Installs the Paper/Spigot {@link SkullProfileService} before Vinject runs {@code @PostConstruct}, so YAML
     * config mapping (e.g. {@code ItemStack} / skull deserialization in {@code ItemStackSerializer}) can use
     * {@link #get()} while the {@code DependencyContainer} is still being constructed.
     * <p>
     * Tries, in order: {@code {pluginMainPackage}.core.platform.spigot.SpigotSkullProfileService},
     * {@code {pluginMainPackage}.core.platform.paper.PaperSkullProfileService} (shaded core), then unshaded
     * {@code net.vortexdevelopment.vortexcore} platform classes.
     *
     * @return {@code true} if a service is installed after this call (including if already installed)
     */
    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass) {
        if (service != null) {
            return true;
        }
        String base = pluginMainClass.getPackageName();
        ClassLoader loader = pluginMainClass.getClassLoader();
        String[] candidates = {
                base + ".core.platform.spigot.SpigotSkullProfileService",
                base + ".core.platform.paper.PaperSkullProfileService",
                "net.vortexdevelopment.vortexcore.platform.spigot.SpigotSkullProfileService",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperSkullProfileService"
        };
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!SkullProfileService.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method register = c.getMethod("registerSkullProfile");
                register.invoke(instance);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return service != null;
    }

    public static SkullProfileService get() {
        SkullProfileService s = service;
        if (s == null) {
            throw new IllegalStateException(
                    "SkullProfileService not installed (use VortexCore-Paper or VortexCore-Spigot)");
        }
        return s;
    }
}
