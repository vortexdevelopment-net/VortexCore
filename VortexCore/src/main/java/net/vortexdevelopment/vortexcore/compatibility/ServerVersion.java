package net.vortexdevelopment.vortexcore.compatibility;

import org.bukkit.Bukkit;

public class ServerVersion {

    private static final String SERVER_VERSION;

    static {
        SERVER_VERSION = Bukkit.getMinecraftVersion();
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
}
