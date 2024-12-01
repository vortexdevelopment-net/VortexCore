package me.ceze88.vortexcore.compatibility;

import org.bukkit.Bukkit;

public enum ServerProject {
    UNKNOWN, CRAFTBUKKIT, SPIGOT, PAPER;
    private static final ServerProject serverProject = checkProject();

    private static ServerProject checkProject() {
        String serverPath = Bukkit.getServer().getClass().getName();
        try {
            Class.forName("com.destroystokyo.paperclip.Paperclip");
            return PAPER;
        } catch (ClassNotFoundException ignore) {
        }

        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return PAPER;
        } catch (ClassNotFoundException ignore) {
        }

        // spigot is the fork that pretty much all builds are based on anymore
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return SPIGOT;
        } catch (ClassNotFoundException ignore) {
        }

        return serverPath.contains("craftbukkit") ? CRAFTBUKKIT : UNKNOWN;
    }

    public static ServerProject getServerVersion() {
        return serverProject;
    }

    public static boolean isServer(ServerProject version) {
        return serverProject == version;
    }

    public static boolean isServer(ServerProject... versions) {
        for (ServerProject version : versions) {
            if (serverProject == version) {
                return true;
            }
        }
        return false;
    }
}
