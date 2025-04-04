package net.vortexdevelopment.vortexcore.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DataManager implements DatabaseConnector {

    private VortexPlugin plugin;
    private HikariConfig hikariConfig;
    private HikariPool hikariPool;
    private List<DataMigration> migrations;

    protected final ExecutorService asyncPool = new ThreadPoolExecutor(1, 5, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-Database-Async-%d").build());

    public DataManager(VortexPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(DataMigration... migrations) {
        this.migrations = List.of(migrations);
        Config databaseConfig = new Config("database.yml");
        String host = databaseConfig.getString("Connection Settings.Hostname");
        int port = databaseConfig.getInt("Connection Settings.Port");
        String database = databaseConfig.getString("Connection Settings.Database");
        String username = databaseConfig.getString("Connection Settings.Username");
        String password = databaseConfig.getString("Connection Settings.Password");
        int maxConnections = databaseConfig.getInt("Connection Settings.Pool Size");

        if (host == null || port == 0 || database == null || username == null || password == null || maxConnections == 0) {
            throw new IllegalArgumentException("Invalid database configuration. Please set up the database.yml file correctly.");
        }

        String type = databaseConfig.getString("Connection Settings.Type").toLowerCase(Locale.ENGLISH);

        hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:" + type + "://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxConnections);
        hikariPool = new HikariPool(hikariConfig);

        //Run migrations
        try {
            runMigrations();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to run migrations: " + ex.getMessage());
        }
    }

    public long getNextId(String table) {
        AtomicReference<Long> id = new AtomicReference<>(0L);
        connectDSL(context -> {
            id.set(Objects.requireNonNull(context.select(DSL.field("id"))
                    .from(DSL.table(getTablePrefix() + table))
                    .orderBy(DSL.field("id").desc())
                    .limit(1)
                    .fetchOne())
                    .into(Long.class));
        });
        return id.get() + 1;
    }

    @Override
    public Connection getConnection() {
        try {
            return hikariPool.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    @Override
    public void connect(VoidConnection connection) {
        try (Connection conn = getConnection()) {
            connection.connect(conn);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed database connection: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public <T> T connect(ConnectionResult<T> connection) {
        try (Connection conn = getConnection()) {
            return connection.connect(conn);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed database connection: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void connectDSL(VoidDSLConnection connection) {
        try (Connection conn = getConnection()) {
            connection.connect(DSL.using(conn));
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed database connection: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public <T> T connectDSL(DSLConnectionResult<T> connection) {
        try (Connection conn = getConnection()) {
            return connection.connect(DSL.using(conn));
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed database connection: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    public void runMigrations() throws SQLException {
        try (Connection connection = getConnection()) {
            int currentMigration = -1;
            boolean migrationsExist;
            try {
                connection.createStatement().execute("SELECT 1 FROM " + this.getMigrationsTableName());
                migrationsExist = true;
            } catch (Exception ex) {
                migrationsExist = false;
            }

            if (!migrationsExist) {
                // No migration table exists, create one
                String createTable = "CREATE TABLE " + this.getMigrationsTableName() + " (migration_version INT NOT NULL)";
                try (PreparedStatement statement = connection.prepareStatement(createTable)) {
                    statement.execute();
                }

                // Insert primary row into migration table
                String insertRow = "INSERT INTO " + this.getMigrationsTableName() + " VALUES (?)";
                try (PreparedStatement statement = connection.prepareStatement(insertRow)) {
                    statement.setInt(1, -1);
                    statement.execute();
                }
            } else {
                // Grab the current migration version
                // Due to the automatic SQLite to H2 conversion that might have happened, two entries (one of them -1) might exist
                String selectVersion = "SELECT migration_version FROM " + this.getMigrationsTableName() + " ORDER BY migration_version DESC LIMIT 1";
                try (PreparedStatement statement = connection.prepareStatement(selectVersion)) {
                    ResultSet result = statement.executeQuery();
                    result.next();
                    currentMigration = result.getInt("migration_version");
                }
            }

            // Grab required migrations
            int finalCurrentMigration = currentMigration;
            List<DataMigration> requiredMigrations = this.migrations.stream()
                    .filter(x -> x.getRevision() > finalCurrentMigration)
                    .sorted(Comparator.comparingInt(DataMigration::getRevision))
                    .collect(Collectors.toList());

            // Nothing to migrate, abort
            if (requiredMigrations.isEmpty()) {
                return;
            }

            // Migrate the data
            for (DataMigration dataMigration : requiredMigrations) {
                dataMigration.migrate(connection, getTablePrefix());
            }

            // Set the new current migration to be the highest migrated to
            currentMigration = requiredMigrations.stream()
                    .map(DataMigration::getRevision)
                    .max(Integer::compareTo)
                    .orElse(-1);

            String updateVersion = "UPDATE " + this.getMigrationsTableName() + " SET migration_version = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateVersion)) {
                statement.setInt(1, currentMigration);
                statement.execute();
            }
        } catch (Exception ex) {
            throw new SQLException("Failed to run migrations", ex);
        }
    }

    private String getMigrationsTableName() {
        return getTablePrefix() + "migrations";
    }

    public String getTablePrefix() {
        if (this.plugin == null) {
            return "";
        }
        return this.plugin.getDescription().getName().toLowerCase() + '_';
    }

    public ExecutorService getAsyncPool() {
        return asyncPool;
    }

    /**
     * Saves the data to the database
     */
    public void save(Data data) {
        this.asyncPool.execute(() -> {
            saveSync(data);
        });
    }

    /**
     * Saves the data to the database
     */
    public void save(Data data, String idField, Object idValue) {
        this.asyncPool.execute(() -> {
            saveSync(data, idField, idValue);
        });
    }

    /**
     * Saves the data to the database
     */
    public void saveSync(Data data, String idField, Object idValue) {
        connectDSL(context -> {
            context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                    .set(data.serialize())
                    .onConflict(DSL.field(idField)).doUpdate()
                    .set(data.serialize())
                    .where(DSL.field(idField).eq(idValue))
                    .execute();
        });
    }

    /**
     * Saves the data to the database synchronously
     */
    public void saveSync(Data data) {
        connectDSL(context -> {
            context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                    .set(data.serialize())
                    .onConflict(data.getId() != -1 ? DSL.field("id") : DSL.field("uuid")).doUpdate()
                    .set(data.serialize())
                    .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString()))
                    .execute();
        });
    }

    /**
     * Saves the data in batch to the database
     */
    public void saveBatch(Collection<Data> dataBatch) {
        this.asyncPool.execute(() -> {
            saveBatchSync(dataBatch);
        });
    }

    /**
     * Saves the data in batch to the database
     */
    public void saveBatchSync(Collection<Data> dataBatch) {
        connectDSL(context -> {
            List<Query> queries = new ArrayList<>();
            for (Data data : dataBatch) {
                queries.add(context.insertInto(DSL.table(getTablePrefix() + data.getTableName()))
                        .set(data.serialize())
                        .onConflict(data.getId() != -1 ? DSL.field("id") : DSL.field("uuid")).doUpdate()
                        .set(data.serialize())
                        .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString())));
            }

            context.batch(queries).execute();
        });
    }

    /**
     * Deletes the data from the database
     */
    public void delete(Data data) {
        this.asyncPool.execute(() -> {
            deleteSync(data);
        });
    }

    /**
     * Deletes the data from the database
     */
    public void deleteSync(Data data) {
        connectDSL(context -> {
            context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                    .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field("uuid").eq(data.getUniqueId().toString()))
                    .execute();
        });
    }

    public void delete(Data data, String idField, Object idValue) {
        this.asyncPool.execute(() -> {
            deleteSync(data, idField, idValue);
        });
    }

    public void deleteSync(Data data, String idField, Object idValue) {
        connectDSL(context -> {
            context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                    .where(DSL.field(idField).eq(idValue))
                    .execute();
        });
    }

    /**
     * Deletes the data from the database
     */
    public void delete(Data data, String uuidColumn) {
        this.asyncPool.execute(() -> {
            connectDSL(context -> {
                context.delete(DSL.table(getTablePrefix() + data.getTableName()))
                        .where(data.getId() != -1 ? DSL.field("id").eq(data.getId()) : DSL.field(uuidColumn).eq(data.getUniqueId().toString()))
                        .execute();
            });
        });
    }

    /**
     * Loads the data from the database
     *
     * @param id The id of the data
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(int id, Class<?> clazz, String table) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field("id").eq(id))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data from the database
     *
     * @param uuid  The uuid of the data
     * @param clazz The class of the data
     * @param table The table of the data without prefix
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(UUID uuid, Class<?> clazz, String table) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field("uuid").eq(uuid.toString()))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data from the database
     *
     * @param uuid       The uuid of the data
     * @param clazz      The class of the data
     * @param table      The table of the data without prefix
     * @param uuidColumn The column of the uuid
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> T load(UUID uuid, Class<?> clazz, String table, String uuidColumn) {
        try {
            AtomicReference<Data> data = new AtomicReference<>();
            AtomicBoolean found = new AtomicBoolean(false);
            connectDSL(context -> {
                try {
                    Data newData = (Data) clazz.getConstructor().newInstance();
                    data.set(newData.deserialize(Objects.requireNonNull(context.select()
                                    .from(DSL.table(getTablePrefix() + table))
                                    .where(DSL.field(uuidColumn).eq(uuid.toString()))
                                    .fetchOne())
                            .intoMap()));
                    found.set(true);
                } catch (NullPointerException ignored) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if (found.get()) {
                return (T) data.get();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data in batch from the database
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> List<T> loadBatch(Class<?> clazz, String table) {
        try {
            List<Data> dataList = Collections.synchronizedList(new ArrayList<>());
            connectDSL(context -> {
                try {
                    for (@NotNull org.jooq.Record record : Objects.requireNonNull(context.select()
                            .from(DSL.table(getTablePrefix() + table))
                            .fetchArray())) {
                        Data data = (Data) clazz.getDeclaredConstructor().newInstance();
                        dataList.add(data.deserialize(record.intoMap()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            return (List<T>) dataList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the data in batch from the database
     *
     * @return The loaded data
     */
    @SuppressWarnings("unchecked")
    public <T extends Data> List<T> loadBatch(Class<?> clazz, String table, Condition... conditions) {
        try {
            List<Data> dataList = Collections.synchronizedList(new ArrayList<>());
            connectDSL(context -> {
                try {
                    for (@NotNull Record record : Objects.requireNonNull(context.select()
                            .from(DSL.table(getTablePrefix() + table))
                            .where(conditions)
                            .fetchArray())) {
                        Data data = (Data) clazz.getDeclaredConstructor().newInstance();
                        dataList.add(data.deserialize(record.intoMap()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            return (List<T>) dataList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Close the database and shutdown the async pool
     */
    public void shutdown() {
        this.asyncPool.shutdown();
        try {
            if (!this.asyncPool.awaitTermination(30, TimeUnit.SECONDS)) {
                this.plugin.getLogger().warning("Failed to shutdown the async DataManager pool in time. Forcing shutdown");
            }
        } catch (InterruptedException ex) {
            this.plugin.getLogger().warning("Error while shutting down the async DataManager pool: " + ex.getMessage());
        }
        this.asyncPool.shutdownNow();

        try {
            if (hikariPool != null) {
                this.hikariPool.shutdown();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Force shutdown the async pool and close the database
     *
     * @return Tasks that were still in the pool's queue
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasksLeftInQueue = this.asyncPool.shutdownNow();
        try {
            this.hikariPool.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return tasksLeftInQueue;
    }

}
