package edu.zsc.ai.plugin.base;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.annotation.CapabilityMarker;
import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.enums.PluginType;
import edu.zsc.ai.plugin.exception.PluginErrorCode;
import edu.zsc.ai.plugin.exception.PluginException;

import java.util.HashSet;
import java.util.Set;


/**
 * Abstract base class for database plugins.
 * Provides default implementation for metadata methods by reading {@link PluginInfo} annotation.
 * Subclasses only need to add {@link PluginInfo} annotation and implement business logic.
 */
public abstract class AbstractDatabasePlugin implements Plugin {
    
    /**
     * Plugin metadata from annotation
     */
    private final PluginInfo pluginInfo;
    
    /**
     * Constructor that reads and validates plugin metadata from annotation
     *
     * @throws PluginException if @PluginInfo annotation is missing
     */
    protected AbstractDatabasePlugin() {
        this.pluginInfo = this.getClass().getAnnotation(PluginInfo.class);
        if (pluginInfo == null) {
            throw new PluginException(
                PluginErrorCode.PLUGIN_METADATA_MISSING,
                "Plugin class " + this.getClass().getName() + " must be annotated with @PluginInfo"
            );
        }
    }
    
    // ========== Plugin Identification (implemented by reading annotation) ==========
    
    @Override
    public String getPluginId() {
        return pluginInfo.id();
    }
    
    @Override
    public String getDisplayName() {
        return pluginInfo.name();
    }
    
    @Override
    public String getVersion() {
        return pluginInfo.version();
    }
    
    @Override
    public DbType getDbType() {
        return pluginInfo.dbType();
    }
    
    @Override
    public PluginType getPluginType() {
        return pluginInfo.dbType().getPluginType();
    }
    
    @Override
    public String getDescription() {
        return pluginInfo.description();
    }
    
    @Override
    public String getVendor() {
        return pluginInfo.vendor();
    }
    
    @Override
    public String getWebsite() {
        return pluginInfo.website();
    }
    
    // ========== Database Version Support ==========
    
    @Override
    public String getSupportMinVersion() {
        return pluginInfo.supportMinVersion();
    }
    
    @Override
    public String getSupportMaxVersion() {
        return pluginInfo.supportMaxVersion();
    }
    
    /**
     * Get all supported capabilities by scanning implemented capability interfaces.
     * Automatically collects capabilities from interfaces annotated with @CapabilityMarker.
     *
     * @return set of capability identifiers
     */
    @Override
    public Set<String> getSupportedCapabilities() {
        Set<String> capabilities = new HashSet<>();
        collectCapabilities(this.getClass(), capabilities);
        return capabilities;
    }
    
    /**
     * Recursively collect capabilities from class and its parent classes
     *
     * @param clazz current class to scan
     * @param capabilities capability set to populate
     */
    private void collectCapabilities(Class<?> clazz, Set<String> capabilities) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        
        // Scan all interfaces of current class
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            CapabilityMarker marker = interfaceClass.getAnnotation(CapabilityMarker.class);
            if (marker != null) {
                capabilities.add(marker.value().getCode());
            }
            // Recursively scan parent interfaces
            collectCapabilities(interfaceClass, capabilities);
        }
        
        // Recursively scan parent class
        collectCapabilities(clazz.getSuperclass(), capabilities);
    }
}

