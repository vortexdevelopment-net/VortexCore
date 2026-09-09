package net.vortexdevelopment.vortexcore.compatibility;

import org.bukkit.Bukkit;

import java.util.Locale;

/**
 * The server implementation detected at runtime.
 *
 * <p>VortexCore is compiled against Paper. Bukkit and Spigot are retained here
 * only so that the plugin can report a clear unsupported-platform message and
 * stop before loading any Paper-specific code.</p>
 */
public enum ServerProject {
    UNKNOWN,
    BUKKIT,
    /** @deprecated Use {@link #BUKKIT}. */
    @Deprecated
    CRAFTBUKKIT,
    SPIGOT,
    PAPER,
    PURPUR,
    LEAF,
    PUFFERFISH,
    FOLIA;

    private static final ServerProject SERVER_PROJECT = checkProject();

    private static ServerProject checkProject() {
        String serverClassName = Bukkit.getServer().getClass().getName();
        String identity = (Bukkit.getName() + " " + Bukkit.getVersion() + " "
                + Bukkit.getBukkitVersion() + " " + serverClassName).toLowerCase(Locale.ROOT);

        // Folia is a Paper fork and must be checked before generic Paper detection.
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer") || identity.contains("folia")) {
            return FOLIA;
        }
        if (identity.contains("purpur") || hasClass("org.purpurmc.purpur.PurpurConfig")) {
            return PURPUR;
        }
        if (identity.contains("pufferfish") || hasClass("org.pufferfish.pufferfish.PufferfishConfig")) {
            return PUFFERFISH;
        }
        if (identity.contains("leaf") || hasClass("org.leavesmc.leaves.LeavesConfig")
                || hasClass("org.leafmc.leaf.LeafConfig")) {
            return LEAF;
        }
        if (identity.contains("paper") || hasClass("com.destroystokyo.paperclip.Paperclip")
                || hasClass("com.destroystokyo.paper.PaperConfig")) {
            return PAPER;
        }
        if (identity.contains("spigot") || hasClass("org.spigotmc.SpigotConfig")) {
            return SPIGOT;
        }
        if (identity.contains("craftbukkit") || identity.contains("bukkit")) {
            return BUKKIT;
        }
        return UNKNOWN;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className, false, ServerProject.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Returns the detected server project. The historical {@code getServerVersion}
     * method remains available as a source-compatible alias.
     */
    public static ServerProject getServerProject() {
        return SERVER_PROJECT;
    }

    public static ServerProject getServerVersion() {
        return getServerProject();
    }

    public static boolean isServer(ServerProject version) {
        return SERVER_PROJECT == version;
    }

    public static boolean isServer(ServerProject... versions) {
        for (ServerProject version : versions) {
            if (SERVER_PROJECT == version) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnsupported() {
        return isServer(BUKKIT, CRAFTBUKKIT, SPIGOT);
    }

    /**
     * Returns whether VortexCore should use its Paper implementation classes.
     * Unknown projects are allowed to try the Paper path so new Paper forks can
     * work before they receive an explicit detector entry.
     */
    public static boolean isPaperCompatible() {
        return !isUnsupported();
    }

    public static boolean isFolia() {
        return SERVER_PROJECT == FOLIA;
    }
}
