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

import java.util.List;

public interface PluginManager {

    List<Plugin> getPluginsByDbType(@NotBlank String dbTypeCode);

    Plugin getPluginByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    MavenCoordinates getMavenCoordinatesByDbTypeAndVersion(@NotNull DbType dbType, String driverVersion);

    List<ConnectionProvider> getConnectionProviderByDbType(@NotBlank String dbTypeCode);

    ConnectionProvider getConnectionProviderByPluginId(@NotBlank String pluginId);

    ConnectionProvider getConnectionProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<DatabaseProvider> getDatabaseProviderByDbType(@NotBlank String dbTypeCode);

    DatabaseProvider getDatabaseProviderByPluginId(@NotBlank String pluginId);

    DatabaseProvider getDatabaseProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<SchemaProvider> getSchemaProviderByDbType(@NotBlank String dbTypeCode);

    SchemaProvider getSchemaProviderByPluginId(@NotBlank String pluginId);

    SchemaProvider getSchemaProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<TableProvider> getTableProviderByDbType(@NotBlank String dbTypeCode);

    TableProvider getTableProviderByPluginId(@NotBlank String pluginId);

    TableProvider getTableProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<ViewProvider> getViewProviderByDbType(@NotBlank String dbTypeCode);

    ViewProvider getViewProviderByPluginId(@NotBlank String pluginId);

    ViewProvider getViewProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    List<ColumnProvider> getColumnProviderByDbType(@NotBlank String dbTypeCode);

    ColumnProvider getColumnProviderByPluginId(@NotBlank String pluginId);

    ColumnProvider getColumnProviderByDbTypeAndVersion(@NotBlank String dbTypeCode, String databaseVersion);

    CommandExecutor<SqlCommandRequest, SqlCommandResult> getSqlCommandExecutorByPluginId(@NotBlank String pluginId);
}
