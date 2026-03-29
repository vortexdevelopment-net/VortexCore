package net.vortexdevelopment.vortexcore.spi;

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

    public static SkullProfileService get() {
        SkullProfileService s = service;
        if (s == null) {
            throw new IllegalStateException(
                    "SkullProfileService not installed (use VortexCore-Paper or VortexCore-Spigot)");
        }
        return s;
    }
}
