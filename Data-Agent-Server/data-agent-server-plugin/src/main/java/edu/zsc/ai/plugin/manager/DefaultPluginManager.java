package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.MavenCoordinates;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Default Plugin Manager Implementation
 * Thread-safe implementation of PluginManager interface.
 * Plugins are automatically loaded via Java SPI when instance is created.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class DefaultPluginManager implements IPluginManager {

    private static final Logger logger = Logger.getLogger(DefaultPluginManager.class.getName());

    /**
     * Plugin registry: plugin ID -> Plugin instance
     */
    private final Map<String, Plugin> pluginMap = new ConcurrentHashMap<>();

    /**
     * Database type index: database type code -> List of plugins
     */
    private final Map<String, List<Plugin>> pluginsByDbType = new ConcurrentHashMap<>();

    /**
     * Private constructor to enforce singleton pattern.
     * Loads plugins via Java SPI during initialization.
     */
    private DefaultPluginManager() {
        loadPlugins();
    }

    /**
     * Load all plugins using Java SPI mechanism.
     */
    private void loadPlugins() {
        logger.info("Loading plugins using Java SPI...");

        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        int successCount = 0;
        int failureCount = 0;

        for (Plugin plugin : loader) {
            try {
                // Add to database type index (use code as key, lowercase for consistency)
                String dbTypeCode = plugin.getDbType().getCode().toLowerCase();
                pluginsByDbType.computeIfAbsent(dbTypeCode, k -> new ArrayList<>()).add(plugin);

                // Add to main plugin map
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

    // ========== Singleton Pattern ==========

    /**
     * Singleton instance holder (lazy initialization)
     */
    private static class InstanceHolder {
        private static final DefaultPluginManager INSTANCE = new DefaultPluginManager();
    }

    /**
     * Get singleton instance of DefaultPluginManager.
     *
     * @return singleton instance
     */
    public static DefaultPluginManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ========== Plugin Query ==========

    @Override
    public Plugin findPluginById(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            return null;
        }
        return pluginMap.get(pluginId);
    }

    // ========== Plugin Selection ==========

    @Override
    public Plugin selectFirstPluginByDbType(String dbTypeCode) {
        if (StringUtils.isBlank(dbTypeCode)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Database type code cannot be empty");
        }

        List<Plugin> plugins = pluginsByDbType.get(dbTypeCode.toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbTypeCode);
        }

        // Return first plugin (sort by version, newest first)
        List<Plugin> sortedPlugins = PluginVersionSorter.sortByVersionDesc(plugins);
        return sortedPlugins.stream()
                .findFirst()
                .orElseThrow(() -> new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                        "No plugin available for database type: " + dbTypeCode));
    }

    @Override
    public Plugin selectPluginByDbTypeAndVersion(String dbTypeCode, String databaseVersion) {
        if (StringUtils.isBlank(dbTypeCode)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Database type code cannot be empty");
        }

        List<Plugin> plugins = pluginsByDbType.get(dbTypeCode.toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbTypeCode);
        }

        return PluginVersionSelector.select(plugins, databaseVersion);
    }

    // ========== Capability Selection ==========

    @Override
    public ConnectionProvider selectConnectionProviderByDbType(String dbTypeCode) {
        if (StringUtils.isBlank(dbTypeCode)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Database type code cannot be empty");
        }

        List<Plugin> plugins = pluginsByDbType.get(dbTypeCode.toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbTypeCode);
        }

        // Return first plugin (sort by version, newest first)
        List<Plugin> sortedPlugins = PluginVersionSorter.sortByVersionDesc(plugins);
        Plugin plugin = sortedPlugins.stream()
                .findFirst()
                .orElseThrow(() -> new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                        "No plugin available for database type: " + dbTypeCode));

        return (ConnectionProvider) plugin;
    }

    @Override
    public ConnectionProvider selectConnectionProviderByPluginId(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Plugin Id cannot be empty");
        }

        Plugin plugin = pluginMap.get(pluginId);
        if (plugin == null) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin found with ID: " + pluginId);
        }
        return (ConnectionProvider) plugin;
    }

    // ========== Driver Management ==========

    @Override
    public MavenCoordinates findDriverMavenCoordinates(DbType dbType, String driverVersion) {
        if (dbType == null) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Database type cannot be null");
        }

        List<Plugin> plugins = pluginsByDbType.get(dbType.getCode().toLowerCase());
        if (plugins == null || plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbType.getCode());
        }

        // Sort by version (newest first) and try each plugin
        List<Plugin> sortedPlugins = PluginVersionSorter.sortByVersionDesc(plugins);

        for (Plugin plugin : sortedPlugins) {
            try {
                MavenCoordinates coords = plugin.getDriverMavenCoordinates(driverVersion);
                return coords;
            } catch (PluginException e) {
                // Continue to next plugin if this one doesn't support the version
                logger.fine(String.format("Plugin %s does not support driver version %s: %s", 
                        plugin.getPluginId(), driverVersion, e.getMessage()));
            }
        }

        // No plugin supports the driver version
        throw new PluginException(PluginErrorCode.DRIVER_NOT_SUPPORTED,
                String.format("No plugin found that supports driver version %s for database type: %s", 
                        driverVersion != null ? driverVersion : "default", dbType.getCode()));
    }
}

