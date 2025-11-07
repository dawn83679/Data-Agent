package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.model.MavenCoordinates;

/**
 * Plugin Manager Static Facade
 * Provides static convenience methods that delegate to DefaultPluginManager singleton instance.
 * This class maintains backward compatibility with existing code that uses static methods.
 * 
 * For new code, consider using IPluginManager interface or DefaultPluginManager.getInstance() directly.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public final class PluginManager {

    private static final IPluginManager INSTANCE = DefaultPluginManager.getInstance();

    private PluginManager() {
        // Utility class, prevent instantiation
    }

    // ========== Plugin Query ==========

    /**
     * Find plugin by ID.
     *
     * @param pluginId plugin ID
     * @return plugin instance, or null if not found
     */
    public static Plugin findPluginById(String pluginId) {
        return INSTANCE.findPluginById(pluginId);
    }

    // ========== Plugin Selection ==========

    /**
     * Select the first plugin for a database type.
     * Returns the first plugin from the sorted list (plugins are sorted by version, newest first).
     *
     * @param dbTypeCode database type code (e.g., "mysql", "MYSQL")
     * @return selected plugin (first in sorted list)
     * @throws edu.zsc.ai.plugin.exception.PluginException if no plugin available for the database type
     */
    public static Plugin selectFirstPluginByDbType(String dbTypeCode) {
        return INSTANCE.selectFirstPluginByDbType(dbTypeCode);
    }

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
    public static Plugin selectPluginByDbTypeAndVersion(String dbTypeCode, String databaseVersion) {
        return INSTANCE.selectPluginByDbTypeAndVersion(dbTypeCode, databaseVersion);
    }

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
    public static ConnectionProvider selectConnectionProviderByDbType(String dbTypeCode) {
        return INSTANCE.selectConnectionProviderByDbType(dbTypeCode);
    }

    /**
     * Select ConnectionProvider by plugin ID.
     *
     * @param pluginId plugin ID
     * @return ConnectionProvider instance
     * @throws edu.zsc.ai.plugin.exception.PluginException if plugin not found or doesn't implement ConnectionProvider
     */
    public static ConnectionProvider selectConnectionProviderByPluginId(String pluginId) {
        return INSTANCE.selectConnectionProviderByPluginId(pluginId);
    }

    // ========== Driver Management ==========

    /**
     * Find Maven coordinates for a driver version.
     * Queries each plugin for the database type and returns coordinates from the first plugin that supports the version.
     *
     * @param dbType database type
     * @param driverVersion driver version (nullable)
     * @return Maven coordinates, or null if no plugin supports the version
     */
    public static MavenCoordinates findDriverMavenCoordinates(DbType dbType, String driverVersion) {
        return INSTANCE.findDriverMavenCoordinates(dbType, driverVersion);
    }

    // ========== Deprecated Methods ==========

    /**
     * @deprecated Use {@link #findPluginById(String)} instead
     */
    @Deprecated
    public static Plugin getPlugin(String pluginId) {
        return findPluginById(pluginId);
    }

    /**
     * @deprecated Use {@link #selectConnectionProviderByDbType(String)} instead
     */
    @Deprecated
    public static ConnectionProvider getConnectionProvider(String dbTypeCode) {
        return selectConnectionProviderByDbType(dbTypeCode);
    }

    /**
     * @deprecated Use {@link #selectConnectionProviderByPluginId(String)} instead
     */
    @Deprecated
    public static ConnectionProvider getConnectionProviderByPluginId(String pluginId) {
        return selectConnectionProviderByPluginId(pluginId);
    }

    /**
     * @deprecated Use {@link #selectFirstPluginByDbType(String)} instead
     */
    @Deprecated
    public static Plugin selectPluginForDbType(String dbTypeCode) {
        return selectFirstPluginByDbType(dbTypeCode);
    }

    /**
     * @deprecated Use {@link #selectFirstPluginByDbType(String)} instead
     */
    @Deprecated
    public static Plugin selectPluginByDbType(String dbTypeCode) {
        return selectFirstPluginByDbType(dbTypeCode);
    }

    /**
     * @deprecated Use {@link #selectFirstPluginByDbType(String)} instead
     */
    @Deprecated
    public static Plugin selectNewestPluginByDbType(String dbTypeCode) {
        return selectFirstPluginByDbType(dbTypeCode);
    }

    /**
     * @deprecated Use {@link #selectPluginByDbTypeAndVersion(String, String)} instead
     */
    @Deprecated
    public static Plugin selectPluginForDbType(String dbTypeCode, String databaseVersion) {
        return selectPluginByDbTypeAndVersion(dbTypeCode, databaseVersion);
    }

    /**
     * @deprecated Use {@link #selectPluginByDbTypeAndVersion(String, String)} instead
     */
    @Deprecated
    public static Plugin selectPluginByDbVersion(String dbTypeCode, String databaseVersion) {
        return selectPluginByDbTypeAndVersion(dbTypeCode, databaseVersion);
    }

    /**
     * @deprecated Use {@link #findDriverMavenCoordinates(DbType, String)} instead
     */
    @Deprecated
    public static MavenCoordinates getDriverMavenCoordinates(DbType dbType, String driverVersion) {
        return findDriverMavenCoordinates(dbType, driverVersion);
    }
}
