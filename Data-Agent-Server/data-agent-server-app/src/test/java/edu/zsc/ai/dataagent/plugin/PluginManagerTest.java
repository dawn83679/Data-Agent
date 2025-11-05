package edu.zsc.ai.dataagent.plugin;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.manager.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PluginManager with real MySQL plugins.
 */
public class PluginManagerTest {

    private PluginManager pluginManager;

    @BeforeEach
    public void setUp() {
        pluginManager = new DefaultPluginManager();
    }

    @Test
    public void testGetAllPlugins() {
        List<Plugin> allPlugins = pluginManager.getAllPlugins();
        assertNotNull(allPlugins, "Plugin list should not be null");
        assertEquals(2, allPlugins.size(), "Should have exactly 2 MySQL plugins");

        System.out.println("✅ Loaded " + allPlugins.size() + " plugins:");
        for (Plugin plugin : allPlugins) {
            System.out.println("  - " + plugin.getDisplayName() + " (ID: " + plugin.getPluginId() + ")");
        }
    }

    @Test
    public void testGetPluginsByDbType() {
        List<Plugin> mysqlPlugins = pluginManager.getPluginsByDbType(DbType.MYSQL);
        assertNotNull(mysqlPlugins);
        assertEquals(2, mysqlPlugins.size(), "Should have 2 MySQL plugins");

        System.out.println("✅ MySQL plugins (sorted by version):");
        for (Plugin plugin : mysqlPlugins) {
            System.out.println("  - " + plugin.getPluginId() + " v" + plugin.getVersion());
        }
    }

    @Test
    public void testGetPluginsByCapability() {
        List<Plugin> connectionPlugins = pluginManager.getPluginsByCapability("CONNECTION");
        assertNotNull(connectionPlugins);
        assertEquals(2, connectionPlugins.size(), "Should have 2 plugins with CONNECTION capability");

        for (Plugin plugin : connectionPlugins) {
            assertTrue(plugin.getSupportedCapabilities().contains("CONNECTION"),
                plugin.getPluginId() + " should have CONNECTION capability");
        }

        System.out.println("✅ Found " + connectionPlugins.size() + " plugins with CONNECTION capability");
    }

    @Test
    public void testPluginCount() {
        int count = pluginManager.getPluginCount();
        assertEquals(2, count, "Should have exactly 2 plugins");

        System.out.println("✅ Plugin count test passed");
    }

    @Test
    public void testEmptyLookupResults() {
        List<Plugin> noPlugins = pluginManager.getPluginsByCapability("NON_EXISTENT");
        assertNotNull(noPlugins);
        assertTrue(noPlugins.isEmpty(), "Should return empty list for non-existent capability");

        System.out.println("✅ Empty lookup test passed");
    }
}

