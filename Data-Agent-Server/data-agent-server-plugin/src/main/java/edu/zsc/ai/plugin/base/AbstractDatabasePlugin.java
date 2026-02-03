package edu.zsc.ai.plugin.base;

import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.enums.PluginType;


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
     * @throws IllegalStateException if @PluginInfo annotation is missing
     */
    protected AbstractDatabasePlugin() {
        this.pluginInfo = this.getClass().getAnnotation(PluginInfo.class);
        if (pluginInfo == null) {
            throw new IllegalStateException(
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
}

