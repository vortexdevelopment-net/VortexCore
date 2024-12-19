package me.ceze88.vortexcore;

import me.ceze88.vortexcore.config.Config;
import me.ceze88.vortexcore.database.DataManager;
import me.ceze88.vortexcore.database.DataMigration;
import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.Component;
import net.vortexdevelopment.vinject.annotation.Root;

import net.vortexdevelopment.vinject.database.Database;
import net.vortexdevelopment.vinject.database.repository.RepositoryContainer;
import net.vortexdevelopment.vinject.di.DependencyContainer;
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
    @Deprecated
    private DataManager dataManager;

    private DependencyContainer dependencyContainer;
    private RepositoryContainer repositoryContainer;
    private Database database;

    @Override
    public final void onLoad() {
        onPluginLoad();
    }

    @Override
    public final void onEnable() {
        getLogger().info(ChatColor.GREEN + "===================");
        getLogger().info("Enabling " + getDescription().getName() + " v" + getDescription().getVersion());
        instance = this;
        VortexCore.setPlugin(this);
        this.dataManager = new DataManager(this);
        onPreComponentLoad();

        String pluginRoot = getClass().getAnnotation(Root.class).packageName();
        if (pluginRoot == null) {
            throw new RuntimeException("Plugin root not found");
        }

        Config databaseConfig = new Config("database.yml");

        //Scan the packages for
        database = new Database(
                databaseConfig.getString("Connection Settings.Hostname"),
                databaseConfig.getString("Connection Settings.Port"),
                databaseConfig.getString("Connection Settings.Database"),
                databaseConfig.getString("Connection Settings.Username"),
                databaseConfig.getString("Connection Settings.Password"),
                databaseConfig.getInt("Connection Settings.Pool Size")
        );
        dependencyContainer = new DependencyContainer(getClass().getAnnotation(Root.class), getClass(), this, database, repositoryContainer);
        repositoryContainer = new RepositoryContainer(database);
        dependencyContainer.inject(this); //inject root class after all components are loaded

        onPluginEnable();
        getLogger().info("§aEnabled successfully!");
        getLogger().info(ChatColor.GREEN + "===================");
    }

    @Override
    public final void onDisable() {
        getLogger().info(ChatColor.RED + "===================");
        getLogger().info("Disabling " + getDescription().getName() + " v" + getDescription().getVersion());
        Bukkit.getScheduler().cancelTasks(this); //Make sure all tasks are cancelled
        onPluginDisable();
        getLogger().info("§cDisabled successfully!");
        getLogger().info(ChatColor.RED + "===================");
    }

    public abstract void onPreComponentLoad();

    public abstract void onPluginLoad();

    protected abstract void onPluginEnable();

    protected abstract void onPluginDisable();

    public static VortexPlugin getInstance() {
        return instance;
    }

    @Deprecated
    public DataManager getDataManager() {
        return dataManager;
    }

    @Deprecated
    protected void initDatabase(DataMigration... migrations) {
        dataManager.init(migrations);
    }

    protected void replaceBean(Class<?> holder, Object bean) {
        //Get subclasses if present
        Component component = holder.getAnnotation(Component.class);
        Bean beanAnnotation = holder.getAnnotation(Bean.class);
        if (component != null) {
            for (Class<?> clazz : component.registerSubclasses()) {
                dependencyContainer.replaceBean(clazz, bean);
            }
        } else if (beanAnnotation != null) {
            for (Class<?> clazz : beanAnnotation.registerSubclasses()) {
                dependencyContainer.replaceBean(clazz, bean);
            }
        }
        dependencyContainer.replaceBean(holder, bean);
    }
}
