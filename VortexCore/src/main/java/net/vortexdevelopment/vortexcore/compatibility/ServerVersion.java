package net.vortexdevelopment.vortexcore.compatibility;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerVersion {

    private static final Pattern MC_VERSION_IN_PARENS = Pattern.compile("\\(MC: ([0-9]+(?:\\.[0-9]+)*)\\)");

    private static final String SERVER_VERSION;
    private static final KnownServerVersions CURRENT_VERSION;

    static {
        SERVER_VERSION = resolveMinecraftVersion();
        CURRENT_VERSION = init();
    }

    /**
     * Resolves the Minecraft version without directly linking to Paper-only API
     * methods, keeping the version check safe during early plugin startup.
     */
    private static String resolveMinecraftVersion() {
        try {
            return (String) Bukkit.class.getMethod("getMinecraftVersion").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Older Paper implementations do not expose this method.
        }
        String bukkitVersion = Bukkit.getBukkitVersion();
        int releaseMarker = bukkitVersion.indexOf("-R");
        if (releaseMarker > 0) {
            return bukkitVersion.substring(0, releaseMarker);
        }
        Matcher matcher = MC_VERSION_IN_PARENS.matcher(Bukkit.getVersion());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return bukkitVersion;
    }

    private static KnownServerVersions init() {
        for (KnownServerVersions version : KnownServerVersions.values()) {
            if (SERVER_VERSION.equals(version.getVersionString())) {
                return version;
            }
        }
        return null;
    }

    public static boolean isAtLeastVersion(String version) {
        return isVersionAtLeast(SERVER_VERSION, version);
    }

    public static boolean isVersionAtLeast(String version, String minimumVersion) {
        return compareVersions(version, minimumVersion) >= 0;
    }

    public static boolean isVersionAtLeast(
            KnownServerVersions version,
            KnownServerVersions minimumVersion) {
        return isVersionAtLeast(version.getVersionString(), minimumVersion.getVersionString());
    }

    private static int compareVersions(String first, String second) {
        if (first == null || second == null || !first.matches("[0-9]+(?:\\.[0-9]+)*")
                || !second.matches("[0-9]+(?:\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid Minecraft version: " + first + " / " + second);
        }
        String[] firstParts = first.split("\\.");
        String[] secondParts = second.split("\\.");
        int length = Math.max(firstParts.length, secondParts.length);
        for (int i = 0; i < length; i++) {
            int firstPart = i < firstParts.length ? Integer.parseInt(firstParts[i]) : 0;
            int secondPart = i < secondParts.length ? Integer.parseInt(secondParts[i]) : 0;
            if (firstPart != secondPart) {
                return Integer.compare(firstPart, secondPart);
            }
        }
        return 0;
    }

    public static boolean isAtLeastVersion(KnownServerVersions version) {
        return isAtLeastVersion(version.getVersionString());
    }

    public static boolean isCurrentVersionFullySupported() {
        return CURRENT_VERSION != null;
    }

    public static boolean isItemComponentsAvailable() {
        return isAtLeastVersion("1.21");
    }

    /**
     * {@code minecraft:tooltip_style} data component is available from 1.21.2+.
     */
    public static boolean isTooltipStyleSupported() {
        return isAtLeastVersion("1.21.2");
    }

    public static KnownServerVersions getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static String getVersionString() {
        return SERVER_VERSION;
    }
}
