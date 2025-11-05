package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginException;

import java.util.List;
import java.util.Map;

/**
 * Plugin manager interface for centralized plugin management.
 * Provides plugin loading, lifecycle management, and lookup capabilities.
 * Framework-agnostic - can be used in Spring or standalone applications.
 */
public interface PluginManager {

    // ========== Plugin Lookup ==========

    /**
     * Get all plugins for a specific database type.
     * Results are ordered by version (newest first).
     *
     * @param dbType database type
     * @return list of plugins (empty if none found)
     */
    List<Plugin> getPluginsByDbType(DbType dbType);

    /**
     * Get all plugins that support a specific capability.
     *
     * @param capability capability code (e.g., "CONNECTION")
     * @return list of plugins (empty if none found)
     */
    List<Plugin> getPluginsByCapability(String capability);

    /**
     * Get all loaded plugins.
     *
     * @return list of all plugins (empty if none loaded)
     */
    List<Plugin> getAllPlugins();

    /**
     * Get count of loaded plugins.
     *
     * @return number of plugins in registry
     */
    int getPluginCount();
}

