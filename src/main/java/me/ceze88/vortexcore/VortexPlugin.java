package me.ceze88.vortexcore;

import me.ceze88.vortexcore.chat.PromptManager;
import me.ceze88.vortexcore.config.Config;
import me.ceze88.vortexcore.database.DataManager;
import me.ceze88.vortexcore.database.DataMigration;
import me.ceze88.vortexcore.gui.GuiManager;
import me.ceze88.vortexcore.hooks.internal.ReloadHook;
import me.ceze88.vortexcore.vinject.annotation.RegisterReloadHook;
import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.Component;
import net.vortexdevelopment.vinject.annotation.Root;

import net.vortexdevelopment.vinject.database.Database;
import net.vortexdevelopment.vinject.database.repository.RepositoryContainer;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private DependencyContainer dependencyContainer;
    private RepositoryContainer repositoryContainer;
    private Database database;

    private PromptManager promptManager;

    private Set<ReloadHook> reloadHooks = new HashSet<>();

    @Override
    public final void onLoad() {
        onPluginLoad();
        Database.setTablePrefix(this.getName().toLowerCase() + "_");
    }

    @Override
    public final void onEnable() {
        try {
            getLogger().info(ChatColor.GREEN + "===================");
            getLogger().info("Enabling " + getDescription().getName() + " v" + getDescription().getVersion());
            instance = this;
            VortexCore.setPlugin(this);
            GuiManager.register(this); //TODO add lang support
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
            repositoryContainer = new RepositoryContainer(database);
            dependencyContainer = new DependencyContainer(getClass().getAnnotation(Root.class), getClass(), this, database, repositoryContainer);
            dependencyContainer.injectStatic(this.getClass());
            dependencyContainer.inject(this); //inject root class after all components are loaded

            getServer().getPluginManager().registerEvents(new PromptManager(), this);

            onPluginEnable();
            getLogger().info("§aEnabled successfully!");
            getLogger().info(ChatColor.GREEN + "===================");
        } catch (Exception e) {
            getLogger().severe("An error occurred while enabling the plugin: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe(ChatColor.RED + "===================");
            Bukkit.getPluginManager().disablePlugin(this);
            GuiManager.disable();
        }
    }

    @Override
    public final void onDisable() {
        GuiManager.disable();
        getLogger().info(ChatColor.RED + "===================");
        getLogger().info("Disabling " + getDescription().getName() + " v" + getDescription().getVersion());
        Bukkit.getScheduler().cancelTasks(this); //Make sure all tasks are cancelled
        onPluginDisable();
        dataManager.shutdown();
        database.shutdown();
        getLogger().info("§cDisabled successfully!");
        getLogger().info(ChatColor.RED + "===================");
        HandlerList.unregisterAll(this);
        dependencyContainer.release();
    }

    public abstract void onPreComponentLoad();

    public abstract void onPluginLoad();

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
        database.init();
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

    public void registerReloadHook(ReloadHook hook) {
        reloadHooks.add(hook);
    }

    public void runReloadHooks() {
        //Order by priority
        List<ReloadHook> hooks = new ArrayList<>(reloadHooks);
        hooks.sort((o1, o2) -> Integer.compare(o2.getClass().getAnnotation(RegisterReloadHook.class).priority(), o1.getClass().getAnnotation(RegisterReloadHook.class).priority()));
        hooks.forEach(ReloadHook::onReload);
    }
}
