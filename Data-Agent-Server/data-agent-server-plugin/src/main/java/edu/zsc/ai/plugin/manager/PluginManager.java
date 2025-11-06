package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Plugin Manager
 * Thread-safe static utility class for managing plugins.
 * Plugins are automatically loaded via Java SPI when class is first used.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class PluginManager {

    private static final Logger logger = Logger.getLogger(PluginManager.class.getName());

    /**
     * Plugin registry: plugin ID -> Plugin instance
     */
    private static final Map<String, Plugin> PLUGIN_MAP = new ConcurrentHashMap<>();
    /**
     * Database type index: database type code -> List of plugins
     */
    private static final Map<String, List<Plugin>> pluginsByDbType = new ConcurrentHashMap<>();

    /**
     * Capability index: capability code -> List of plugins
     */
    private static final Map<String, List<Plugin>> pluginsByCapability = new ConcurrentHashMap<>();

    static {
        // Auto-load all plugins using Java SPI
        logger.info("Loading plugins using Java SPI...");

        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        int successCount = 0;
        int failureCount = 0;

        for (Plugin plugin : loader) {
            try {
                // Add to database type index (use code as key)
                String dbTypeCode = plugin.getDbType().getCode();
                pluginsByDbType.computeIfAbsent(dbTypeCode, k -> new ArrayList<>()).add(plugin);

                // Add to capability index
                for (String capability : plugin.getSupportedCapabilities()) {
                    pluginsByCapability.computeIfAbsent(capability, k -> new ArrayList<>()).add(plugin);
                }
                // Add to main plugin map
                PLUGIN_MAP.put(plugin.getPluginId(), plugin);

                logger.info(String.format("Loaded plugin: %s (ID: %s, Version: %s)", plugin.getDisplayName(), plugin.getPluginId(), plugin.getVersion()));
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.severe(String.format("Failed to load plugin %s: %s", plugin.getClass().getName(), e.getMessage()));
            }
        }

        logger.info(String.format("Plugin loading completed. Success: %d, Failed: %d", successCount, failureCount));
    }

    // ========== Plugin Lookup ==========

    /**
     * Get all plugins for a specific database type.
     * Results are ordered by version (newest first).
     *
     * @param dbType database type
     * @return list of plugins (empty if none found)
     */
    public static List<Plugin> getPluginsByDbType(DbType dbType) {
        if (dbType == null) {
            return Collections.emptyList();
        }

        String dbTypeCode = dbType.getCode();
        List<Plugin> plugins = pluginsByDbType.get(dbTypeCode);
        if (plugins == null) {
            return Collections.emptyList();
        }

        // Sort by version (newest first)
        return plugins.stream()
                .sorted(Comparator.comparing(Plugin::getVersion).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all plugins for a specific database type by string code.
     * Results are ordered by version (newest first).
     * This method avoids the need to convert string to enum.
     *
     * @param dbTypeCode database type code (e.g., "mysql", "MYSQL")
     * @return list of plugins (empty if none found)
     */
    public static List<Plugin> getPluginsByDbTypeCode(String dbTypeCode) {
        if (StringUtils.isBlank(dbTypeCode)) {
            return Collections.emptyList();
        }

        // Convert string to enum and call the enum-based method
        try {
            DbType dbType = DbType.fromCode(dbTypeCode);
            return getPluginsByDbType(dbType);
        } catch (IllegalArgumentException e) {
            // Unknown database type, return empty list
            return Collections.emptyList();
        }
    }

    /**
     * Get all plugins that support a specific capability.
     *
     * @param capability capability code (e.g., "CONNECTION")
     * @return list of plugins (empty if none found)
     */
    public static List<Plugin> getPluginsByCapability(String capability) {
        if (StringUtils.isBlank(capability)) {
            return Collections.emptyList();
        }

        List<Plugin> plugins = pluginsByCapability.get(capability);
        return plugins != null ? List.copyOf(plugins) : List.of();
    }

    /**
     * Get all loaded plugins.
     *
     * @return list of all plugins (empty if none loaded)
     */
    public static List<Plugin> getAllPlugins() {
        // Collect all unique plugins from dbType index
        return pluginsByDbType.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get count of loaded plugins.
     *
     * @return number of plugins in registry
     */
    public static int getPluginCount() {
        return getAllPlugins().size();
    }

    // ========== Capability Lookup ==========

    /**
     * Get ConnectionProvider for a specific database type by string code.
     * Returns the first available plugin that implements ConnectionProvider capability.
     * Plugins are ordered by version (newest first).
     *
     * @param dbTypeCode database type code (e.g., "mysql", "MYSQL")
     * @return ConnectionProvider instance
     * @throws PluginException if no plugin with ConnectionProvider capability found
     */
    public static ConnectionProvider getConnectionProvider(String dbTypeCode) {
        if (StringUtils.isBlank(dbTypeCode)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Database type code cannot be empty");
        }

        // Get plugins by database type
        List<Plugin> plugins = getPluginsByDbTypeCode(dbTypeCode);
        if (plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbTypeCode);
        }

        return (ConnectionProvider) plugins.get(0);
    }


    public static ConnectionProvider getConnectionProviderByPluginId(String pluginId) {
        if (StringUtils.isBlank(pluginId)) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "Plugin Id cannot be empty");
        }

        // Get plugins by database type
        Plugin plugin = PLUGIN_MAP.get(pluginId);
        if (plugin == null) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin found with ID: " + pluginId);
        }
        return (ConnectionProvider) plugin;
    }

    /**
     * Select appropriate plugin based on database version.
     * Compares database version with plugin's supported version range.
     *
     * @param dbTypeCode      database type code
     * @param databaseVersion actual database version from connection
     * @return selected plugin
     * @throws PluginException if no compatible plugin found
     */
    public static Plugin selectPluginByDbVersion(String dbTypeCode, String databaseVersion) {
        List<Plugin> plugins = getPluginsByDbTypeCode(dbTypeCode);
        if (plugins.isEmpty()) {
            throw new PluginException(PluginErrorCode.PLUGIN_NOT_FOUND,
                    "No plugin available for database type: " + dbTypeCode);
        }

        // Find plugin that supports the database version
        for (Plugin plugin : plugins) {
            if (isVersionCompatible(databaseVersion, plugin.getSupportMinVersion(), plugin.getSupportMaxVersion())) {
                return plugin;
            }
        }

        // If no exact match found, use the first plugin (fallback)
        logger.warning(String.format("No plugin found that explicitly supports database version %s, using first available plugin", databaseVersion));
        return plugins.get(0);
    }

    /**
     * Check if database version is compatible with plugin's version range.
     *
     * @param dbVersion  database version to check
     * @param minVersion minimum supported version (empty string means no lower limit)
     * @param maxVersion maximum supported version (empty string means no upper limit)
     * @return true if compatible, false otherwise
     */
    private static boolean isVersionCompatible(String dbVersion, String minVersion, String maxVersion) {
        if (dbVersion == null || dbVersion.isEmpty()) {
            return true; // If version unknown, assume compatible
        }

        try {
            // Check minimum version constraint
            if (minVersion != null && !minVersion.isEmpty()) {
                int comparisonMin = compareVersions(dbVersion, minVersion);
                if (comparisonMin < 0) {
                    return false; // Database version is lower than minimum
                }
            }

            // Check maximum version constraint
            // Empty maxVersion means supporting all future versions
            if (maxVersion != null && !maxVersion.isEmpty()) {
                int comparisonMax = compareVersions(dbVersion, maxVersion);
                if (comparisonMax > 0) {
                    return false; // Database version is higher than maximum
                }
            }

            return true;
        } catch (Exception e) {
            logger.warning(String.format("Failed to compare versions: dbVersion=%s, minVersion=%s, maxVersion=%s, error=%s",
                    dbVersion, minVersion, maxVersion, e.getMessage()));
            return true; // If comparison fails, assume compatible
        }
    }

    /**
     * Compare two version strings.
     * Uses semantic versioning comparison (e.g., "5.7.0" vs "8.0.0").
     * Returns negative if version1 < version2, zero if equal, positive if version1 > version2.
     *
     * @param version1 first version string
     * @param version2 second version string
     * @return comparison result
     */
    private static int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int part2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (part1 != part2) {
                return Integer.compare(part1, part2);
            }
        }

        return 0;
    }

    /**
     * Parse version part (removes non-digit suffix, e.g., "5.7.0-beta" -> 5.7.0).
     *
     * @param versionPart version part string
     * @return numeric value
     */
    private static int parseVersionPart(String versionPart) {
        if (versionPart == null || versionPart.isEmpty()) {
            return 0;
        }

        // Extract numeric prefix (e.g., "5-beta" -> 5)
        StringBuilder numericPart = new StringBuilder();
        for (char c : versionPart.toCharArray()) {
            if (Character.isDigit(c)) {
                numericPart.append(c);
            } else {
                break;
            }
        }

        return numericPart.length() > 0 ? Integer.parseInt(numericPart.toString()) : 0;
    }
}
