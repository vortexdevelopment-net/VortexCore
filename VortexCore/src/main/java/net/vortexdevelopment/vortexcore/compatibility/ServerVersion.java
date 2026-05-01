package net.vortexdevelopment.vortexcore.compatibility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;

public class ServerVersion {

    private static final Pattern MC_VERSION_IN_PARENS = Pattern.compile("\\(MC: ([0-9]+(?:\\.[0-9]+)*)\\)");

    private static final String SERVER_VERSION;
    private static final KnownServerVersions CURRENT_VERSION;

    static {
        SERVER_VERSION = resolveMinecraftVersion();
        CURRENT_VERSION = init();
    }

    /**
     * Paper exposes {@code Bukkit.getMinecraftVersion()}; Spigot does not. Using the method
     * directly causes {@link NoSuchMethodError} on Spigot at class-init time when compiled
     * against the Paper API, so we resolve via reflection with fallbacks.
     */
    private static String resolveMinecraftVersion() {
        try {
            return (String) Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Spigot and older Paper
        }
        String bukkitVersion = Bukkit.getBukkitVersion();
        int r = bukkitVersion.indexOf("-R");
        if (r > 0) {
            return bukkitVersion.substring(0, r);
        }
        Matcher m = MC_VERSION_IN_PARENS.matcher(Bukkit.getVersion());
        if (m.find()) {
            return m.group(1);
        }
        return bukkitVersion;
    }

    private static KnownServerVersions init() {
        for (KnownServerVersions version : KnownServerVersions.values()) {
            if (SERVER_VERSION.equals(version.getVersionString())) {
                return version;
            }
        }
        return null; // Unknown version
    }

    public static boolean isAtLeastVersion(String version) {
        String[] currentParts = SERVER_VERSION.split("\\.");
        String[] targetParts = version.split("\\.");

        int length = Math.max(currentParts.length, targetParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int targetPart = i < targetParts.length ? Integer.parseInt(targetParts[i]) : 0;

            if (currentPart < targetPart) {
                return false;
            } else if (currentPart > targetPart) {
                return true;
            }
        }
        return true; // Versions are equal
    }

    public static boolean isAtLeastVersion(KnownServerVersions version) {
        return isAtLeastVersion(version.getVersionString());
    }

    public static boolean isCurrentVersionFullySupported() {
        for (KnownServerVersions version : KnownServerVersions.values()) {
            if (SERVER_VERSION.equals(version.getVersionString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isItemComponentsAvailable() {
        return isAtLeastVersion("1.21");
    }

    /**
     * {@code minecraft:tooltip_style} data component (Paper item stacks) is available from 1.21.2+.
     */
    public static boolean isTooltipStyleSupported() {
        return isAtLeastVersion("1.21.2");
    }

    public static KnownServerVersions getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
