package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves capability managers from plugins.
 */
public final class PluginCapabilityResolver {

    private PluginCapabilityResolver() {
        // Utility class, prevent instantiation
    }

    /**
     * Get all plugins that implement the specified capability, filtered from the given plugin list.
     *
     * @param plugins         sorted list of plugins
     * @param capabilityClass the capability interface/class to filter by
     * @param dbTypeCode      database type code for error messages
     * @param <T>             capability type
     * @return list of capability managers (unmodifiable)
     * @throws IllegalArgumentException if no plugin implements the capability
     */
    public static <T> List<T> getManagers(List<Plugin> plugins, Class<T> capabilityClass, String dbTypeCode) {
        List<T> managers = plugins.stream()
                .filter(capabilityClass::isInstance)
                .map(capabilityClass::cast)
                .toList();
        if (managers.isEmpty()) {
            throw new IllegalArgumentException("No " + capabilityClass.getSimpleName() + " available for database type: " + dbTypeCode);
        }
        return managers;
    }

    /**
     * Get a capability manager by plugin ID.
     *
     * @param pluginMap       map of plugin ID to plugin
     * @param pluginId        plugin ID
     * @param capabilityClass the capability interface/class
     * @param <T>             capability type
     * @return the capability manager
     * @throws NullPointerException     if plugin not found
     * @throws IllegalArgumentException if plugin does not implement the capability
     */
    public static <T> T getManagerByPluginId(Map<String, Plugin> pluginMap, String pluginId, Class<T> capabilityClass) {
        Plugin plugin = Objects.requireNonNull(pluginMap.get(pluginId), "No plugin found with ID: " + pluginId);
        if (!capabilityClass.isInstance(plugin)) {
            throw new IllegalArgumentException("Plugin " + pluginId + " does not implement " + capabilityClass.getSimpleName());
        }
        return capabilityClass.cast(plugin);
    }

    /**
     * Get a capability manager by database type and version.
     *
     * @param plugins         sorted list of plugins
     * @param databaseVersion target database version (may be null for latest)
     * @param capabilityClass the capability interface/class
     * @param <T>             capability type
     * @return the capability manager
     * @throws IllegalArgumentException if selected plugin does not implement the capability
     */
    public static <T> T getManagerByDbTypeAndVersion(List<Plugin> plugins, String databaseVersion, Class<T> capabilityClass) {
        Plugin plugin = PluginVersionSelector.select(plugins, databaseVersion);
        if (!capabilityClass.isInstance(plugin)) {
            throw new IllegalArgumentException("Plugin " + plugin.getPluginId() + " does not implement " + capabilityClass.getSimpleName());
        }
        return capabilityClass.cast(plugin);
    }
}
