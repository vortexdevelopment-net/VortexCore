package net.vortexdevelopment.vortexcore;

import lombok.Getter;
import net.vortexdevelopment.vinject.annotation.Injectable;
import net.vortexdevelopment.vinject.di.DependencyRepository;
import net.vortexdevelopment.vortexcore.chat.PromptManager;
import net.vortexdevelopment.vortexcore.command.CommandManager;
import net.vortexdevelopment.vortexcore.config.Config;
import net.vortexdevelopment.vortexcore.database.DataMigration;
import net.vortexdevelopment.vortexcore.database.DataMigrationManager;
import net.vortexdevelopment.vortexcore.database.MigrationRepository;
import net.vortexdevelopment.vortexcore.gui.GuiManager;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.hologram.HologramManager;
import net.vortexdevelopment.vortexcore.utils.PluginInitState;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import net.vortexdevelopment.vinject.annotation.Inject;
import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.Component;
import net.vortexdevelopment.vinject.annotation.Root;

import net.vortexdevelopment.vinject.database.Database;
import net.vortexdevelopment.vinject.database.repository.RepositoryContainer;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Injectable
public abstract class VortexPlugin extends JavaPlugin {

    private static VortexPlugin instance;
    private DependencyContainer dependencyContainer;
    private RepositoryContainer repositoryContainer;
    private Database database;
    @Getter
    private CommandManager commandManager = new CommandManager();

    private Set<ReloadHook> reloadHooks = new HashSet<>();

    private boolean emergencyStop = false;
    private List<DataMigration> migrations = new ArrayList<>();
    private PluginInitState initState = PluginInitState.NOT_INITIALIZED;


    /**
     * Verify the plugin license. This method is called during onLoad().
     * @throws IllegalStateException if the license is invalid
     */
    protected abstract void verifyLicense() throws IllegalStateException;

    @Override
    public final void onLoad() {
        this.initState = PluginInitState.ON_LOAD;
        instance = this;
        // Create plugin folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        try {
            verifyLicense();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        commandManager.init(this);

        // Read the database config in case it needs to be used in the plugin load
        Config databaseConfig = new Config("database.yml");
        database = new Database(
                databaseConfig.getString("Connection Settings.Hostname"),
                databaseConfig.getString("Connection Settings.Port"),
                databaseConfig.getString("Connection Settings.Database"),
                databaseConfig.getString("Connection Settings.Type").toLowerCase(Locale.ENGLISH),
                databaseConfig.getString("Connection Settings.Username"),
                databaseConfig.getString("Connection Settings.Password"),
                databaseConfig.getInt("Connection Settings.Pool Size"),
                new File(getDataFolder(), getName().toLowerCase()));
        Database.setTablePrefix(this.getName().toLowerCase() + "_");
        onPluginLoad();
    }

    @Override
    public final void onEnable() {
        this.initState = PluginInitState.ON_ENABLE;
        if (emergencyStop) {
            return;
        }
        try {
            HologramManager.init();
            AdventureUtils.sendMessage("§a===================", Bukkit.getConsoleSender());
            AdventureUtils.sendMessage(
                    "§aEnabling " + getDescription().getName() + " v" + getDescription().getVersion(),
                    Bukkit.getConsoleSender());
            VortexCore.setPlugin(this);
            GuiManager.register(this); // TODO add lang support

            String pluginRoot = getClass().getAnnotation(Root.class).packageName();
            if (pluginRoot == null) {
                throw new RuntimeException("Plugin root not found");
            }

            // Scan the packages for
            repositoryContainer = new RepositoryContainer(database);
            dependencyContainer = new DependencyContainer(getClass().getAnnotation(Root.class), getClass(), this,
                    database, repositoryContainer, unused -> {
                        onPreComponentLoad();
                    });

            dependencyContainer.injectStatic(this.getClass());
            dependencyContainer.inject(this); // inject root class after all components are loaded

            // Register Database bean
            dependencyContainer.replaceBean(Database.class, database);

            MigrationRepository migrationRepository = dependencyContainer.getDependency(MigrationRepository.class);
            DataMigrationManager dataMigrationManager = new DataMigrationManager(migrationRepository);
            dataMigrationManager.registerMigrations(migrations);
            try (Connection connection = database.getConnection()) {
                dataMigrationManager.runMigrations(connection, this.getName().toLowerCase() + "_");
            }

            onPluginEnable();
            AdventureUtils.sendMessage("§aEnabled successfully!", Bukkit.getConsoleSender());
            AdventureUtils.sendMessage("§a===================", Bukkit.getConsoleSender());
        } catch (Exception e) {
            AdventureUtils.sendMessage("§cAn error occurred while enabling the plugin: " + e.getMessage(),
                    Bukkit.getConsoleSender());
            e.printStackTrace();
            AdventureUtils.sendMessage("§c===================", Bukkit.getConsoleSender());
            Bukkit.getPluginManager().disablePlugin(this);
            GuiManager.disable();
        }
    }

    @Override
    public final void onDisable() {
        this.initState = PluginInitState.ON_DISABLE;
        GuiManager.disable();
        HologramManager.clear();
        AdventureUtils.sendMessage("§c===================", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§cDisabling " + getDescription().getName() + " v" + getDescription().getVersion(),
                Bukkit.getConsoleSender());
        Bukkit.getScheduler().cancelTasks(this); // Make sure all tasks are canceled
        if (!emergencyStop) {
            onPluginDisable();
        }
        database.shutdown();
        HandlerList.unregisterAll(this);
        if (dependencyContainer != null) {
            dependencyContainer.release();
        }
        AdventureUtils.sendMessage("§cDisabled successfully!", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§c===================", Bukkit.getConsoleSender());
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

    protected void initDatabase(DataMigration... migrations) {
        try {
            if (this.initState != PluginInitState.ON_LOAD) {
                throw new IllegalStateException("Database must be initialized during onLoad()");
            }
            this.database.init();
            if (migrations != null) {
                this.migrations = Arrays.asList(migrations);
            }
        } catch (Exception e) {
            this.emergencyStop = true;
            AdventureUtils.sendMessage("§cCould not connect to the database: " + e.getMessage(), Bukkit.getConsoleSender());
            e.printStackTrace();
            AdventureUtils.sendMessage("§cPlease correctly set up your database connection in the database.yml file.", Bukkit.getConsoleSender());
            AdventureUtils.sendMessage("§cDisabling plugin...", Bukkit.getConsoleSender());
            Bukkit.getScheduler().cancelTasks(this);
            HandlerList.unregisterAll(this);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    protected void replaceBean(Class<?> holder, Object bean) {
        // Get subclasses if present
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
        // Order by priority
        List<ReloadHook> hooks = new ArrayList<>(reloadHooks);
        hooks.sort((o1, o2) -> Integer.compare(o2.getClass().getAnnotation(RegisterReloadHook.class).priority(),
                o1.getClass().getAnnotation(RegisterReloadHook.class).priority()));
        hooks.forEach(ReloadHook::onReload);
    }
}
