package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Plugin Version Sorter
 * Responsible for sorting plugins by version.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class PluginVersionSorter {

    private PluginVersionSorter() {
        // Utility class, prevent instantiation
    }

    /**
     * Sort plugins by version in descending order (newest first).
     *
     * @param plugins list of plugins to sort
     * @return sorted list of plugins (newest first)
     */
    public static List<Plugin> sortByVersionDesc(List<Plugin> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return plugins;
        }
        return plugins.stream()
                .sorted(Comparator.comparing(Plugin::getVersion).reversed())
                .collect(Collectors.toList());
    }
}

