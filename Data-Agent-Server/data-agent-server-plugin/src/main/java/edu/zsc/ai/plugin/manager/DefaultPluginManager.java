package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.SqlPlugin;
import edu.zsc.ai.plugin.capability.ColumnManager;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.capability.DatabaseManager;
import edu.zsc.ai.plugin.capability.FunctionManager;
import edu.zsc.ai.plugin.capability.IndexManager;
import edu.zsc.ai.plugin.capability.ProcedureManager;
import edu.zsc.ai.plugin.capability.SchemaManager;
import edu.zsc.ai.plugin.capability.SqlSplitter;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.capability.TableManager;
import edu.zsc.ai.plugin.capability.TriggerManager;
import edu.zsc.ai.plugin.capability.ViewManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.sql.DefaultSqlSplitter;
import edu.zsc.ai.plugin.sql.DefaultSqlValidator;
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
    public List<ConnectionManager> getConnectionManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), ConnectionManager.class, dbTypeCode);
    }

    @Override
    public ConnectionManager getConnectionManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, ConnectionManager.class);
    }

    @Override
    public ConnectionManager getConnectionManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ConnectionManager.class);
    }

    @Override
    public List<DatabaseManager> getDatabaseManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), DatabaseManager.class, dbTypeCode);
    }

    @Override
    public DatabaseManager getDatabaseManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, DatabaseManager.class);
    }

    @Override
    public DatabaseManager getDatabaseManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, DatabaseManager.class);
    }

    @Override
    public List<SchemaManager> getSchemaManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), SchemaManager.class, dbTypeCode);
    }

    @Override
    public SchemaManager getSchemaManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, SchemaManager.class);
    }

    @Override
    public SchemaManager getSchemaManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, SchemaManager.class);
    }

    @Override
    public List<TableManager> getTableManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), TableManager.class, dbTypeCode);
    }

    @Override
    public TableManager getTableManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, TableManager.class);
    }

    @Override
    public TableManager getTableManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, TableManager.class);
    }

    @Override
    public List<ViewManager> getViewManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), ViewManager.class, dbTypeCode);
    }

    @Override
    public ViewManager getViewManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, ViewManager.class);
    }

    @Override
    public ViewManager getViewManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ViewManager.class);
    }

    @Override
    public List<ColumnManager> getColumnManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), ColumnManager.class, dbTypeCode);
    }

    @Override
    public ColumnManager getColumnManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, ColumnManager.class);
    }

    @Override
    public ColumnManager getColumnManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, ColumnManager.class);
    }

    @Override
    public List<IndexManager> getIndexManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), IndexManager.class, dbTypeCode);
    }

    @Override
    public IndexManager getIndexManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, IndexManager.class);
    }

    @Override
    public IndexManager getIndexManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion) {
        return PluginCapabilityResolver.getManagerByDbTypeAndVersion(getPluginsByDbTypeInternal(dbTypeCode), databaseVersion, IndexManager.class);
    }

    @Override
    public List<FunctionManager> getFunctionManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), FunctionManager.class, dbTypeCode);
    }

    @Override
    public FunctionManager getFunctionManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, FunctionManager.class);
    }

    @Override
    public List<ProcedureManager> getProcedureManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), ProcedureManager.class, dbTypeCode);
    }

    @Override
    public ProcedureManager getProcedureManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, ProcedureManager.class);
    }

    @Override
    public List<TriggerManager> getTriggerManagerByDbType(@NotBlank String dbTypeCode) {
        return PluginCapabilityResolver.getManagers(getPluginsByDbTypeInternal(dbTypeCode), TriggerManager.class, dbTypeCode);
    }

    @Override
    public TriggerManager getTriggerManagerByPluginId(@NotBlank String pluginId) {
        return PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, TriggerManager.class);
    }

    @Override
    public CommandExecutor<SqlCommandRequest, SqlCommandResult> getSqlCommandExecutorByPluginId(@NotBlank String pluginId) {
        return (CommandExecutor<SqlCommandRequest, SqlCommandResult>) PluginCapabilityResolver.getManagerByPluginId(pluginMap, pluginId, CommandExecutor.class);
    }

    @Override
    public SqlSplitter getSqlSplitterByPluginId(@NotBlank String pluginId) {
        Plugin plugin = pluginMap.get(pluginId);
        return (plugin instanceof SqlSplitter splitter) ? splitter : DefaultSqlSplitter.INSTANCE;
    }

    @Override
    public SqlValidator getSqlValidatorByPluginId(@NotBlank String pluginId) {
        Plugin plugin = pluginMap.get(pluginId);
        return (plugin instanceof SqlValidator validator) ? validator : DefaultSqlValidator.INSTANCE;
    }

    public boolean supportsSchemaByPluginId(@NotBlank String pluginId) {
        Plugin plugin = pluginMap.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("No plugin found with ID: " + pluginId);
        }
        return !(plugin instanceof SqlPlugin sqlPlugin) || sqlPlugin.supportSchema();
    }
}
