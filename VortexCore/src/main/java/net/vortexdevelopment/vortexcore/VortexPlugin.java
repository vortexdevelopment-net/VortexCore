package net.vortexdevelopment.vortexcore;

import lombok.Getter;
import net.vortexdevelopment.vinject.annotation.Bean;
import net.vortexdevelopment.vinject.annotation.component.Component;
import net.vortexdevelopment.vinject.annotation.component.Root;
import net.vortexdevelopment.vinject.annotation.util.Injectable;
import net.vortexdevelopment.vinject.database.Database;
import net.vortexdevelopment.vinject.database.repository.RepositoryContainer;
import net.vortexdevelopment.vinject.di.ConfigurationContainer;
import net.vortexdevelopment.vinject.di.DependencyContainer;
import net.vortexdevelopment.vortexcore.command.CommandManager;
import net.vortexdevelopment.vortexcore.database.DataMigration;
import net.vortexdevelopment.vortexcore.database.DataMigrationManager;
import net.vortexdevelopment.vortexcore.database.MigrationRepository;
import net.vortexdevelopment.vortexcore.gui.GuiManager;
import net.vortexdevelopment.vortexcore.hooks.internal.ConfigReloadHook;
import net.vortexdevelopment.vortexcore.hooks.internal.ReloadHook;
import net.vortexdevelopment.vortexcore.text.lang.Lang;
import net.vortexdevelopment.vortexcore.spi.BukkitAdventureBridges;
import net.vortexdevelopment.vortexcore.spi.CommandMaps;
import net.vortexdevelopment.vortexcore.spi.SkullProfiles;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import net.vortexdevelopment.vortexcore.text.hologram.HologramManager;
import net.vortexdevelopment.vortexcore.utils.PluginInitState;
import net.vortexdevelopment.vortexcore.vinject.annotation.RegisterReloadHook;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.lang.reflect.Method;
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
    private final Database database = new Database();
    @Getter
    private final CommandManager commandManager = new CommandManager();

    private final Set<RegisteredReloadHook> reloadHooks = new HashSet<>();
    private final List<Object> registeredPlaceholderExpansions = new ArrayList<>();

    private boolean emergencyStop = false;
    private PluginInitState initState = PluginInitState.NOT_INITIALIZED;

    private boolean initDatabaseCalled = false;
    private DataMigration[] pendingMigrations;

    private Metrics bstats;
    private Object bukkitAudiences;

    private final Object lock = new Object();

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
        // Set the root directory for configuration files
        ConfigurationContainer.setRootDirectory(this.getDataFolder().toPath());

        try {
            verifyLicense();
        } catch (IllegalStateException e) {
            // Check immidiately for trial mode
            if (Bukkit.getOnlinePlayers() .size() > 3) {
                this.emergencyStop = true;
                AdventureUtils.sendMessage("§cTrial version exceeded player limit of 3 players.", Bukkit.getOnlinePlayers().toArray(new Player[0]));
                AdventureUtils.sendMessage("§cTrial version exceeded player limit of 3 players.", Bukkit.getConsoleSender());
                AdventureUtils.sendMessage("§cDisabling plugin...", Bukkit.getConsoleSender());
                Bukkit.getScheduler().cancelTasks(this);
                HandlerList.unregisterAll(this);
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        } catch (Exception e) {
            this.emergencyStop = true;
            AdventureUtils.sendMessage("§cAn error occurred during license verification: " + e.getMessage(), Bukkit.getConsoleSender());
            e.printStackTrace();
            AdventureUtils.sendMessage("§cDisabling plugin...", Bukkit.getConsoleSender());
            Bukkit.getScheduler().cancelTasks(this);
            HandlerList.unregisterAll(this);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        commandManager.init(this);
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
            BukkitAdventureBridges.installEarlyIfAbsent(getClass());
            if (!CommandMaps.installEarlyIfAbsent(getClass())) {
                getLogger().warning(
                        "CommandMapBridge was not pre-installed. Dynamic commands will fail unless platform classes "
                                + "(e.g. SpigotCommandMapBridge) are in this JAR. If you use maven-shade-plugin with "
                                + "minimizeJar, disable it or keep net/**/platform/** — minimization can strip those "
                                + "classes because they are only loaded by name.");
            }
            if (!SkullProfiles.installEarlyIfAbsent(getClass())) {
                getLogger().warning(
                        "SkullProfileService was not pre-installed. Skull / player head deserialization from YAML "
                                + "may fail until DI finishes. Use the unified VortexCore runtime artifact (not API-only), "
                                + "and if you shade, keep net/**/platform/** or call SkullProfiles.installEarlyIfAbsent "
                                + "from onEnable before constructing DependencyContainer.");
            }
            GuiManager.register(this); // TODO add lang support

            String pluginRoot = getClass().getAnnotation(Root.class).packageName();
            if (pluginRoot == null) {
                throw new RuntimeException("Plugin root not found");
            }

            // Scan the packages for
            repositoryContainer = new RepositoryContainer(database);

            connectDatabase();

            Lang.initializeEarly();

            dependencyContainer = new DependencyContainer(getClass().getAnnotation(Root.class), getClass(), this,
                    database, repositoryContainer, unused -> {
                        onPreComponentLoad();
                    });

            runDatabaseMigrations();

            dependencyContainer.getInjectionEngine().injectStatic(this.getClass());
            dependencyContainer.getInjectionEngine().inject(this); // inject root class after all components are loaded

            // Register Database bean
            dependencyContainer.addBean(Database.class, database);

            onPluginEnable();

            initBukkitAudiences();

            // Enable bStats if present
            Integer bstatsId = getBstatsPluginId();
            if (bstatsId != null) {
                bstats = new Metrics(this, bstatsId);
            }

            try {
                verifyLicense();
            } catch (IllegalStateException e) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    // Check if player count is above 3, if not disable plugin and display trial version message
                    if (Bukkit.getOnlinePlayers().size() > 3) {
                        this.emergencyStop = true;
                        AdventureUtils.sendMessage("§cTrial version exceeded player limit of 3 players.", Bukkit.getOnlinePlayers().toArray(new Player[0]));
                        AdventureUtils.sendMessage("§cTrial version exceeded player limit of 3 players.", Bukkit.getConsoleSender());
                        AdventureUtils.sendMessage("§cDisabling plugin...", Bukkit.getConsoleSender());
                        Bukkit.getScheduler().cancelTasks(this);
                        HandlerList.unregisterAll(this);
                        Bukkit.getPluginManager().disablePlugin(this);
                    }
                }, 0L, 100L);
            }

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
        AdventureUtils.sendMessage("§c===================", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§cDisabling " + getDescription().getName() + " v" + getDescription().getVersion(), Bukkit.getConsoleSender());
        GuiManager.disable();
        HologramManager.clear();
        for (Object expansion : registeredPlaceholderExpansions) {
            try {
                java.lang.reflect.Method unregisterMethod = expansion.getClass().getMethod("unregister");
                unregisterMethod.invoke(expansion);
            } catch (Exception e) {
                getLogger().warning("Failed to unregister PlaceholderAPI expansion: " + e.getMessage());
            }
        }
        registeredPlaceholderExpansions.clear();
        Bukkit.getScheduler().cancelTasks(this); // Make sure all tasks are canceled
        if (!emergencyStop) {
            onPluginDisable();
        }
        if (dependencyContainer != null) {
            dependencyContainer.release();
        }
        if (database != null) {
            database.shutdown();
        }
        HandlerList.unregisterAll(this);

        if (bstats != null) {
            bstats.shutdown();
            bstats = null;
        }
        closeBukkitAudiences();

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

    @Nullable
    protected abstract Integer getBstatsPluginId();

    public static VortexPlugin getInstance() {
        return instance;
    }

    public void registerPlaceholderExpansion(Object expansion) {
        registeredPlaceholderExpansions.add(expansion);
    }

    public boolean sendAudienceMessage(CommandSender sender, net.kyori.adventure.text.Component message) {
        Object audiences = bukkitAudiences;
        if (audiences == null) {
            return false;
        }
        try {
            Method senderMethod = audiences.getClass().getMethod("sender", CommandSender.class);
            Object audience = senderMethod.invoke(audiences, sender);
            Method sendMessageMethod = audience.getClass()
                    .getMethod("sendMessage", net.kyori.adventure.text.Component.class);
            sendMessageMethod.invoke(audience, message);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void initBukkitAudiences() {
        try {
            Class<?> audiencesClass = Class.forName("net.kyori.adventure.platform.bukkit.BukkitAudiences");
            Method createMethod = audiencesClass.getMethod("create", org.bukkit.plugin.Plugin.class);
            bukkitAudiences = createMethod.invoke(null, this);
        } catch (Throwable ignored) {
            bukkitAudiences = null;
        }
    }

    private void closeBukkitAudiences() {
        Object audiences = bukkitAudiences;
        bukkitAudiences = null;
        if (audiences == null) {
            return;
        }
        try {
            Method closeMethod = audiences.getClass().getMethod("close");
            closeMethod.invoke(audiences);
        } catch (Throwable ignored) {
        }
    }

    protected void initDatabase(DataMigration... migrations) {
        if (this.initState != PluginInitState.ON_LOAD) {
            throw new IllegalStateException("Database must be initialized during onPluginLoad()");
        }
        this.initDatabaseCalled = true;
        this.pendingMigrations = migrations;
    }

    /**
     * Creates a config file from VortexCore resources on the classpath, or writes {@code defaultYamlContent}
     * when the dependent plugin JAR does not embed that resource.
     */
    private static final String DATABASE_CONFIG_PATH = "Connection Settings.";
    private static final String DATABASE_DISABLE_H2_SERVER_MODE_KEY = DATABASE_CONFIG_PATH + "Disable H2 Server Mode";

    private static void ensureBundledConfigYaml(File configFile, String resourceFileName)
            throws IOException {
        if (configFile.exists()) {
            return;
        }
        File parent = configFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (InputStream in = VortexPlugin.class.getResourceAsStream("/" + resourceFileName)) {
            if (in != null) {
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
    }

    private void ensureDatabaseYaml(File databaseConfigFile) throws IOException {
        ensureBundledConfigYaml(databaseConfigFile, "database.yml");
    }

    private YamlConfiguration loadDatabaseConfiguration(File databaseConfigFile) throws IOException {
        ensureDatabaseYaml(databaseConfigFile);
        YamlConfiguration databaseConfig = YamlConfiguration.loadConfiguration(databaseConfigFile);

        try (InputStream defaultsStream = getClass().getResourceAsStream("/database.yml")) {
            if (defaultsStream == null) {
                return databaseConfig;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) {
                    continue;
                }
                if (!databaseConfig.contains(key)) {
                    databaseConfig.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                databaseConfig.save(databaseConfigFile);
            }
        }
        return databaseConfig;
    }

    private void warnIfH2ServerModeDisabled(String databaseType, boolean disableH2ServerMode) {
        if (!"h2".equalsIgnoreCase(databaseType) || !disableH2ServerMode) {
            return;
        }
        getLogger().warning("H2 server mode is disabled in database.yml (Disable H2 Server Mode: true).");
        getLogger().warning("This should only be used in rare containerized setups where H2 AUTO_SERVER causes unknown host errors.");
        getLogger().warning("When disabled, PlugMan reloads will break this plugin's H2 database access. Leave this false in almost all cases.");
        AdventureUtils.sendMessage("§e[" + getName() + "] H2 server mode is disabled in database.yml.", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§eThis is not recommended for most servers and breaks PlugMan reloads with H2.", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§eOnly use Disable H2 Server Mode when required by your hosting environment.", Bukkit.getConsoleSender());
    }

    public static void ensureGlobalYaml(File globalConfigFile) throws IOException {
        ensureBundledConfigYaml(globalConfigFile, "global.yml");
    }

    public static void ensureSkyYaml(File skyConfigFile) throws IOException {
        //ensureBundledConfigYaml(skyConfigFile, "sky.yml");
    }

    private void connectDatabase() {
        if (!initDatabaseCalled) {
            return;
        }
        try {
            // Read the database config in case it needs to be used in the plugin load
            File databaseConfigFile = new File(getDataFolder(), "database.yml");
            YamlConfiguration databaseConfig = loadDatabaseConfiguration(databaseConfigFile);
            String databaseType = databaseConfig.getString(DATABASE_CONFIG_PATH + "Type").toLowerCase(Locale.ENGLISH);
            boolean disableH2ServerMode = databaseConfig.getBoolean(DATABASE_DISABLE_H2_SERVER_MODE_KEY, false);
            warnIfH2ServerModeDisabled(databaseType, disableH2ServerMode);
            this.database.init(
                    databaseConfig.getString(DATABASE_CONFIG_PATH + "Hostname"),
                    databaseConfig.getString(DATABASE_CONFIG_PATH + "Port"),
                    databaseConfig.getString(DATABASE_CONFIG_PATH + "Database"),
                    databaseType,
                    databaseConfig.getString(DATABASE_CONFIG_PATH + "Username"),
                    databaseConfig.getString(DATABASE_CONFIG_PATH + "Password"),
                    databaseConfig.getInt(DATABASE_CONFIG_PATH + "Pool Size"),
                    new File(getDataFolder(), getName().toLowerCase()),
                    !disableH2ServerMode);

            this.database.connect(); // Setups Hikari pool
        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    private void runDatabaseMigrations() {
        if (!initDatabaseCalled) {
            return;
        }
        try {
            if (pendingMigrations != null && pendingMigrations.length > 0) {
                MigrationRepository migrationRepository = dependencyContainer.getDependency(MigrationRepository.class);
                DataMigrationManager dataMigrationManager = new DataMigrationManager(migrationRepository);
                dataMigrationManager.registerMigrations(Arrays.asList(pendingMigrations));
                try (Connection connection = database.getConnection()) {
                    dataMigrationManager.runMigrations(connection, this.getName().toLowerCase() + "_");
                }
            }
        } catch (Exception e) {
            handleDatabaseError(e);
        }
    }

    private void handleDatabaseError(Exception e) {
        this.emergencyStop = true;
        AdventureUtils.sendMessage("§cCould not connect to the database: " + e.getMessage(), Bukkit.getConsoleSender());
        e.printStackTrace();
        AdventureUtils.sendMessage("§cPlease correctly set up your database connection in the database.yml file.", Bukkit.getConsoleSender());
        AdventureUtils.sendMessage("§cDisabling plugin...", Bukkit.getConsoleSender());
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().disablePlugin(this);
    }

    protected void replaceBean(Class<?> holder, Object bean) {
        // Get subclasses if present
        Component component = holder.getAnnotation(Component.class);
        Bean beanAnnotation = holder.getAnnotation(Bean.class);
        if (component != null) {
            for (Class<?> clazz : component.registerSubclasses()) {
                dependencyContainer.addBean(clazz, bean);
            }
        } else if (beanAnnotation != null) {
            for (Class<?> clazz : beanAnnotation.registerSubclasses()) {
                dependencyContainer.addBean(clazz, bean);
            }
        }
        dependencyContainer.addBean(holder, bean);
    }

    public void registerReloadHook(ReloadHook hook) {
        reloadHooks.add(new RegisteredReloadHook(hook, resolveReloadHookPriority(hook)));
    }

    public void runReloadHooks() {
        List<RegisteredReloadHook> hooks = new ArrayList<>(reloadHooks);
        hooks.sort((o1, o2) -> Integer.compare(o2.priority(), o1.priority()));
        hooks.forEach(entry -> entry.hook().onReload());
    }

    private int resolveReloadHookPriority(ReloadHook hook) {
        if (hook instanceof ConfigReloadHook configReloadHook) {
            return resolveReloadHookPriority(configReloadHook.sourceClass());
        }
        return resolveReloadHookPriority(hook.getClass());
    }

    private int resolveReloadHookPriority(Class<?> clazz) {
        RegisterReloadHook annotation = clazz.getAnnotation(RegisterReloadHook.class);
        return annotation != null ? annotation.priority() : 10;
    }

    private record RegisteredReloadHook(ReloadHook hook, int priority) {
    }
}
