package net.vortexdevelopment.vortexcore.spi;

import net.vortexdevelopment.vortexcore.compatibility.ServerProject;

import java.lang.reflect.Method;

/**
 * Active {@link SkullProfileService} for the Paper-compatible runtime.
 */
public final class SkullProfiles {

    private static volatile SkullProfileService service;

    private SkullProfiles() {
    }

    public static void install(SkullProfileService skullProfileService) {
        if (!isCompatiblePlatform(skullProfileService)) {
            throw new IllegalStateException("Incorrect SkullProfileService for runtime: expected "
                    + "paper-compatible but got " + skullProfileService.getClass().getName());
        }
        if (service != null && !isCompatiblePlatform(service)) {
            throw new IllegalStateException("An incompatible SkullProfileService was already installed: "
                    + service.getClass().getName());
        }
        SkullProfiles.service = skullProfileService;
    }

    private static boolean isPaperService(SkullProfileService skullProfileService) {
        return skullProfileService != null
                && skullProfileService.getClass().getName().contains(".platform.paper.");
    }

    /**
     * Installs the Paper {@link SkullProfileService} before Vinject runs {@code @PostConstruct}, so YAML
     * config mapping (e.g. {@code ItemStack} / skull deserialization in {@code ItemStackSerializer}) can use
     * {@link #get()} while the {@code DependencyContainer} is still being constructed.
     * <p>
     * Tries the relocated VortexCore package, the conventional plugin core
     * package, and finally the unshaded VortexCore package.
     *
     * @return {@code true} if a service is installed after this call (including if already installed)
     */
    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass) {
        return installEarlyIfAbsent(pluginMainClass, "net.vortexdevelopment.vortexcore");
    }

    public static boolean installEarlyIfAbsent(Class<?> pluginMainClass, String vortexCorePackage) {
        if (service != null) {
            return true;
        }
        String base = pluginMainClass.getPackageName();
        ClassLoader loader = pluginMainClass.getClassLoader();
        String[] candidates = serviceCandidates(base, vortexCorePackage);
        for (String name : candidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                if (!SkullProfileService.class.isAssignableFrom(c)) {
                    continue;
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                Method register = c.getMethod("registerSkullProfile");
                register.invoke(instance);
                if (service != null) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new IllegalStateException("Could not install a compatible Paper SkullProfileService for runtime "
                + ServerProject.getServerProject());
    }

    private static String[] serviceCandidates(String base, String vortexCorePackage) {
        return new String[]{
                vortexCorePackage + ".platform.paper.PaperSkullProfileService",
                base + ".core.platform.paper.PaperSkullProfileService",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperSkullProfileService"
        };
    }

    public static SkullProfileService get() {
        SkullProfileService s = service;
        if (s == null) {
            throw new IllegalStateException(
                    "SkullProfileService not installed (use the unified VortexCore runtime artifact)");
        }
        if (!isCompatiblePlatform(s)) {
            throw new IllegalStateException("Incorrect SkullProfileService installed for runtime: expected "
                    + "paper-compatible but got " + s.getClass().getName());
        }
        return s;
    }

    private static boolean isCompatiblePlatform(SkullProfileService skullProfileService) {
        return ServerProject.isPaperCompatible() && isPaperService(skullProfileService);
    }
}
