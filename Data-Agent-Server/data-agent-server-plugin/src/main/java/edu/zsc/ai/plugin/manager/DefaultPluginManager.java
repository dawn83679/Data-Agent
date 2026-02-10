package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ColumnProvider;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.capability.DatabaseProvider;
import edu.zsc.ai.plugin.capability.SchemaProvider;
import edu.zsc.ai.plugin.capability.TableProvider;
import edu.zsc.ai.plugin.capability.ViewProvider;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = Logger.getLogger(DefaultPluginManager.class.getName());

    private final Map<String, Plugin> pluginMap = new ConcurrentHashMap<>();

    private final Map<String, List<Plugin>> pluginsByDbType = new ConcurrentHashMap<>();

    private static final DefaultPluginManager INSTANCE = new DefaultPluginManager();

    public static DefaultPluginManager getInstance() {
        return INSTANCE;
    }

    private DefaultPluginManager() {
        loadPlugins();
    }

    private void loadPlugins() {
        logger.info("Loading plugins using Java SPI...");

        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        int successCount = 0;
        int failureCount = 0;

        for (Plugin plugin : loader) {
            try {
                String dbTypeCode = plugin.getDbType().getCode().toLowerCase();
                pluginsByDbType.computeIfAbsent(dbTypeCode, k -> new ArrayList<>()).add(plugin);

                pluginMap.put(plugin.getPluginId(), plugin);

                logger.info(String.format("Loaded plugin: %s (ID: %s, Version: %s)", plugin.getDisplayName(), plugin.getPluginId(), plugin.getVersion()));
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.severe(String.format("Failed to load plugin %s: %s", plugin.getClass().getName(), e.getMessage()));
            }
        }

        logger.info(String.format("Plugin loading completed. Success: %d, Failed: %d", successCount, failureCount));
    }

    @Override
    public MavenCoordinates getMavenCoordinatesByDbTypeAndVersion(@NotNull DbType dbType, String driverVersion) {
        Objects.requireNonNull(dbType, "Database type cannot be null");
        List<Plugin> plugins = pluginsByDbType.get(dbType.getCode().toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new IllegalArgumentException("No plugin available for database type: " + dbType.getCode());
        }

        List<Plugin> sortedPlugins = PluginVersionSorter.sortByVersionDesc(plugins);

        for (Plugin plugin : sortedPlugins) {
            try {
                return plugin.getDriverMavenCoordinates(driverVersion);
            } catch (RuntimeException e) {
                logger.fine(String.format("Plugin %s does not support driver version %s: %s",
                        plugin.getPluginId(), driverVersion, e.getMessage()));
            }
        }

        throw new IllegalArgumentException(
                String.format("No plugin found that supports driver version %s for database type: %s",
                        driverVersion != null ? driverVersion : "default", dbType.getCode()));
    }

    private List<Plugin> getPluginsByDbTypeInternal(@NotBlank String dbTypeCode) {
        List<Plugin> plugins = pluginsByDbType.get(dbTypeCode.toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new IllegalArgumentException("No plugin available for database type: " + dbTypeCode);
        }
        return PluginVersionSorter.sortByVersionDesc(plugins);
    }

    @Override
    public List<Plugin> getPluginsByDbType(@NotBlank String dbTypeCode) {
        return getPluginsByDbTypeInternal(dbTypeCode);
    }

    @Override
    public Plugin getPluginByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginVersionSelector.select(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion);
    }

    @Override
    public List<ConnectionProvider> getConnectionProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), ConnectionProvider.class, dbTypeCode);
    }

    @Override
    public ConnectionProvider getConnectionProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, ConnectionProvider.class);
    }

    @Override
    public ConnectionProvider getConnectionProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ConnectionProvider.class);
    }

    @Override
    public List<DatabaseProvider> getDatabaseProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), DatabaseProvider.class, dbTypeCode);
    }

    @Override
    public DatabaseProvider getDatabaseProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, DatabaseProvider.class);
    }

    @Override
    public DatabaseProvider getDatabaseProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, DatabaseProvider.class);
    }

    @Override
    public List<SchemaProvider> getSchemaProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), SchemaProvider.class, dbTypeCode);
    }

    @Override
    public SchemaProvider getSchemaProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, SchemaProvider.class);
    }

    @Override
    public SchemaProvider getSchemaProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, SchemaProvider.class);
    }

    @Override
    public List<TableProvider> getTableProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), TableProvider.class, dbTypeCode);
    }

    @Override
    public TableProvider getTableProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, TableProvider.class);
    }

    @Override
    public TableProvider getTableProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, TableProvider.class);
    }

    @Override
    public List<ViewProvider> getViewProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), ViewProvider.class, dbTypeCode);
    }

    @Override
    public ViewProvider getViewProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, ViewProvider.class);
    }

    @Override
    public ViewProvider getViewProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ViewProvider.class);
    }

    @Override
    public List<ColumnProvider> getColumnProviderByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getProviders(getPluginsByDbTypeInternal(dbTypeCode), ColumnProvider.class, dbTypeCode);
    }

    @Override
    public ColumnProvider getColumnProviderByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, ColumnProvider.class);
    }

    @Override
    public ColumnProvider getColumnProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getProviderByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ColumnProvider.class);
    }

    @Override
    public CommandExecutor<SqlCommandRequest, SqlCommandResult> getSqlCommandExecutorByPluginId(@NotBlank String pluginId) {
        return (CommandExecutor<SqlCommandRequest, SqlCommandResult>) PluginCapabilityResolver.getProviderByPluginId(pluginMap, pluginId, CommandExecutor.class);
    }
}
