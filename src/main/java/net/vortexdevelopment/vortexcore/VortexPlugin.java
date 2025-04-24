package net.vortexdevelopment.vortexcore;

import lombok.Getter;
import net.kyori.adventure.Adventure;
import net.vortexdevelopment.vinject.annotation.Inject;
import net.vortexdevelopment.vortexcore.chat.PromptManager;
import net.vortexdevelopment.vortexcore.command.CommandManager;
import net.vortexdevelopment.vortexcore.config.Config;
import net.vortexdevelopment.vortexcore.database.DataManager;
import net.vortexdevelopment.vortexcore.database.DataMigration;
import net.vortexdevelopment.vortexcore.gui.GuiManager;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.hooks.plugin.vault.EconomyHook;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.hologram.HologramManager;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
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
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    @Getter
    private CommandManager commandManager;

    private Set<ReloadHook> reloadHooks = new HashSet<>();

    @Override
    public final void onLoad() {
        instance = this;
        commandManager = new CommandManager(this);

        //Read the database config in case it needs to be used in the plugin load
        Config databaseConfig = new Config("database.yml");
        database = new Database(
                databaseConfig.getString("Connection Settings.Hostname"),
                databaseConfig.getString("Connection Settings.Port"),
                databaseConfig.getString("Connection Settings.Database"),
                databaseConfig.getString("Connection Settings.Type").toLowerCase(Locale.ENGLISH),
                databaseConfig.getString("Connection Settings.Username"),
                databaseConfig.getString("Connection Settings.Password"),
                databaseConfig.getInt("Connection Settings.Pool Size")
        );
        onPluginLoad();
        Database.setTablePrefix(this.getName().toLowerCase() + "_");
    }

    @Override
    public final void onEnable() {
        try {
            HologramManager.init();
            getLogger().info(ChatColor.GREEN + "===================");
            getLogger().info("Enabling " + getDescription().getName() + " v" + getDescription().getVersion());
            VortexCore.setPlugin(this);
            GuiManager.register(this); //TODO add lang support
            this.dataManager = new DataManager(this);

            String pluginRoot = getClass().getAnnotation(Root.class).packageName();
            if (pluginRoot == null) {
                throw new RuntimeException("Plugin root not found");
            }


            //Scan the packages for
            repositoryContainer = new RepositoryContainer(database);
            dependencyContainer = new DependencyContainer(getClass().getAnnotation(Root.class), getClass(), this, database, repositoryContainer, unused -> {
                onPreComponentLoad();
            });
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
        HologramManager.clear();
        getLogger().info(ChatColor.RED + "===================");
        getLogger().info("Disabling " + getDescription().getName() + " v" + getDescription().getVersion());
        Bukkit.getScheduler().cancelTasks(this); //Make sure all tasks are cancelled
        onPluginDisable();
        if (dataManager != null) {
            dataManager.shutdown();
        }
        database.shutdown();
        getLogger().info("§cDisabled successfully!");
        getLogger().info(ChatColor.RED + "===================");
        HandlerList.unregisterAll(this);
        if (dependencyContainer != null) {
            dependencyContainer.release();
        }
    }

    public String getPrimaryColor() {
        return "<color:#137FFF>";
    }

    public String getSecondaryColor() {
        return "<color:#FFAA00>";
    }

    public net.kyori.adventure.text.Component getPrefix() {
        return AdventureUtils.formatComponent("<bold><gradient:#9200B7:#137FFF>" + getName() + "</gradient></bold>");
    }

    public String getPrefixString() {
        return "<bold><gradient:#9200B7:#137FFF>" + getName() + "</gradient></bold>";
    }

    public net.kyori.adventure.text.Component getPrefixWithDash() {
        return AdventureUtils.formatComponent("<bold><gradient:#9200B7:#137FFF>" + getName() + " - </gradient></bold>");
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
        try {
            dataManager.init(migrations);
            database.init();
        } catch (Exception e) {
            getLogger().severe("Could not connect to the database: " + e.getMessage());
            getLogger().severe("Please correctly set up your database connection in the database.yml file.");
            getLogger().severe("Disabling plugin...");
            Bukkit.getScheduler().cancelTasks(this);
            HandlerList.unregisterAll(this);
            Bukkit.getPluginManager().disablePlugin(this);
        }
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
