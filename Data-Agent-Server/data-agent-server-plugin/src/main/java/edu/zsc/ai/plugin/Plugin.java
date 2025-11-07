package edu.zsc.ai.plugin;

import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.enums.PluginType;
import edu.zsc.ai.plugin.model.MavenCoordinates;

import java.util.Set;


/**
 * Top-level database plugin interface.
 * All plugin implementations must inherit from this interface.
 */
public interface Plugin {
    
    // ========== Plugin Identification ==========
    
    /**
     * Get plugin unique identifier
     *
     * @return plugin id (lowercase, e.g., "mysql")
     */
    String getPluginId();
    
    /**
     * Get plugin display name
     *
     * @return plugin display name (e.g., "MySQL Database")
     */
    String getDisplayName();
    
    /**
     * Get plugin version
     *
     * @return plugin version (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Get database type
     *
     * @return database type
     */
    DbType getDbType();
    
    /**
     * Get plugin type
     *
     * @return plugin type (SQL/NoSQL)
     */
    PluginType getPluginType();
    
    /**
     * Get plugin description
     *
     * @return plugin description
     */
    String getDescription();
    
    /**
     * Get vendor/author name
     *
     * @return vendor name
     */
    String getVendor();
    
    /**
     * Get official website URL
     *
     * @return website URL
     */
    String getWebsite();
    
    // ========== Database Version Support ==========
    
    /**
     * Get minimum supported database version
     *
     * @return minimum database version (e.g., "5.7.0")
     */
    String getSupportMinVersion();
    
    /**
     * Get maximum supported database version.
     * Empty string means supporting all future versions.
     *
     * @return maximum database version (e.g., "5.7.99"), or empty string
     */
    String getSupportMaxVersion();
    
    /**
     * Get all supported capabilities of this plugin.
     * Capabilities are automatically collected from implemented capability interfaces.
     *
     * @return set of capability identifiers
     */
    Set<String> getSupportedCapabilities();
    
    // ========== Driver Maven Coordinates ==========
    
    /**
     * Get Maven coordinates for the JDBC driver.
     * Plugin determines the appropriate groupId and artifactId based on the version.
     *
     * @param driverVersion driver version (e.g., "8.0.33", "5.1.49"), or null for default version
     * @return Maven coordinates (groupId, artifactId, version)
     * @throws edu.zsc.ai.plugin.exception.PluginException if the plugin does not support the given version or doesn't provide Maven coordinates
     */
    MavenCoordinates getDriverMavenCoordinates(String driverVersion);
}

