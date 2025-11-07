package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin Version Selector
 * Responsible for selecting the most appropriate plugin from a list based on database version.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class PluginVersionSelector {

    private static final Logger logger = Logger.getLogger(PluginVersionSelector.class.getName());
    
    /**
     * Pattern to extract numeric prefix from version part.
     * Matches leading digits (e.g., "5" from "5-beta", "33" from "33RC1").
     */
    private static final Pattern NUMERIC_PREFIX_PATTERN = Pattern.compile("^(\\d+)");

    private PluginVersionSelector() {
        // Utility class, prevent instantiation
    }

    /**
     * Select the most appropriate plugin from the list based on database version.
     * Plugins are sorted by version (newest first), then the first compatible plugin is returned.
     * If no compatible plugin is found, returns the first plugin as fallback.
     *
     * @param plugins        list of plugins to select from
     * @param databaseVersion database version to match (nullable)
     * @return selected plugin
     */
    public static Plugin select(List<Plugin> plugins, String databaseVersion) {
        if (plugins == null || plugins.isEmpty()) {
            throw new IllegalArgumentException("Plugin list cannot be null or empty");
        }

        // Sort by version (newest first)
        List<Plugin> sortedPlugins = PluginVersionSorter.sortByVersionDesc(plugins);

        // If no version specified, return first plugin
        if (databaseVersion == null || databaseVersion.isEmpty()) {
            return sortedPlugins.get(0);
        }


        // Find plugin that supports the database version
        for (Plugin plugin : sortedPlugins) {
            if (isVersionCompatible(databaseVersion, plugin.getSupportMinVersion(), plugin.getSupportMaxVersion())) {
                return plugin;
            }
        }

        // If no exact match found, use the first plugin (fallback)
        logger.warning(String.format("No plugin found that explicitly supports database version %s, using first available plugin", databaseVersion));
        return sortedPlugins.get(0);
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
    private static int compareVersions(@NotBlank String version1, @NotBlank String version2) {
       
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
     * Parse version part to integer.
     * Extracts numeric prefix using regex (e.g., "5-beta" -> 5, "33RC1" -> 33).
     *
     * @param versionPart version part string
     * @return numeric value, or 0 if no numeric prefix found
     */
    private static int parseVersionPart(String versionPart) {
        if (versionPart == null || versionPart.isEmpty()) {
            return 0;
        }

        Matcher matcher = NUMERIC_PREFIX_PATTERN.matcher(versionPart);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

