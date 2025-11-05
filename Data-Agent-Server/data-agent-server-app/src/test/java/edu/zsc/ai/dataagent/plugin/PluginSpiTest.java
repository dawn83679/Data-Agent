package edu.zsc.ai.dataagent.plugin;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.manager.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for plugin SPI loading and capability collection using PluginManager.
 */
public class PluginSpiTest {
    
    private final PluginManager pluginManager = new DefaultPluginManager();
    
    @Test
    public void testSpiLoading() {
        int count = 0;
        for (Plugin plugin : pluginManager.getAllPlugins()) {
            count++;
            System.out.println("=".repeat(50));
            System.out.println("Loaded plugin: " + plugin.getClass().getName());
            System.out.println("Plugin ID: " + plugin.getPluginId());
            System.out.println("Display Name: " + plugin.getDisplayName());
            System.out.println("Version: " + plugin.getVersion());
            System.out.println("Database Type: " + plugin.getDbType());
            System.out.println("Plugin Type: " + plugin.getPluginType());
            System.out.println("Description: " + plugin.getDescription());
            System.out.println("Vendor: " + plugin.getVendor());
            System.out.println("Website: " + plugin.getWebsite());
            System.out.println("Min DB Version: " + plugin.getSupportMinVersion());
            System.out.println("Max DB Version: " + plugin.getSupportMaxVersion());
            
            // Test capability collection
            Set<String> capabilities = plugin.getSupportedCapabilities();
            System.out.println("Supported Capabilities: " + capabilities);
            System.out.println("=".repeat(50));
            System.out.println();
            
            // Assertions
            assertNotNull(plugin.getPluginId());
            assertNotNull(plugin.getDisplayName());
            assertNotNull(plugin.getVersion());
            assertNotNull(plugin.getDbType());
            assertNotNull(plugin.getPluginType());
            assertNotNull(capabilities);
        }
        
        // Should have loaded 2 MySQL plugins (5.7 and 8.0+)
        assertEquals(2, count, "Should load 2 MySQL plugins");
        
        System.out.println("Total plugins loaded: " + count);
    }
    
    @Test
    public void testCapabilityCollection() {
        for (Plugin plugin : pluginManager.getAllPlugins()) {
            Set<String> capabilities = plugin.getSupportedCapabilities();
            
            System.out.println(plugin.getPluginId() + " capabilities: " + capabilities);
            
            // All plugins should have CONNECTION capability
            assertTrue(capabilities.contains("CONNECTION"), 
                plugin.getPluginId() + " should support CONNECTION capability");
        }
    }
}

