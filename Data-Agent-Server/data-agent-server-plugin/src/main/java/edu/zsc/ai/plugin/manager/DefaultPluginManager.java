package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;
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
 * Default implementation of PluginManager using Java SPI.
 * Plugins are automatically loaded in static initializer when class is first used.
 */
public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = Logger.getLogger(DefaultPluginManager.class.getName());

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

    @Override
    public List<Plugin> getPluginsByDbType(DbType dbType) {
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

    @Override
    public List<Plugin> getPluginsByCapability(String capability) {
        if (StringUtils.isBlank(capability)) {
            return Collections.emptyList();
        }

        List<Plugin> plugins = pluginsByCapability.get(capability);
        return plugins != null ? List.copyOf(plugins) : List.of();
    }

    @Override
    public List<Plugin> getAllPlugins() {
        // Collect all unique plugins from dbType index
        return pluginsByDbType.values().stream()
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public int getPluginCount() {
        return getAllPlugins().size();
    }
}

