package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.model.MavenCoordinates;

/**
 * Plugin Manager Interface
 * Defines the contract for plugin management operations.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface IPluginManager {

    // ========== Plugin Query ==========

    /**
     * Find plugin by ID.
     *
     * @param pluginId plugin ID
     * @return plugin instance, or null if not found
     */
    Plugin findPluginById(String pluginId);

    // ========== Plugin Selection ==========

    /**
     * Select the first plugin for a database type.
     * Returns the first plugin from the sorted list (plugins are sorted by version, newest first).
     *
     * @param dbTypeCode database type code (e.g., "mysql", "MYSQL")
     * @return selected plugin (first in sorted list)
     * @throws edu.zsc.ai.plugin.exception.PluginException if no plugin available for the database type
     */
    Plugin selectFirstPluginByDbType(String dbTypeCode);

    /**
     * Select the most appropriate plugin for a database type based on database version.
     * Returns the plugin that best matches the database version.
     * This method simplifies application layer code by eliminating the need to handle List selection logic.
     *
     * @param dbTypeCode      database type code (e.g., "mysql", "MYSQL")
     * @param databaseVersion actual database version from connection (e.g., "8.0.33")
     * @return selected plugin that best matches the database version
     * @throws edu.zsc.ai.plugin.exception.PluginException if no plugin available for the database type
     */
    Plugin selectPluginByDbTypeAndVersion(String dbTypeCode, String databaseVersion);

    // ========== Capability Selection ==========

    /**
     * Select ConnectionProvider for a specific database type.
     * Returns the first available plugin that implements ConnectionProvider capability.
     * Plugins are ordered by version (newest first).
     *
     * @param dbTypeCode database type code (e.g., "mysql", "MYSQL")
     * @return ConnectionProvider instance
     * @throws edu.zsc.ai.plugin.exception.PluginException if no plugin with ConnectionProvider capability found
     */
    ConnectionProvider selectConnectionProviderByDbType(String dbTypeCode);

    /**
     * Select ConnectionProvider by plugin ID.
     *
     * @param pluginId plugin ID
     * @return ConnectionProvider instance
     * @throws edu.zsc.ai.plugin.exception.PluginException if plugin not found or doesn't implement ConnectionProvider
     */
    ConnectionProvider selectConnectionProviderByPluginId(String pluginId);

    // ========== Driver Management ==========

    /**
     * Find Maven coordinates for a driver version.
     * Queries each plugin for the database type and returns coordinates from the first plugin that supports the version.
     *
     * @param dbType database type
     * @param driverVersion driver version (nullable)
     * @return Maven coordinates
     * @throws edu.zsc.ai.plugin.exception.PluginException if no plugin supports the version or database type not found
     */
    MavenCoordinates findDriverMavenCoordinates(DbType dbType, String driverVersion);
}
