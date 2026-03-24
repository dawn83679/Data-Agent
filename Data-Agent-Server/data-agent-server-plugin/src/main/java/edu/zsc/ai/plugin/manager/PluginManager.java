package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ColumnManager;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.FunctionManager;
import edu.zsc.ai.plugin.capability.IndexManager;
import edu.zsc.ai.plugin.capability.ProcedureManager;
import edu.zsc.ai.plugin.capability.SqlSplitter;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.capability.TriggerManager;
import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.capability.DatabaseManager;
import edu.zsc.ai.plugin.capability.SchemaManager;
import edu.zsc.ai.plugin.capability.TableManager;
import edu.zsc.ai.plugin.capability.ViewManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface PluginManager {

    List<Plugin> getPluginsByDbType(@NotBlank String dbTypeCode);

    Plugin getPluginByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    MavenCoordinates getMavenCoordinatesByDbTypeAndVersion(@NotNull DbType dbType, String driverVersion);

    List<ConnectionManager> getConnectionManagerByDbType(@NotBlank String dbTypeCode);

    ConnectionManager getConnectionManagerByPluginId(@NotBlank String pluginId);

    ConnectionManager getConnectionManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<DatabaseManager> getDatabaseManagerByDbType(@NotBlank String dbTypeCode);

    DatabaseManager getDatabaseManagerByPluginId(@NotBlank String pluginId);

    DatabaseManager getDatabaseManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<SchemaManager> getSchemaManagerByDbType(@NotBlank String dbTypeCode);

    SchemaManager getSchemaManagerByPluginId(@NotBlank String pluginId);

    SchemaManager getSchemaManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<TableManager> getTableManagerByDbType(@NotBlank String dbTypeCode);

    TableManager getTableManagerByPluginId(@NotBlank String pluginId);

    TableManager getTableManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<ViewManager> getViewManagerByDbType(@NotBlank String dbTypeCode);

    ViewManager getViewManagerByPluginId(@NotBlank String pluginId);

    ViewManager getViewManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<ColumnManager> getColumnManagerByDbType(@NotBlank String dbTypeCode);

    ColumnManager getColumnManagerByPluginId(@NotBlank String pluginId);

    ColumnManager getColumnManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<IndexManager> getIndexManagerByDbType(@NotBlank String dbTypeCode);

    IndexManager getIndexManagerByPluginId(@NotBlank String pluginId);

    IndexManager getIndexManagerByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<FunctionManager> getFunctionManagerByDbType(@NotBlank String dbTypeCode);

    FunctionManager getFunctionManagerByPluginId(@NotBlank String pluginId);

    List<ProcedureManager> getProcedureManagerByDbType(@NotBlank String dbTypeCode);

    ProcedureManager getProcedureManagerByPluginId(@NotBlank String pluginId);

    List<TriggerManager> getTriggerManagerByDbType(@NotBlank String dbTypeCode);

    TriggerManager getTriggerManagerByPluginId(@NotBlank String pluginId);

    CommandExecutor<SqlCommandRequest, SqlCommandResult> getSqlCommandExecutorByPluginId(@NotBlank String pluginId);

    /**
     * Get the SqlSplitter for the given plugin.
     * Falls back to DefaultSqlSplitter if the plugin does not implement SqlSplitter.
     */
    SqlSplitter getSqlSplitterByPluginId(@NotBlank String pluginId);

    /**
     * Get the SqlValidator for the given plugin.
     * Falls back to DefaultSqlValidator if the plugin does not implement SqlValidator.
     */
    SqlValidator getSqlValidatorByPluginId(@NotBlank String pluginId);
}
