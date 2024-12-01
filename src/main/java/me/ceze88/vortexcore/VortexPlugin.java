package me.ceze88.vortexcore;

import me.ceze88.vortexcore.database.DataManager;
import me.ceze88.vortexcore.database.DataMigration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class VortexPlugin extends JavaPlugin {

    public static final String DOWNLOAD_ID;

    static {
        System.setProperty("org.jooq.no-tips", "true");
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport", "ERROR");
        DOWNLOAD_ID = "%%DOWNLOAD_ID%%";
    }

    private static VortexPlugin instance;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        instance = this;
        VortexCore.setPlugin(this);
        this.dataManager = new DataManager(this);
        getLogger().info(ChatColor.GREEN + "===================");
        getLogger().info("Enabling " + getDescription().getName() + " v" + getDescription().getVersion());
        onPluginEnable();
        getLogger().info("§aEnabled successfully!");
        getLogger().info(ChatColor.GREEN + "===================");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "===================");
        getLogger().info("Disabling " + getDescription().getName() + " v" + getDescription().getVersion());
        Bukkit.getScheduler().cancelTasks(this); //Make sure all tasks are cancelled
        onPluginDisable();
        getLogger().info("§cDisabled successfully!");
        getLogger().info(ChatColor.RED + "===================");
    }

    protected abstract void onPluginEnable();

    protected abstract void onPluginDisable();

    public static VortexPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    protected void initDatabase(DataMigration... migrations) {
        dataManager.init(migrations);
    }
}
