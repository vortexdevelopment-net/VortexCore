package net.vortexdevelopment.vortexcore.spi;

import net.vortexdevelopment.vortexcore.compatibility.ServerProject;

import java.lang.reflect.Method;

/**
 * Active {@link SkullProfileService} for the current artifact (Paper or Spigot).
 */
public final class SkullProfiles {

    private static volatile SkullProfileService service;

    private SkullProfiles() {
    }

    public static void install(SkullProfileService skullProfileService) {
        boolean expectPaper = ServerProject.isServer(ServerProject.PAPER);
        if (!isCompatiblePlatform(skullProfileService, expectPaper)) {
            throw new IllegalStateException("Incorrect SkullProfileService for runtime: expected "
                    + expectedPlatform(expectPaper) + " but got " + skullProfileService.getClass().getName());
        }
        if (service != null && !isCompatiblePlatform(service, expectPaper)) {
            throw new IllegalStateException("An incompatible SkullProfileService was already installed: "
                    + service.getClass().getName());
        }
        SkullProfiles.service = skullProfileService;
    }

    private static boolean isPaperService(SkullProfileService skullProfileService) {
        return skullProfileService != null
                && skullProfileService.getClass().getName().contains(".platform.paper.");
    }

    private static boolean isSpigotService(SkullProfileService skullProfileService) {
        return skullProfileService != null
                && skullProfileService.getClass().getName().contains(".platform.spigot.");
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
        boolean preferPaper = ServerProject.isServer(ServerProject.PAPER);
        String[] candidates = serviceCandidates(base, preferPaper);
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
        throw new IllegalStateException("Could not install a compatible SkullProfileService for runtime "
                + expectedPlatform(preferPaper));
    }

    private static String[] serviceCandidates(String base, boolean preferPaper) {
        if (preferPaper) {
            return new String[] {
                    base + ".core.platform.paper.PaperSkullProfileService",
                    base + ".core.platform.spigot.SpigotSkullProfileService",
                    "net.vortexdevelopment.vortexcore.platform.paper.PaperSkullProfileService",
                    "net.vortexdevelopment.vortexcore.platform.spigot.SpigotSkullProfileService"
            };
        }
        return new String[] {
                base + ".core.platform.spigot.SpigotSkullProfileService",
                base + ".core.platform.paper.PaperSkullProfileService",
                "net.vortexdevelopment.vortexcore.platform.spigot.SpigotSkullProfileService",
                "net.vortexdevelopment.vortexcore.platform.paper.PaperSkullProfileService"
        };
    }

    public static SkullProfileService get() {
        SkullProfileService s = service;
        if (s == null) {
            throw new IllegalStateException(
                    "SkullProfileService not installed (use the unified VortexCore runtime artifact)");
        }
        boolean expectPaper = ServerProject.isServer(ServerProject.PAPER);
        if (!isCompatiblePlatform(s, expectPaper)) {
            throw new IllegalStateException("Incorrect SkullProfileService installed for runtime: expected "
                    + expectedPlatform(expectPaper) + " but got " + s.getClass().getName());
        }
        return s;
    }

    private static boolean isCompatiblePlatform(SkullProfileService skullProfileService, boolean expectPaper) {
        return expectPaper ? isPaperService(skullProfileService) : isSpigotService(skullProfileService);
    }

    private static String expectedPlatform(boolean expectPaper) {
        return expectPaper ? "paper" : "spigot";
    }
}
